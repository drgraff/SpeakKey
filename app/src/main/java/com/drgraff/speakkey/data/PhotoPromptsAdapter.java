package com.drgraff.speakkey.data;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.drgraff.speakkey.R;
// TODO: Create PhotoPromptEditorActivity and uncomment this. For now, it's in ui.prompts
import com.drgraff.speakkey.ui.prompts.PhotoPromptEditorActivity;

import java.util.ArrayList;
import java.util.List;

public class PhotoPromptsAdapter extends RecyclerView.Adapter<PhotoPromptsAdapter.PhotoPromptViewHolder> {

    private List<PhotoPrompt> photoPromptsList;
    private final PhotoPromptManager photoPromptManager; // Kept for direct delete and toggle
    private final Context context;
    private final OnPhotoPromptInteractionListener listener;

    public interface OnPhotoPromptInteractionListener {
        void onEditPhotoPrompt(PhotoPrompt photoPrompt);
        // Delete and toggle can remain handled by adapter if direct manager access is fine
        // Or they can be moved to listener as well for consistency.
        // For now, only edit is through listener as it needs to start activity for result.
    }

    public PhotoPromptsAdapter(Context context, List<PhotoPrompt> photoPromptsList, PhotoPromptManager photoPromptManager, OnPhotoPromptInteractionListener listener) {
        this.context = context;
        this.photoPromptsList = photoPromptsList != null ? photoPromptsList : new ArrayList<>();
        this.photoPromptManager = photoPromptManager;
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
        PhotoPrompt currentPrompt = photoPromptsList.get(position);

        holder.photoPromptLabelTextView.setText(currentPrompt.getLabel());
        holder.photoPromptTextTextView.setText(currentPrompt.getText());

        holder.photoPromptActiveSwitch.setOnCheckedChangeListener(null);
        holder.photoPromptActiveSwitch.setChecked(currentPrompt.isActive());
        holder.photoPromptActiveSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) { // Check position validity
                PhotoPrompt promptToUpdate = photoPromptsList.get(holder.getAdapterPosition());
                promptToUpdate.setActive(isChecked);
                photoPromptManager.updatePhotoPrompt(promptToUpdate);
            }
        });

        holder.editPhotoPromptButton.setOnClickListener(v -> {
            if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                 PhotoPrompt promptToEdit = photoPromptsList.get(holder.getAdapterPosition());
                listener.onEditPhotoPrompt(promptToEdit);
            }
        });

        holder.deletePhotoPromptButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                PhotoPrompt promptToDelete = photoPromptsList.get(adapterPosition);
                photoPromptManager.deletePhotoPrompt(promptToDelete.getId());
                photoPromptsList.remove(adapterPosition);
                notifyItemRemoved(adapterPosition);
                notifyItemRangeChanged(adapterPosition, photoPromptsList.size() - adapterPosition);
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
        return photoPromptsList != null ? photoPromptsList.size() : 0;
    }

    public void setPhotoPrompts(List<PhotoPrompt> newPhotoPrompts) {
        this.photoPromptsList.clear();
        if (newPhotoPrompts != null) {
            this.photoPromptsList.addAll(newPhotoPrompts);
        }
        notifyDataSetChanged();
    }

    public PhotoPrompt getPhotoPromptAt(int position) {
        if (position >= 0 && position < photoPromptsList.size()) {
            return photoPromptsList.get(position);
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
