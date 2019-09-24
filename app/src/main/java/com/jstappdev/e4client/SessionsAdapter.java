package com.jstappdev.e4client;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SessionsAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<SessionsAdapter.MyViewHolder> {

    private List<com.jstappdev.e4client.Session> sessions;


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

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Session s = sessions.get(position);

        holder.id.setText(s.getId());
        holder.start_time.setText(Long.toString(s.getStart_time()));
        holder.duration.setText(Long.toString(s.getDuration()));
        holder.device_id.setText(s.getDevice_id());
        holder.label.setText(s.getLabel());
        holder.device.setText(s.getDevice());

        if (position % 2 != 0)
            holder.itemView.setBackgroundColor(Color.LTGRAY);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }
}
