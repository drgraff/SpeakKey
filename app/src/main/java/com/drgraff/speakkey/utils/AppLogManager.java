package com.drgraff.speakkey.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppLogManager {
    private static AppLogManager instance;
    private final List<LogEntry> logEntries;

    private AppLogManager() {
        logEntries = Collections.synchronizedList(new ArrayList<>());
    }

    public static synchronized AppLogManager getInstance() {
        if (instance == null) {
            instance = new AppLogManager();
        }
        return instance;
    }

    public void addEntry(String level, String message, String detail) {
        // Add new entries at the beginning of the list so they appear at the top in the RecyclerView
        logEntries.add(0, new LogEntry(System.currentTimeMillis(), level, message, detail));
    }
    
    public void addEntry(LogEntry entry) {
        logEntries.add(0, entry);
    }

    public List<LogEntry> getEntries() {
        // Return a copy to prevent external modification issues if not using synchronized list directly
        // However, for display in RecyclerView, direct reference might be fine if adapter is notified correctly
        // For simplicity and safety, returning a copy is robust.
        synchronized (logEntries) {
            return new ArrayList<>(logEntries);
        }
    }

    public void clearEntries() {
        logEntries.clear();
    }
}
