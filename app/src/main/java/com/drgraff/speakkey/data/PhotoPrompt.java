package com.drgraff.speakkey.data;

import java.util.Objects;

public class PhotoPrompt {
    private long id;
    private String label;
    private String text;
    private boolean isActive;
    private long timestamp;

    // Default constructor (e.g., for GSON or other deserialization libraries)
    public PhotoPrompt() {
    }

    // Constructor to initialize all fields
    public PhotoPrompt(long id, String label, String text, boolean isActive, long timestamp) {
        this.id = id;
        this.label = label;
        this.text = text;
        this.isActive = isActive;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhotoPrompt that = (PhotoPrompt) o;
        return id == that.id &&
                isActive == that.isActive &&
                timestamp == that.timestamp &&
                Objects.equals(label, that.label) &&
                Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, text, isActive, timestamp);
    }

    @Override
    public String toString() {
        return "PhotoPrompt{" +
                "id=" + id +
                ", label='" + label + '\'' +
                ", text='" + text + '\'' +
                ", isActive=" + isActive +
                ", timestamp=" + timestamp +
                '}';
    }
}
