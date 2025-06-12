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
import com.drgraff.speakkey.ui.prompts.PromptEditorActivity; // Corrected import path

import java.util.ArrayList;
import java.util.List;

public class PromptsAdapter extends RecyclerView.Adapter<PromptsAdapter.PromptViewHolder> {

    private List<Prompt> prompts;
    private final PromptManager promptManager;
    private final Context context;
    private final OnPromptInteractionListener listener; // Added listener

    // Interface for interaction Callbacks
    public interface OnPromptInteractionListener {
        void onEditPrompt(Prompt prompt);
        // void onDeletePrompt(Prompt prompt); // Example for future
        // void onTogglePromptActive(Prompt prompt, boolean isActive); // Example for future
    }

    // Constructor updated
    public PromptsAdapter(Context context, List<Prompt> prompts, PromptManager promptManager, OnPromptInteractionListener listener) {
        this.context = context;
        this.prompts = prompts != null ? prompts : new ArrayList<>();
        this.promptManager = promptManager;
        this.listener = listener; // Assign listener
    }

    @NonNull
    @Override
    public PromptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_prompt, parent, false);
        return new PromptViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromptViewHolder holder, int position) {
        Prompt currentPrompt = prompts.get(position);

        String label = currentPrompt.getLabel();
        if (label == null || label.trim().isEmpty()) {
            holder.promptLabelTextView.setText(context.getString(R.string.untitled_prompt_label));
        } else {
            holder.promptLabelTextView.setText(label);
        }

        holder.promptActiveSwitch.setChecked(currentPrompt.isActive());

        holder.promptActiveSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            currentPrompt.setActive(isChecked);
            promptManager.updatePrompt(currentPrompt); // Persist the change
            // Optional: Add a Toast or log
            // Toast.makeText(context, "Prompt " + (isChecked ? "activated" : "deactivated"), Toast.LENGTH_SHORT).show();
        });

        holder.editPromptButton.setOnClickListener(v -> {
            if (listener != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                listener.onEditPrompt(prompts.get(holder.getAdapterPosition()));
            }
        });

        holder.deletePromptButton.setOnClickListener(v -> {
            // Get position before removing from list
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                Prompt promptToDelete = prompts.get(adapterPosition); // Get prompt before removing
                promptManager.deletePrompt(promptToDelete.getId());
                prompts.remove(adapterPosition);
                notifyItemRemoved(adapterPosition);
                notifyItemRangeChanged(adapterPosition, prompts.size() - adapterPosition); // Update positions
                String message = context.getString(R.string.prompt_deleted_message);
                if (promptToDelete.getLabel() != null && !promptToDelete.getLabel().trim().isEmpty()) {
                    message += ": " + promptToDelete.getLabel();
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return prompts != null ? prompts.size() : 0;
    }

    public void setPrompts(List<Prompt> newPrompts) {
        this.prompts.clear();
        if (newPrompts != null) {
            this.prompts.addAll(newPrompts);
        }
        notifyDataSetChanged(); // Consider DiffUtil for more complex scenarios
    }

    // ViewHolder updated
    static class PromptViewHolder extends RecyclerView.ViewHolder {
        TextView promptLabelTextView; // Changed from promptTextView
        SwitchCompat promptActiveSwitch; // Changed from CheckBox
        ImageButton editPromptButton;
        ImageButton deletePromptButton;

        PromptViewHolder(View itemView) {
            super(itemView);
            promptLabelTextView = itemView.findViewById(R.id.prompt_label_text_view); // Updated ID
            promptActiveSwitch = itemView.findViewById(R.id.prompt_active_switch); // Updated ID
            editPromptButton = itemView.findViewById(R.id.prompt_edit_button);
            deletePromptButton = itemView.findViewById(R.id.prompt_delete_button);
        }
    }
}
