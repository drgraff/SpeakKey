package com.drgraff.speakkey.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections; // For synchronizedList if needed, though modifying copies

public class PromptManager {
    private static final String PREFS_NAME = "SpeakKeyPrefs";
    private static final String PROMPTS_KEY = "UserPrompts";
    private SharedPreferences sharedPreferences;
    private Gson gson;

    public PromptManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public List<Prompt> getPrompts() {
        String json = sharedPreferences.getString(PROMPTS_KEY, null);
        if (json == null) {
            return new ArrayList<>(); // Return empty list if no prompts saved
        }
        Type type = new TypeToken<ArrayList<Prompt>>() {}.getType();
        List<Prompt> prompts = gson.fromJson(json, type);
        return prompts != null ? prompts : new ArrayList<>();
    }

    private void savePrompts(List<Prompt> prompts) {
        String json = gson.toJson(prompts);
        sharedPreferences.edit().putString(PROMPTS_KEY, json).apply();
    }

    public void addPrompt(String text, String label) {
        List<Prompt> prompts = getPrompts();
        long newId = System.currentTimeMillis(); // Simple unique ID
        prompts.add(new Prompt(newId, text, false, label)); // New prompts are inactive by default
        savePrompts(prompts);
    }

    public void updatePrompt(Prompt promptToUpdate) {
        List<Prompt> prompts = getPrompts();
        for (int i = 0; i < prompts.size(); i++) {
            if (prompts.get(i).getId() == promptToUpdate.getId()) {
                prompts.set(i, promptToUpdate);
                savePrompts(prompts);
                return;
            }
        }
    }

    public void deletePrompt(long promptId) {
        List<Prompt> prompts = getPrompts();
        prompts.removeIf(prompt -> prompt.getId() == promptId);
        savePrompts(prompts);
    }
    
    // Optional: A method to toggle active state directly might be useful
    public void togglePromptActiveStatus(long promptId) {
        List<Prompt> prompts = getPrompts();
        for (Prompt prompt : prompts) {
            if (prompt.getId() == promptId) {
                prompt.setActive(!prompt.isActive());
                break; 
            }
        }
        savePrompts(prompts);
    }
}
