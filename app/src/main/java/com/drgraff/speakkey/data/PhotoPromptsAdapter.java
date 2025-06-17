package com.drgraff.speakkey.data;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import androidx.core.graphics.ColorUtils;
import androidx.preference.PreferenceManager;
import com.drgraff.speakkey.utils.DynamicThemeApplicator;
import com.drgraff.speakkey.utils.ThemeManager;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.drgraff.speakkey.R;
// TODO: Create PhotoPromptEditorActivity and uncomment this. For now, it's in ui.prompts
import com.drgraff.speakkey.ui.prompts.PhotoPromptEditorActivity; // This will need to be changed to PromptEditorActivity eventually
import com.drgraff.speakkey.data.Prompt; // Added
import com.drgraff.speakkey.data.PromptManager; // Added

import java.util.ArrayList;
import java.util.List;

public class PhotoPromptsAdapter extends RecyclerView.Adapter<PhotoPromptsAdapter.PhotoPromptViewHolder> {

    private List<Prompt> promptsList; // Changed
    private final PromptManager promptManager; // Changed
    private final Context context;
    private final OnPhotoPromptInteractionListener listener;

    public interface OnPhotoPromptInteractionListener {
        void onEditPhotoPrompt(Prompt prompt); // Changed
        // Delete and toggle can remain handled by adapter if direct manager access is fine
        // Or they can be moved to listener as well for consistency.
        // For now, only edit is through listener as it needs to start activity for result.
    }

    public PhotoPromptsAdapter(Context context, List<Prompt> promptsList, PromptManager promptManager, OnPhotoPromptInteractionListener listener) { // Changed
        this.context = context;
        this.promptsList = promptsList != null ? promptsList : new ArrayList<>(); // Changed
        this.promptManager = promptManager; // Changed
        this.listener = listener;
    }

    @NonNull
    @Override
    public PhotoPromptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_photo_prompt, parent, false);
        return new PhotoPromptViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoPromptViewHolder holder, int position) {
        Prompt currentPrompt = promptsList.get(position); // Changed

        holder.photoPromptLabelTextView.setText(currentPrompt.getLabel());
        holder.photoPromptTextTextView.setText(currentPrompt.getText());

        // OLED Theming for SwitchCompat
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String currentTheme = prefs.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);

        if (ThemeManager.THEME_OLED.equals(currentTheme)) {
            int accentColor = prefs.getInt("pref_oled_accent_general", DynamicThemeApplicator.DEFAULT_OLED_ACCENT_GENERAL);
            int secondaryTextColor = prefs.getInt("pref_oled_general_text_secondary", DynamicThemeApplicator.DEFAULT_OLED_GENERAL_TEXT_SECONDARY);
            int primaryTextColor = prefs.getInt("pref_oled_general_text_primary", DynamicThemeApplicator.DEFAULT_OLED_GENERAL_TEXT_PRIMARY); // For unchecked thumb

            ColorStateList thumbTintList = new ColorStateList(
                new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{-android.R.attr.state_checked}
                },
                new int[]{
                    accentColor,        // Checked
                    primaryTextColor    // Unchecked
                }
            );

            ColorStateList trackTintList = new ColorStateList(
                new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{-android.R.attr.state_checked}
                },
                new int[]{
                    ColorUtils.setAlphaComponent(accentColor, 128),        // Checked (50% alpha)
                    ColorUtils.setAlphaComponent(secondaryTextColor, 128) // Unchecked (50% alpha)
                }
            );

            holder.photoPromptActiveSwitch.setThumbTintList(thumbTintList);
            holder.photoPromptActiveSwitch.setTrackTintList(trackTintList);
        }

        holder.photoPromptActiveSwitch.setOnCheckedChangeListener(null);
        holder.photoPromptActiveSwitch.setChecked(currentPrompt.isActive());
        holder.photoPromptActiveSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) { // Check position validity
                Prompt promptToUpdate = promptsList.get(holder.getAdapterPosition()); // Changed
                promptToUpdate.setActive(isChecked);
                promptManager.updatePrompt(promptToUpdate); // Changed
            }
        });

        holder.editPhotoPromptButton.setOnClickListener(v -> {
            if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                 Prompt promptToEdit = promptsList.get(holder.getAdapterPosition()); // Changed
                listener.onEditPhotoPrompt(promptToEdit);
            }
        });

        holder.deletePhotoPromptButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                Prompt promptToDelete = promptsList.get(adapterPosition); // Changed
                promptManager.deletePrompt(promptToDelete.getId()); // Changed
                promptsList.remove(adapterPosition); // Changed
                notifyItemRemoved(adapterPosition);
                notifyItemRangeChanged(adapterPosition, promptsList.size() - adapterPosition); // Changed
                String message = "Photo Prompt deleted"; // Consider using string resource
                if (promptToDelete.getLabel() != null && !promptToDelete.getLabel().trim().isEmpty()) {
                    message += ": " + promptToDelete.getLabel();
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return promptsList != null ? promptsList.size() : 0; // Changed
    }

    public void setPrompts(List<Prompt> newPrompts) { // Changed
        this.promptsList.clear(); // Changed
        if (newPrompts != null) {
            this.promptsList.addAll(newPrompts); // Changed
        }
        notifyDataSetChanged();
    }

    public Prompt getPromptAt(int position) { // Changed
        if (position >= 0 && position < promptsList.size()) { // Changed
            return promptsList.get(position); // Changed
        }
        return null;
    }

    static class PhotoPromptViewHolder extends RecyclerView.ViewHolder {
        TextView photoPromptLabelTextView;
        TextView photoPromptTextTextView; // For displaying the prompt text
        SwitchCompat photoPromptActiveSwitch;
        ImageButton editPhotoPromptButton;
        ImageButton deletePhotoPromptButton;

        PhotoPromptViewHolder(View itemView) {
            super(itemView);
            // Placeholder IDs - these will need to match the actual layout file
            photoPromptLabelTextView = itemView.findViewById(R.id.photo_prompt_label_text_view);
            photoPromptTextTextView = itemView.findViewById(R.id.photo_prompt_text_text_view);
            photoPromptActiveSwitch = itemView.findViewById(R.id.photo_prompt_active_switch);
            editPhotoPromptButton = itemView.findViewById(R.id.photo_prompt_edit_button);
            deletePhotoPromptButton = itemView.findViewById(R.id.photo_prompt_delete_button);
        }
    }
}
