package com.drgraff.speakkey.utils;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import com.drgraff.speakkey.utils.ThemeManager;
import com.drgraff.speakkey.utils.DynamicThemeApplicator;
import androidx.core.content.ContextCompat; // For potential fallback colors

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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(holder.itemView.getContext());
        String currentTheme = prefs.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
        boolean isOledMode = ThemeManager.THEME_OLED.equals(currentTheme);

        int oledPrimaryTextColor = 0;
        int oledSecondaryTextColor = 0;
        if (isOledMode) {
            oledPrimaryTextColor = prefs.getInt("pref_oled_general_text_primary", DynamicThemeApplicator.DEFAULT_OLED_GENERAL_TEXT_PRIMARY);
            oledSecondaryTextColor = prefs.getInt("pref_oled_general_text_secondary", DynamicThemeApplicator.DEFAULT_OLED_GENERAL_TEXT_SECONDARY);

            holder.timestampTextView.setTextColor(oledSecondaryTextColor);
            holder.messageTextView.setTextColor(oledPrimaryTextColor);
            holder.detailTextView.setTextColor(oledPrimaryTextColor); // Or secondary if preferred
        } else {
            // Optional: Explicitly set to default theme colors if not OLED.
            // This example assumes TextViews will inherit appropriate colors from the base theme.
            // If specific default colors are needed, they can be set here using ContextCompat.getColor.
            // For instance:
            // holder.timestampTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.default_log_timestamp_color));
            // holder.messageTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.default_log_message_color));
            // holder.detailTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.default_log_detail_color));
            // For now, we rely on base theme inheritance for non-OLED.
        }

        // Set color based on log level
        if (isOledMode) {
            switch (entry.level.toUpperCase()) {
                case "ERROR":
                    holder.levelTextView.setTextColor(Color.parseColor("#FF5252")); // Brighter Red for OLED
                    break;
                case "SUCCESS":
                    holder.levelTextView.setTextColor(Color.parseColor("#69F0AE")); // Brighter Green for OLED
                    break;
                case "INFO":
                    int infoColor = prefs.getInt("pref_oled_accent_general", DynamicThemeApplicator.DEFAULT_OLED_ACCENT_GENERAL);
                    holder.levelTextView.setTextColor(infoColor);
                    break;
                default: // WARN, DEBUG, etc.
                    holder.levelTextView.setTextColor(oledPrimaryTextColor); // Default to primary OLED text color
                    break;
            }
        } else {
            // Existing non-OLED color logic
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
                default: // WARN, DEBUG, etc.
                    holder.levelTextView.setTextColor(Color.BLACK);
                    break;
            }
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
