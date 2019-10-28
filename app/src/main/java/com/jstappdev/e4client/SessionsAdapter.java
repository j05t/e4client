package com.jstappdev.e4client;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.jstappdev.e4client.data.CSVFile;
import com.jstappdev.e4client.data.E4Session;
import com.jstappdev.e4client.data.E4SessionData;
import com.squareup.okhttp.Request;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class SessionsAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<SessionsAdapter.MyViewHolder> {

    private SharedViewModel sharedViewModel;

    private SessionsAdapter instance;

    public SessionsAdapter(SharedViewModel sharedViewModel) {
        this.sharedViewModel = sharedViewModel;
        this.instance = this;
    }


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        TextView id;
        TextView start_time;
        TextView duration;
        TextView device_id;
        TextView label;
        TextView device;

        MyViewHolder(View v) {
            super(v);

            id = v.findViewById(R.id.session_id);
            start_time = v.findViewById(R.id.session_start_time);
            duration = v.findViewById(R.id.session_duration);
            device_id = v.findViewById(R.id.session_device_id);
            label = v.findViewById(R.id.session_label);
            device = v.findViewById(R.id.session_device);
        }
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sessions_list_item, parent, false);

        return new MyViewHolder(v);
    }

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int position = (int) v.getTag();
            final E4Session e4Session = sharedViewModel.getE4Sessions().get(position);

            new AlertDialog.Builder(v.getContext())
                    .setTitle("Session " + e4Session.getId())
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setMessage("Start: " + e4Session.getStartDate() + "\nDuration: " + e4Session.getDurationAsString())
                    .setCancelable(true)
                    .setPositiveButton("Share", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (Utils.isSessionDownloaded(e4Session)) {
                                final File file = new File(MainActivity.context.getFilesDir(), e4Session.getZIPFilename());

                                MainActivity.context.startActivity(new Intent()
                                        .setAction(Intent.ACTION_SEND)
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(MainActivity.context, MainActivity.context.getApplicationContext().getPackageName() + ".provider", file))
                                        .setType("application/zip"));
                            } else {
                                Toast.makeText(v.getContext(), String.format("Session %s not downloaded.", e4Session.getId()), Toast.LENGTH_SHORT).show();
                            }
                            dialog.dismiss();
                        }
                    })
                    .setNeutralButton("View Data", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            new LoadAndViewSessionData(v).execute(e4Session);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    }).create().show();
        }
    };

    private final View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            final int position = (int) v.getTag();
            final E4Session e4Session = sharedViewModel.getE4Sessions().get(position);

            new AlertDialog.Builder(v.getContext())
                    .setTitle("Delete Session " + e4Session.getId()).setIcon(android.R.drawable.ic_delete)
                    .setMessage(String.format("Start: %s\nDuration: %s", e4Session.getStartDate(), e4Session.getDurationAsString()))
                    .setCancelable(true)
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            new DeleteSession(instance, sharedViewModel, position).execute(e4Session);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    }).create().show();

            return false;
        }
    };


    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, final int position) {
        final E4Session e4Session = sharedViewModel.getE4Sessions().get(position);

        holder.itemView.setOnClickListener(onClickListener);
        holder.itemView.setOnLongClickListener(onLongClickListener);
        holder.itemView.setTag(position);

        holder.id.setText(e4Session.getId());
        holder.start_time.setText(e4Session.getStartDate());
        holder.duration.setText(e4Session.getDurationAsString());
        holder.device_id.setText(e4Session.getDeviceId());
        holder.label.setText(e4Session.getLabel());
        holder.device.setText(e4Session.getDevice());

        if (Utils.isSessionDownloaded(e4Session)) {
            holder.itemView.setBackgroundColor(Color.parseColor("#1cdefce0"));
        }
    }

    @Override
    public int getItemCount() {
        return sharedViewModel.getE4Sessions().size();
    }


    private static class DeleteSession extends AsyncTask<E4Session, String, Boolean> {

        private SessionsAdapter adapter;
        private SharedViewModel viewModel;
        private int position;

        DeleteSession(final SessionsAdapter sessionsAdapter, SharedViewModel sharedViewModel, int position) {
            this.adapter = sessionsAdapter;
            this.position = position;
            this.viewModel = sharedViewModel;
        }

        @Override
        protected Boolean doInBackground(E4Session... sessions) {
            final E4Session e4Session = sessions[0];
            final String sessionId = e4Session.getId();
            final String url = "https://www.empatica.com/connect/connect.php/sessions/" + sessionId;
            final Request request = new Request.Builder().url(url).delete().build();

            try {
                if (Utils.isSessionDownloaded(e4Session)) {
                    if (new File(MainActivity.context.getFilesDir(), e4Session.getZIPFilename()).delete()) {
                        publishProgress("Deleted local data.");
                    } else {
                        publishProgress("Failed to delete local data.");
                    }
                }

                publishProgress("Deleting session in Empatica cloud..");

                if (MainActivity.okHttpClient.newCall(request).execute().isSuccessful()) {
                    publishProgress("Deleted remote data.");
                } else {
                    publishProgress("Failed to delete remote data.");
                }

                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            viewModel.getSessionStatus().setValue(values[0]);
        }

        @Override
        protected void onPostExecute(Boolean done) {

            if (done) {
                viewModel.getSessionStatus().setValue("Session deleted.");
                viewModel.getE4Sessions().remove(position);
                adapter.notifyItemRemoved(position);
            }

        }

    }

    // we cannot afford to load BVP and ACC data into memory for sessions longer than about 8 hours
    private static class LoadAndViewSessionData extends AsyncTask<E4Session, String, Boolean> {

        final SharedViewModel viewModel = ViewModelProviders.of(MainActivity.context).get(SharedViewModel.class);
        final E4SessionData e4SessionData = viewModel.getSesssionData();

        WeakReference<View> view;

        LoadAndViewSessionData(View v) {
            view = new WeakReference<>(v);
        }

        @Override
        protected Boolean doInBackground(E4Session... e4Sessions) {
            final E4Session e4Session = e4Sessions[0];

            publishProgress(String.format("Loading session %s data..", e4Session.getId()));

            if (Utils.isSessionDownloaded(e4Session)) {

                try {
                    final File sessionFile = new File(MainActivity.context.getFilesDir(), e4Session.getZIPFilename());

                    Log.d(MainActivity.TAG, "reading " + e4Session.getZIPFilename());

                    String basePath = MainActivity.context.getCacheDir().getPath();

                    Log.d(MainActivity.TAG, "extracting to directory " + basePath);

                    new ZipFile(sessionFile.getAbsolutePath()).extractAll(basePath);

                    basePath += File.separator;

                    /*
                    final File ibiFile = new File(basePath + "IBI.csv");
                    final File accFile = new File(basePath + "ACC.csv");
                     */

                    final File tagFile = new File(basePath + "tags.csv");

                    publishProgress("Processing tag data");

                    try (BufferedReader reader = new BufferedReader(new FileReader(tagFile))) {
                        String line;

                        while ((line = reader.readLine()) != null) {
                            Log.d(MainActivity.TAG, "loaded tag " + line);

                            e4SessionData.getTags().add(Double.parseDouble(line));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }

                    // same file format for EDA, HR, BVP, TEMP
                    final File edaFile = new File(basePath + "EDA.csv");
                    final File tempFile = new File(basePath + "TEMP.csv");
                    //   final File bvpFile = new File(basePath + "BVP.csv");
                    final File hrFile = new File(basePath + "HR.csv");

                    CSVFile data;

                    publishProgress("Processing EDA data");

                    data = new CSVFile(new FileInputStream(edaFile));
                    e4SessionData.setInitialTime((long) data.getInitialTime());
                    e4SessionData.setGsrTimestamps(data.getX());
                    e4SessionData.setGsr(data.getY());
                    edaFile.delete();

                    publishProgress("Processing temperature data");

                    data = new CSVFile(new FileInputStream(tempFile));
                    e4SessionData.setTempTimestamps(data.getX());
                    e4SessionData.setTemp(data.getY());
                    tempFile.delete();

                    /*
                    data = new CSVFile(new FileInputStream(bvpFile));
                    sessionData.setBvpTimestamps(data.getX());
                    sessionData.setBvp(data.getY());
                    bvpFile.delete();
                    */

                    publishProgress("Processing HR data");

                    data = new CSVFile(new FileInputStream(hrFile));
                    e4SessionData.setHrTimestamps(data.getX());
                    e4SessionData.setHr(data.getY());
                    hrFile.delete();

                    /*
                    data = new CSVFile(new FileInputStream(ibiFile));
                    sessionData.setIbiTimestamps(data.getX());
                    sessionData.setIbi(data.getY());
                    ibiFile.delete();
                    */

                    e4SessionData.setInitialTime(e4Session.getStartTime());

                    publishProgress(String.format("Loaded data for session %s", e4Session.getId()));

                } catch (FileNotFoundException | ZipException e) {
                    e.printStackTrace();
                    return false;
                }
            } else {
                publishProgress("Session data not downloaded!");
            }

            return true;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            if (values.length > 0) viewModel.getSessionStatus().setValue(values[0]);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success && view.get() != null)
                Navigation.findNavController(view.get()).navigate(R.id.nav_charts);
        }
    }
}
