package com.jstappdev.e4client;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.jstappdev.e4client.data.E4Session;
import com.jstappdev.e4client.util.DownloadSessions;
import com.jstappdev.e4client.util.LoadAndViewSessionData;
import com.squareup.okhttp.Request;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class SessionsAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<SessionsAdapter.MyViewHolder> {

    private final SharedViewModel sharedViewModel;
    private final WeakReference<MainActivity> contextRef;
    private final SessionsAdapter instance;

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int position = (int) v.getTag();

            if (position >= sharedViewModel.getE4Sessions().size()) return;

            final E4Session e4Session = sharedViewModel.getE4Sessions().get(position);

            new AlertDialog.Builder(v.getContext())
                    .setTitle("Session " + e4Session.getId())
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setMessage(String.format("Start: %s\nDuration: %s", e4Session.getStartDate(), e4Session.getDurationAsString()))
                    .setCancelable(true)
                    .setPositiveButton("Share", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (sharedViewModel.isSessionDownloaded(e4Session)) {
                                final File file = new File(contextRef.get().getFilesDir(), e4Session.getZIPFilename());

                                contextRef.get().startActivity(new Intent()
                                        .setAction(Intent.ACTION_SEND)
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(contextRef.get(), contextRef.get().getApplicationContext().getPackageName() + ".provider", file))
                                        .setType("application/zip"));
                            } else {
                                sharedViewModel.getCurrentStatus().postValue(String.format("Session %s not downloaded.", e4Session.getId()));
                            }
                            dialog.dismiss();
                        }
                    })
                    .setNeutralButton("View Data", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //noinspection ConstantConditions
                            if (!sharedViewModel.getIsConnected().getValue()) {
                                new LoadAndViewSessionData(contextRef.get()).execute(e4Session);
                            } else {
                                sharedViewModel.getCurrentStatus().postValue("Not available while streaming.");
                            }
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
                    .setTitle("Edit Session " + e4Session.getId()).setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(String.format("Start: %s\nDuration: %s", e4Session.getStartDate(), e4Session.getDurationAsString()))
                    .setCancelable(true)
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            sharedViewModel.getE4Sessions().remove(position);
                            instance.notifyDataSetChanged();
                            instance.notifyItemRemoved(position);

                            if (e4Session.isDownloaded() || e4Session.isUploaded())
                                new DeleteSession(sharedViewModel).execute(e4Session);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    }).setNeutralButton("(Re)download", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    final File file = new File(contextRef.get().getFilesDir(), e4Session.getZIPFilename());

                    Log.d(MainActivity.TAG, "deleting session " + e4Session);

                    if (!file.delete()) {
                        sharedViewModel.setSessionStatus("Failed to delete session data.");
                    }

                    final ArrayList<E4Session> downloadMe = new ArrayList<>();
                    downloadMe.add(e4Session);

                    //noinspection unchecked
                    new DownloadSessions(instance, contextRef.get()).execute(downloadMe);

                    dialog.dismiss();
                }
            }).create().show();

            return false;
        }
    };

    public SessionsAdapter(final Context context) {
        contextRef = new WeakReference<>((MainActivity) context);
        sharedViewModel = new ViewModelProvider(contextRef.get()).get(SharedViewModel.class);
        instance = this;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sessions_list_item, parent, false);

        return new MyViewHolder(v);
    }

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

        if (sharedViewModel.isSessionDownloaded(e4Session)) {
            holder.isDownloaded.setCheckMarkDrawable(android.R.drawable.checkbox_on_background);
            holder.isDownloaded.setChecked(true);
            e4Session.setIsDownloaded(true);
        } else {
            holder.isDownloaded.setCheckMarkDrawable(android.R.drawable.checkbox_off_background);
            holder.isDownloaded.setChecked(false);
            e4Session.setIsDownloaded(false);
        }

        if (sharedViewModel.getUploadedSessionIDs().contains(e4Session.getId())) {
            holder.isUploaded.setCheckMarkDrawable(android.R.drawable.checkbox_on_background);
            holder.isUploaded.setChecked(true);
            e4Session.setIsUploaded(true);
        } else {
            holder.isUploaded.setCheckMarkDrawable(android.R.drawable.checkbox_off_background);
            holder.isUploaded.setChecked(false);
            e4Session.setIsUploaded(false);
        }
    }

    @Override
    public int getItemCount() {
        return sharedViewModel.getE4Sessions().size();
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
        CheckedTextView isDownloaded;
        CheckedTextView isUploaded;

        MyViewHolder(View v) {
            super(v);

            id = v.findViewById(R.id.session_id);
            start_time = v.findViewById(R.id.session_start_time);
            duration = v.findViewById(R.id.session_duration);
            device_id = v.findViewById(R.id.session_device_id);
            label = v.findViewById(R.id.session_label);
            device = v.findViewById(R.id.session_device);
            isDownloaded = v.findViewById(R.id.isDownloaded);
            isUploaded = v.findViewById(R.id.isUploaded);
        }
    }

    private static class DeleteSession extends AsyncTask<E4Session, String, Boolean> {

        private SharedViewModel viewModel;

        DeleteSession(SharedViewModel sharedViewModel) {
            this.viewModel = sharedViewModel;
        }

        @Override
        protected Boolean doInBackground(E4Session... sessions) {
            final E4Session e4Session = sessions[0];
            final String sessionId = e4Session.getId();
            final String url = "https://www.empatica.com/connect/connect.php/sessions/" + sessionId;
            final Request request = new Request.Builder().url(url).delete().build();

            try {
                if (viewModel.isSessionDownloaded(e4Session)) {
                    if (new File(viewModel.getFilesDir(), e4Session.getZIPFilename()).delete()) {
                        publishProgress("Deleted local data.");
                    } else {
                        publishProgress("Failed to delete local data.");
                    }
                }

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

            viewModel.getCurrentStatus().setValue(values[0]);
        }

        @Override
        protected void onPostExecute(Boolean done) {

            if (done) {
                viewModel.getCurrentStatus().setValue("Session deleted.");
            }

        }

    }


}
