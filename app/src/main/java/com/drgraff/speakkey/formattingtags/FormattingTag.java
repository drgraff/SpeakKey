package com.drgraff.speakkey.formattingtags;

import java.util.Objects;

public class FormattingTag {

    private long id;
    private String name;
    private String openingTagText;
    private String closingTagText;
    private String keystrokeSequence;
    private boolean isActive;

    // Default constructor
    public FormattingTag() {
    }

    // Constructor for all fields (id can be set later or auto-generated)
    public FormattingTag(String name, String openingTagText, String closingTagText, String keystrokeSequence, boolean isActive) {
        this.name = name;
        this.openingTagText = openingTagText;
        this.closingTagText = closingTagText;
        this.keystrokeSequence = keystrokeSequence;
        this.isActive = isActive;
    }

    // Constructor including id
    public FormattingTag(long id, String name, String openingTagText, String closingTagText, String keystrokeSequence, boolean isActive) {
        this.id = id;
        this.name = name;
        this.openingTagText = openingTagText;
        this.closingTagText = closingTagText;
        this.keystrokeSequence = keystrokeSequence;
        this.isActive = isActive;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOpeningTagText() {
        return openingTagText;
    }

    public void setOpeningTagText(String openingTagText) {
        this.openingTagText = openingTagText;
    }

    public String getClosingTagText() {
        return closingTagText;
    }

    public void setClosingTagText(String closingTagText) {
        this.closingTagText = closingTagText;
    }

    public String getKeystrokeSequence() {
        return keystrokeSequence;
    }

    public void setKeystrokeSequence(String keystrokeSequence) {
        this.keystrokeSequence = keystrokeSequence;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormattingTag that = (FormattingTag) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "FormattingTag{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", openingTagText='" + openingTagText + '\'' +
                ", closingTagText='" + closingTagText + '\'' +
                ", keystrokeSequence='" + keystrokeSequence + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
