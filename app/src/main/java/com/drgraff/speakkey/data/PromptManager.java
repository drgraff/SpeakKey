package com.drgraff.speakkey.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log; // Added import
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections; // For synchronizedList if needed, though modifying copies
// Assuming PhotoPrompt class is in the same package or imported if not.
// If it's in com.drgraff.speakkey.data.PhotoPrompt, it's fine.

public class PromptManager {
    private static final String PREFS_NAME = "SpeakKeyPrefs";
    private static final String PROMPTS_KEY = "UserPrompts";
    private static final String PHOTO_PROMPTS_MIGRATED_KEY = "photo_prompts_migrated_v1";
    private SharedPreferences sharedPreferences;
    private Gson gson;

    public PromptManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        migratePhotoPromptsIfNeeded(context);
    }

    private synchronized void migratePhotoPromptsIfNeeded(Context context) {
        if (sharedPreferences.getBoolean(PHOTO_PROMPTS_MIGRATED_KEY, false)) {
            Log.i("PromptManager", "Photo prompt migration already performed.");
            return;
        }

        SharedPreferences photoPrefs = context.getSharedPreferences("photo_prompts_prefs", Context.MODE_PRIVATE);
        String jsonPhotoPrompts = photoPrefs.getString("key_photo_prompts_list", null);
        boolean migrationPerformed = false;

        if (jsonPhotoPrompts != null && !jsonPhotoPrompts.isEmpty()) {
            Type type = new TypeToken<ArrayList<PhotoPrompt>>() {}.getType(); // Assumes PhotoPrompt.java still exists
            List<PhotoPrompt> oldPhotoPrompts = gson.fromJson(jsonPhotoPrompts, type);

            if (oldPhotoPrompts != null && !oldPhotoPrompts.isEmpty()) {
                List<Prompt> mainPrompts = getAllPrompts(); // Ensure this list is mutable (it is, as it's new ArrayList from getAllPrompts)

                for (PhotoPrompt oldPhotoPrompt : oldPhotoPrompts) {
                    long newId = System.currentTimeMillis();
                    try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } // Ensure unique timestamp for ID

                    Prompt migratedPrompt = new Prompt(
                        newId,
                        oldPhotoPrompt.getText(),
                        oldPhotoPrompt.isActive(),
                        oldPhotoPrompt.getLabel(),
                        "photo_vision", // promptModeType
                        oldPhotoPrompt.getTimestamp() == 0 ? newId : oldPhotoPrompt.getTimestamp() // Use existing timestamp or newId as timestamp
                    );
                    mainPrompts.add(migratedPrompt);
                    migrationPerformed = true;
                }

                if (migrationPerformed) {
                    savePrompts(mainPrompts);
                    Log.i("PromptManager", "Successfully migrated " + oldPhotoPrompts.size() + " photo prompts.");
                    photoPrefs.edit().remove("key_photo_prompts_list").apply(); // Clear old data
                }
            }
        }
        sharedPreferences.edit().putBoolean(PHOTO_PROMPTS_MIGRATED_KEY, true).apply();
        if (migrationPerformed) {
             Log.i("PromptManager", "Photo prompt migration complete and flag set.");
        } else {
             Log.i("PromptManager", "No photo prompts found to migrate, migration flag set.");
        }
    }

    public List<Prompt> getAllPrompts() {
        String json = sharedPreferences.getString(PROMPTS_KEY, null);
        List<Prompt> prompts;
        if (json == null) {
            prompts = new ArrayList<>();
        } else {
            Type type = new TypeToken<ArrayList<Prompt>>() {}.getType();
            prompts = gson.fromJson(json, type);
            if (prompts == null) {
                prompts = new ArrayList<>();
            }
        }

        // Migration logic
        boolean needsSave = false;
        for (Prompt p : prompts) {
            if (p.getPromptModeType() == null) {
                p.setPromptModeType("two_step_transcription"); // Default for old prompts
                needsSave = true;
            }
        }
        if (needsSave) {
            savePrompts(prompts); // Re-save if any prompts were migrated
        }
        return prompts;
    }

    private void savePrompts(List<Prompt> prompts) {
        String json = gson.toJson(prompts);
        sharedPreferences.edit().putString(PROMPTS_KEY, json).apply();
    }

    public void addPrompt(String text, String label, String promptModeType) {
        List<Prompt> prompts = getAllPrompts();
        long newId = System.currentTimeMillis(); // Simple unique ID
        // Add timestamp to the Prompt constructor call
        prompts.add(new Prompt(newId, text, false, label, promptModeType, newId));
        savePrompts(prompts);
    }

    public void updatePrompt(Prompt promptToUpdate) {
        List<Prompt> prompts = getAllPrompts();
        for (int i = 0; i < prompts.size(); i++) {
            if (prompts.get(i).getId() == promptToUpdate.getId()) {
                prompts.set(i, promptToUpdate);
                savePrompts(prompts);
                return;
            }
        }
    }

    public void deletePrompt(long promptId) {
        List<Prompt> prompts = getAllPrompts();
        prompts.removeIf(prompt -> prompt.getId() == promptId);
        savePrompts(prompts);
    }
    
    public List<Prompt> getPromptsForMode(String promptModeType) {
        List<Prompt> allPrompts = getAllPrompts();
        List<Prompt> filteredPrompts = new ArrayList<>();
        if (promptModeType == null) {
            Log.w("PromptManager", "getPromptsForMode called with null promptModeType, returning all prompts.");
            return allPrompts;
        }
        for (Prompt p : allPrompts) {
            if (promptModeType.equals(p.getPromptModeType()) || (p.getPromptModeType() == null && "two_step_transcription".equals(promptModeType))) {
                // Include if mode matches OR if prompt mode is null (old prompt) and requested mode is the default migration mode.
                // This second part of condition can be removed after migration is deemed complete for all users.
                filteredPrompts.add(p);
            }
        }
        return filteredPrompts;
    }

    // Optional: A method to toggle active state directly might be useful
    public void togglePromptActiveStatus(long promptId) {
        List<Prompt> prompts = getAllPrompts();
        for (Prompt prompt : prompts) {
            if (prompt.getId() == promptId) {
                prompt.setActive(!prompt.isActive());
                break; 
            }
        }
        savePrompts(prompts);
    }
}
