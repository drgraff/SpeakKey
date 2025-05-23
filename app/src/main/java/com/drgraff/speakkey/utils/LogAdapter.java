package com.drgraff.speakkey.utils;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drgraff.speakkey.R; // Make sure R is imported from the correct package

import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

    private List<LogEntry> logEntries;

    public LogAdapter(List<LogEntry> logEntries) {
        this.logEntries = logEntries;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        LogEntry entry = logEntries.get(position);

        holder.timestampTextView.setText(entry.getFormattedTimestamp());
        holder.levelTextView.setText(entry.level);
        holder.messageTextView.setText(entry.message);
        
        if (entry.detail != null && !entry.detail.isEmpty()) {
            holder.detailTextView.setText(entry.detail);
            holder.detailTextView.setVisibility(View.VISIBLE);
        } else {
            holder.detailTextView.setVisibility(View.GONE);
        }

        // Set color based on log level
        switch (entry.level.toUpperCase()) {
            case "ERROR":
                holder.levelTextView.setTextColor(Color.RED);
                break;
            case "SUCCESS":
                holder.levelTextView.setTextColor(Color.parseColor("#006400")); // Dark Green
                break;
            case "INFO":
                holder.levelTextView.setTextColor(Color.BLUE);
                break;
            default:
                holder.levelTextView.setTextColor(Color.BLACK);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return logEntries != null ? logEntries.size() : 0;
    }

    public void updateLogEntries(List<LogEntry> newLogEntries) {
        this.logEntries = newLogEntries;
        notifyDataSetChanged(); // Consider using DiffUtil for more complex scenarios
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView timestampTextView;
        TextView levelTextView;
        TextView messageTextView;
        TextView detailTextView;

        LogViewHolder(View itemView) {
            super(itemView);
            timestampTextView = itemView.findViewById(R.id.log_timestamp);
            levelTextView = itemView.findViewById(R.id.log_level);
            messageTextView = itemView.findViewById(R.id.log_message);
            detailTextView = itemView.findViewById(R.id.log_detail);
        }
    }
}
