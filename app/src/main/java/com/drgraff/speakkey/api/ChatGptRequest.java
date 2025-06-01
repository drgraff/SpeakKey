package com.drgraff.speakkey.api;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList; // Added
import java.util.List;

public class ChatGptRequest {
    @SerializedName("model")
    private String model;

    @SerializedName("messages")
    private List<Message> messages;

    @SerializedName("max_tokens")
    private Integer maxTokens; // Optional: Add max_tokens for vision requests

    // Constructor for text-only messages for backward compatibility (or can be removed if not needed)
    public ChatGptRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
        this.maxTokens = null; // Default to null if not specified
    }

    // Constructor including max_tokens
    public ChatGptRequest(String model, List<Message> messages, Integer maxTokens) {
        this.model = model;
        this.messages = messages;
        this.maxTokens = maxTokens;
    }


    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public static class ContentPart {
        @SerializedName("type")
        public String type;

        // Add any other common fields or methods if necessary
    }

    public static class TextContentPart extends ContentPart {
        @SerializedName("text")
        public String text;

        public TextContentPart(String text) {
            this.type = "text";
            this.text = text;
        }
    }

    public static class ImageUrl {
        @SerializedName("url")
        public String url;
        // @SerializedName("detail") // Optional: "low", "high", "auto"
        // public String detail;

        public ImageUrl(String url) {
            this.url = url;
            // this.detail = "auto"; // Default detail
        }
    }

    public static class ImageContentPart extends ContentPart {
        @SerializedName("image_url") // Field name must match API
        public ImageUrl imageUrl;

        public ImageContentPart(String imageUrlString) {
            this.type = "image_url";
            this.imageUrl = new ImageUrl(imageUrlString);
        }
    }

    public static class Message {
        @SerializedName("role")
        private String role;

        // Content can now be a list of parts (text or image)
        @SerializedName("content")
        private List<ContentPart> content;

        // Constructor for single text content (backward compatibility)
        public Message(String role, String textContent) {
            this.role = role;
            this.content = new ArrayList<>();
            this.content.add(new TextContentPart(textContent));
        }

        // Constructor for mixed content (list of ContentPart)
        public Message(String role, List<ContentPart> contentParts) {
            this.role = role;
            this.content = contentParts;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public List<ContentPart> getContent() { // Return type changed
            return content;
        }

        public void setContent(List<ContentPart> content) { // Parameter type changed
            this.content = content;
        }
    }
}
// Ensure java.util.ArrayList is imported at the top of the file.
// import java.util.ArrayList;
