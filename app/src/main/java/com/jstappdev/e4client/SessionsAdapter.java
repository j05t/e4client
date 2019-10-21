package com.jstappdev.e4client;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.jstappdev.e4client.data.CSVFile;
import com.jstappdev.e4client.data.Session;
import com.jstappdev.e4client.data.SessionData;
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
            final Session session = sharedViewModel.getSessions().get(position);
            final String sessionId = session.getId();

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(v.getContext());

            alertDialogBuilder.setTitle("Session " + sessionId);
            alertDialogBuilder.setIcon(android.R.drawable.ic_dialog_info);

            // set dialog message
            alertDialogBuilder
                    .setMessage("Start: " + session.getStartDate() + "\nDuration: " + session.getDurationAsString())
                    .setCancelable(true)
                    .setPositiveButton("Share", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Toast.makeText(v.getContext(), "Share session " + sessionId, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNeutralButton("View Data", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            new LoadAndViewSessionData(v).execute(session);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();

            alertDialog.show();
        }
    };

    private final View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            final int position = (int) v.getTag();
            final Session session = sharedViewModel.getSessions().get(position);
            final String sessionId = session.getId();

            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(v.getContext());

            alertDialogBuilder.setTitle("Delete Session " + sessionId);
            alertDialogBuilder.setIcon(android.R.drawable.ic_delete);

            alertDialogBuilder
                    .setMessage(String.format("Start: %s\nDuration: %s", session.getStartDate(), session.getDurationAsString()))
                    .setCancelable(true)
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            new DeleteSession(instance, sharedViewModel, position).execute(sessionId);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            alertDialogBuilder.create().show();

            return false;
        }
    };


    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, final int position) {
        final Session s = sharedViewModel.getSessions().get(position);

        holder.itemView.setOnClickListener(onClickListener);
        holder.itemView.setOnLongClickListener(onLongClickListener);
        holder.itemView.setTag(position);

        holder.id.setText(s.getId());
        holder.start_time.setText(s.getStartDate());
        holder.duration.setText(s.getDurationAsString());
        holder.device_id.setText(s.getDeviceId());
        holder.label.setText(s.getLabel());
        holder.device.setText(s.getDevice());

        if (Utils.isSessionDownloaded(s)) {
            holder.itemView.setBackgroundColor(Color.parseColor("#1cdefce0"));
        }
    }

    @Override
    public int getItemCount() {
        return sharedViewModel.getSessions().size();
    }


    private static class DeleteSession extends AsyncTask<String, Void, Boolean> {

        private SessionsAdapter adapter;
        private SharedViewModel viewModel;
        private int position;

        DeleteSession(final SessionsAdapter sessionsAdapter, SharedViewModel sharedViewModel, int position) {
            this.adapter = sessionsAdapter;
            this.position = position;
            this.viewModel = sharedViewModel;
        }

        @Override
        protected Boolean doInBackground(String... ids) {
            final String sessionId = ids[0];
            final String url = "https://www.empatica.com/connect/connect.php/sessions/" + sessionId;
            final Request request = new Request.Builder().url(url).delete().build();

            try {
                return MainActivity.okHttpClient.newCall(request).execute().isSuccessful();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            String s;

            if (success) {
                s = "Deleted session.";
                viewModel.getSessions().remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, viewModel.getSessions().size());
            } else {
                s = "FAILED to delete session.";
            }
            viewModel.getSessionStatus().setValue(s);
        }

    }

    // we cannot afford to load BVP and ACC data into memory for sessions longer than about 8 hours
    private static class LoadAndViewSessionData extends AsyncTask<Session, String, Boolean> {

        final SharedViewModel viewModel = ViewModelProviders.of(MainActivity.context).get(SharedViewModel.class);
        final SessionData sessionData = viewModel.getSesssionData();

        WeakReference<View> view;

        LoadAndViewSessionData(View v) {
            view = new WeakReference<>(v);
        }

        @Override
        protected Boolean doInBackground(Session... sessions) {
            final Session session = sessions[0];

            publishProgress(String.format("Loading session %s data..", session.getId()));

            if (Utils.isSessionDownloaded(session)) {

                try {
                    final File sessionFile = new File(MainActivity.context.getFilesDir(), session.getZIPFilename());

                    Log.d(MainActivity.TAG, "reading " + session.getZIPFilename());

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

                            sessionData.getTags().add(Double.parseDouble(line));
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
                    sessionData.setInitialTime((long) data.getInitialTime());
                    sessionData.setGsrTimestamps(data.getX());
                    sessionData.setGsr(data.getY());
                    edaFile.delete();

                    publishProgress("Processing temperature data");

                    data = new CSVFile(new FileInputStream(tempFile));
                    sessionData.setTempTimestamps(data.getX());
                    sessionData.setTemp(data.getY());
                    tempFile.delete();

                    /*
                    data = new CSVFile(new FileInputStream(bvpFile));
                    sessionData.setBvpTimestamps(data.getX());
                    sessionData.setBvp(data.getY());
                    bvpFile.delete();
                    */

                    publishProgress("Processing HR data");

                    data = new CSVFile(new FileInputStream(hrFile));
                    sessionData.setHrTimestamps(data.getX());
                    sessionData.setHr(data.getY());
                    hrFile.delete();

                    /*
                    data = new CSVFile(new FileInputStream(ibiFile));
                    sessionData.setIbiTimestamps(data.getX());
                    sessionData.setIbi(data.getY());
                    ibiFile.delete();
                    */

                    sessionData.setInitialTime(session.getStartTime());

                    publishProgress(String.format("Loaded data for session %s", session.getId()));

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
