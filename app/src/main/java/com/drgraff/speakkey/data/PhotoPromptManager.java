package com.drgraff.speakkey.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PhotoPromptManager {
    private static final String PHOTO_PROMPTS_PREFS_NAME = "photo_prompts_prefs";
    private static final String PHOTO_PROMPTS_LIST_KEY = "key_photo_prompts_list";

    private SharedPreferences sharedPreferences;
    private Gson gson;

    public PhotoPromptManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PHOTO_PROMPTS_PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public List<PhotoPrompt> getPhotoPrompts() {
        String json = sharedPreferences.getString(PHOTO_PROMPTS_LIST_KEY, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<PhotoPrompt>>() {}.getType();
        List<PhotoPrompt> prompts = gson.fromJson(json, type);
        if (prompts == null) {
            prompts = new ArrayList<>();
        }
        // Sort by timestamp, descending (newest first)
        Collections.sort(prompts, new Comparator<PhotoPrompt>() {
            @Override
            public int compare(PhotoPrompt p1, PhotoPrompt p2) {
                return Long.compare(p2.getTimestamp(), p1.getTimestamp());
            }
        });
        return prompts;
    }

    private void savePhotoPrompts(List<PhotoPrompt> prompts) {
        String json = gson.toJson(prompts);
        sharedPreferences.edit().putString(PHOTO_PROMPTS_LIST_KEY, json).apply();
    }

    // Adds a new PhotoPrompt. ID and Timestamp are generated here.
    public void addPhotoPrompt(String label, String text) {
        List<PhotoPrompt> prompts = getPhotoPrompts();
        long currentTime = System.currentTimeMillis();
        PhotoPrompt newPrompt = new PhotoPrompt(currentTime, label, text, false, currentTime); // id and timestamp from currentTime
        prompts.add(newPrompt);
        savePhotoPrompts(prompts);
    }

    // Adds a PhotoPrompt object directly. Assumes ID and Timestamp might be pre-set (e.g. for import)
    // If ID is 0, it generates one. If timestamp is 0, it sets it.
    public void addPhotoPrompt(PhotoPrompt prompt) {
        List<PhotoPrompt> prompts = getPhotoPrompts();
        if (prompt.getId() == 0) { // Check if ID needs to be generated
            prompt.setId(System.currentTimeMillis());
        }
        if (prompt.getTimestamp() == 0) { // Check if timestamp needs to be set
             prompt.setTimestamp(System.currentTimeMillis());
        }
        // Ensure no duplicate ID by removing if exists, then adding (effectively an upsert for new)
        // More robust would be to check for ID and throw error or merge, but for simplicity:
        prompts.removeIf(p -> p.getId() == prompt.getId());
        prompts.add(prompt);
        savePhotoPrompts(prompts);
    }


    public void updatePhotoPrompt(PhotoPrompt promptToUpdate) {
        List<PhotoPrompt> prompts = getPhotoPrompts();
        for (int i = 0; i < prompts.size(); i++) {
            if (prompts.get(i).getId() == promptToUpdate.getId()) {
                // Preserve original creation timestamp if not explicitly set in promptToUpdate
                if (promptToUpdate.getTimestamp() == 0) {
                    promptToUpdate.setTimestamp(prompts.get(i).getTimestamp());
                }
                prompts.set(i, promptToUpdate);
                savePhotoPrompts(prompts);
                return;
            }
        }
    }

    public void deletePhotoPrompt(long promptId) {
        List<PhotoPrompt> prompts = getPhotoPrompts();
        prompts.removeIf(prompt -> prompt.getId() == promptId);
        savePhotoPrompts(prompts);
    }

    public PhotoPrompt getPhotoPromptById(long promptId) {
        List<PhotoPrompt> prompts = getPhotoPrompts();
        for (PhotoPrompt prompt : prompts) {
            if (prompt.getId() == promptId) {
                return prompt;
            }
        }
        return null;
    }

    public void togglePhotoPromptActiveStatus(long promptId) {
        List<PhotoPrompt> prompts = getPhotoPrompts();
        boolean found = false;
        for (PhotoPrompt prompt : prompts) {
            if (prompt.getId() == promptId) {
                prompt.setActive(!prompt.isActive());
                found = true;
                break;
            }
        }
        if (found) {
            savePhotoPrompts(prompts);
        }
    }
}
