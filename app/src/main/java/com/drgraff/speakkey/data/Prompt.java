package com.drgraff.speakkey.data;

import java.util.Objects;

public class Prompt {
    private long id; // Using System.currentTimeMillis() for simplicity
    private String text;
    private boolean isActive;
    private String label;

    // Default constructor for GSON
    public Prompt() {
    }

    public Prompt(long id, String text, boolean isActive, String label) {
        this.id = id;
        this.text = text;
        this.isActive = isActive;
        this.label = label;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Prompt prompt = (Prompt) o;
        return id == prompt.id &&
                isActive == prompt.isActive &&
                Objects.equals(text, prompt.text) &&
                Objects.equals(label, prompt.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, text, isActive, label);
    }

    @Override
    public String toString() {
        return "Prompt{" +
                "id=" + id +
                ", text='" + text + '\'' +
                ", isActive=" + isActive +
                ", label='" + label + '\'' +
                '}';
    }
}
