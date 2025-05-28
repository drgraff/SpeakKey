package com.drgraff.speakkey.formattingtags;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
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
// Import EditFormattingTagActivity once it's created
// import com.drgraff.speakkey.formattingtags.EditFormattingTagActivity;

import java.util.List;

public class FormattingTagAdapter extends RecyclerView.Adapter<FormattingTagAdapter.FormattingTagViewHolder> {

    private List<FormattingTag> formattingTags;
    private final Context context;
    private final FormattingTagManager tagManager;
    private static final String TAG = "FormattingTagAdapter";

    public FormattingTagAdapter(Context context, List<FormattingTag> formattingTags, FormattingTagManager tagManager) {
        this.context = context;
        this.formattingTags = formattingTags;
        this.tagManager = tagManager;
    }

    @NonNull
    @Override
    public FormattingTagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_formatting_tag, parent, false);
        return new FormattingTagViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull FormattingTagViewHolder holder, int position) {
        FormattingTag currentTag = formattingTags.get(position);

        holder.tagNameTextView.setText(currentTag.getName());
        holder.tagOpeningTextView.setText(currentTag.getOpeningTagText());
        holder.tagKeystrokesTextView.setText(currentTag.getKeystrokeSequence());
        holder.tagActiveSwitch.setChecked(currentTag.isActive());

        // Remove previous listeners to prevent multiple triggers
        holder.tagActiveSwitch.setOnCheckedChangeListener(null);
        holder.editButton.setOnClickListener(null);
        holder.deleteButton.setOnClickListener(null);


        holder.tagActiveSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            currentTag.setActive(isChecked);
            try {
                tagManager.open();
                tagManager.updateTag(currentTag); // Assuming updateTag method exists in FormattingTagManager
            } catch (Exception e) {
                Log.e(TAG, "Error updating tag active state", e);
                Toast.makeText(context, "Error updating tag", Toast.LENGTH_SHORT).show();
                // Revert switch state if update failed
                buttonView.setChecked(!isChecked);
                currentTag.setActive(!isChecked); // Revert in the model as well
            } finally {
                tagManager.close();
            }
        });

        holder.editButton.setOnClickListener(v -> {
            // Intent intent = new Intent(context, EditFormattingTagActivity.class);
            // intent.putExtra(EditFormattingTagActivity.EXTRA_TAG_ID, currentTag.getId());
            // context.startActivity(intent);
            // For now, as EditFormattingTagActivity doesn't exist, we can log or Toast
            Log.d(TAG, "Edit button clicked for tag ID: " + currentTag.getId());
            Toast.makeText(context, "Edit for tag: " + currentTag.getName(), Toast.LENGTH_SHORT).show();
            // TODO: Replace with actual Intent when EditFormattingTagActivity is available
             if (context instanceof FormattingTagsActivity) {
                Intent intent = new Intent(context, EditFormattingTagActivity.class); // Assuming EditFormattingTagActivity will be created
                intent.putExtra("EXTRA_TAG_ID", currentTag.getId());
                ((FormattingTagsActivity) context).startActivityForResult(intent, FormattingTagsActivity.REQUEST_CODE_EDIT_TAG);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                FormattingTag tagToDelete = formattingTags.get(currentPosition);
                try {
                    tagManager.open();
                    tagManager.deleteTag(tagToDelete.getId()); // Assuming deleteTag method exists
                    formattingTags.remove(currentPosition);
                    notifyItemRemoved(currentPosition);
                    // Corrected the range for notifyItemRangeChanged
                    notifyItemRangeChanged(currentPosition, formattingTags.size() - currentPosition);
                    Toast.makeText(context, context.getString(R.string.formatting_tag_deleted_message), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error deleting tag", e);
                    Toast.makeText(context, "Error deleting tag", Toast.LENGTH_SHORT).show();
                } finally {
                    tagManager.close();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return formattingTags == null ? 0 : formattingTags.size();
    }

    public void setFormattingTags(List<FormattingTag> newTags) {
        if (newTags == null) {
            this.formattingTags.clear();
        } else {
            this.formattingTags = newTags; // Directly assign if the list is managed externally or a new list is preferred
            // Or, if you want to add to the existing list instance:
            // this.formattingTags.clear();
            // this.formattingTags.addAll(newTags);
        }
        notifyDataSetChanged();
    }


    static class FormattingTagViewHolder extends RecyclerView.ViewHolder {
        TextView tagNameTextView, tagOpeningTextView, tagKeystrokesTextView;
        SwitchCompat tagActiveSwitch;
        ImageButton editButton, deleteButton;

        public FormattingTagViewHolder(@NonNull View itemView) {
            super(itemView);
            tagNameTextView = itemView.findViewById(R.id.tag_name_text_view);
            tagOpeningTextView = itemView.findViewById(R.id.tag_opening_text_view);
            tagKeystrokesTextView = itemView.findViewById(R.id.tag_keystrokes_text_view);
            tagActiveSwitch = itemView.findViewById(R.id.tag_active_switch);
            editButton = itemView.findViewById(R.id.tag_edit_button);
            deleteButton = itemView.findViewById(R.id.tag_delete_button);
        }
    }
}
