package com.jstappdev.e4client;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jstappdev.e4client.data.Session;
import com.jstappdev.e4client.ui.SessionsFragment;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.List;

public class SessionsAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<SessionsAdapter.MyViewHolder> {

    private List<Session> sessions;


    public SessionsAdapter(List<Session> sessions) {
        this.sessions = sessions;
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
            final Session session = sessions.get(position);
            final String sessionId = session.getId();

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(v.getContext());

            alertDialogBuilder.setTitle("Session " + sessionId);
            alertDialogBuilder.setIcon(android.R.drawable.ic_dialog_info);

            // set dialog message
            alertDialogBuilder
                    .setMessage("Session Start: " + session.getStartDate() + "\nDuration: " + session.getDurationAsString())
                    .setCancelable(true)
                    .setPositiveButton("Share", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Toast.makeText(v.getContext(), "Share session " + sessionId, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNeutralButton("View", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Toast.makeText(v.getContext(), "View session " + sessionId, Toast.LENGTH_SHORT).show();
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
            final Session session = sessions.get(position);
            final String sessionId = session.getId();

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(v.getContext());

            alertDialogBuilder.setTitle("Delete Session " + sessionId);
            alertDialogBuilder.setIcon(android.R.drawable.ic_delete);

            alertDialogBuilder
                    .setMessage("Session Start: " + session.getStartDate() + "\nDuration: " + session.getDurationAsString())
                    .setCancelable(true)
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            new Utils.DeleteSession(v.getContext(), SessionsFragment.okHttpClient).execute(sessionId);
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

            return false;
        }
    };


    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Session s = sessions.get(position);

        holder.itemView.setOnClickListener(onClickListener);
        holder.itemView.setOnLongClickListener(onLongClickListener);
        holder.itemView.setTag(position);

        holder.id.setText(s.getId());
        holder.start_time.setText(s.getStartDate());
        holder.duration.setText(s.getDurationAsString());
        holder.device_id.setText(s.getDevice_id());
        holder.label.setText(s.getLabel());
        holder.device.setText(s.getDevice());

        //if (position % 2 != 0)
        //    holder.itemView.setBackgroundColor(Color.LTGRAY);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }
}
