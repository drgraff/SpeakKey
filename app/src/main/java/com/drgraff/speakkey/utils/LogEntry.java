package com.drgraff.speakkey.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogEntry {
    public final long timestamp;
    public final String level; // e.g., "INFO", "ERROR", "SUCCESS"
    public final String message;
    public final String detail;

    public LogEntry(long timestamp, String level, String message, String detail) {
        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
        this.detail = detail;
    }

    public String getFormattedTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
