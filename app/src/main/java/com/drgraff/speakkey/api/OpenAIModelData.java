package com.drgraff.speakkey.api;

import java.util.List;

public class OpenAIModelData {

    public static class ModelInfo {
        public String id;
        public String object;
        public long created;
        public String owned_by;

        // Getters
        public String getId() { return id; }
        public String getObject() { return object; }
        public long getCreated() { return created; }
        public String getOwned_by() { return owned_by; }
    }

    public static class OpenAIModelsResponse {
        public String object;
        public List<ModelInfo> data;

        // Getters
        public String getObject() { return object; }
        public List<ModelInfo> getData() { return data; }
    }
}
