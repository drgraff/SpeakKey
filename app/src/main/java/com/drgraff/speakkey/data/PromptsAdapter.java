package com.drgraff.speakkey.data;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drgraff.speakkey.R; // Ensure R is imported from the correct package

import java.util.List;

public class PromptsAdapter extends RecyclerView.Adapter<PromptsAdapter.PromptViewHolder> {

    private List<Prompt> prompts;
    private final OnPromptInteractionListener listener;

    public interface OnPromptInteractionListener {
        void onPromptActivateToggle(Prompt prompt, boolean isActive);
        void onEditPrompt(Prompt prompt);
        void onDeletePrompt(Prompt prompt);
    }

    public PromptsAdapter(List<Prompt> prompts, OnPromptInteractionListener listener) {
        this.prompts = prompts;
        this.listener = listener;
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
        holder.promptTextView.setText(currentPrompt.getText());
        holder.promptActiveCheckbox.setChecked(currentPrompt.isActive());

        holder.promptActiveCheckbox.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPromptActivateToggle(currentPrompt, holder.promptActiveCheckbox.isChecked());
            }
        });

        holder.editPromptButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditPrompt(currentPrompt);
            }
        });

        holder.deletePromptButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeletePrompt(currentPrompt);
            }
        });
    }

    @Override
    public int getItemCount() {
        return prompts != null ? prompts.size() : 0;
    }

    public void updatePrompts(List<Prompt> newPrompts) {
        this.prompts = newPrompts;
        notifyDataSetChanged(); // Consider DiffUtil for more complex scenarios
    }

    static class PromptViewHolder extends RecyclerView.ViewHolder {
        TextView promptTextView;
        CheckBox promptActiveCheckbox;
        ImageButton editPromptButton;
        ImageButton deletePromptButton;

        PromptViewHolder(View itemView) {
            super(itemView);
            promptTextView = itemView.findViewById(R.id.prompt_text_view);
            promptActiveCheckbox = itemView.findViewById(R.id.prompt_active_checkbox);
            editPromptButton = itemView.findViewById(R.id.edit_prompt_button);
            deletePromptButton = itemView.findViewById(R.id.delete_prompt_button);
        }
    }
}
