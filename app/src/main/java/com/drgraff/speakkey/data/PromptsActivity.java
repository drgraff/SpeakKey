package com.drgraff.speakkey.data;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drgraff.speakkey.R; // Ensure R is imported correctly

import java.util.ArrayList;
import java.util.List;

public class PromptsActivity extends AppCompatActivity implements PromptsAdapter.OnPromptInteractionListener {

    private RecyclerView promptsRecyclerView;
    private PromptsAdapter promptsAdapter;
    private PromptManager promptManager;
    private List<Prompt> promptList;

    private EditText promptInputText;
    private Button addPromptButton;

    private Prompt currentEditingPrompt = null; // To keep track if we are editing an existing prompt

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prompts);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Manage Prompts");
        }

        promptManager = new PromptManager(this);
        promptList = new ArrayList<>(); // Initialize empty, will be loaded in onResume

        promptInputText = findViewById(R.id.prompt_input_text);
        addPromptButton = findViewById(R.id.add_prompt_button);
        promptsRecyclerView = findViewById(R.id.prompts_recycler_view);

        promptsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        promptsAdapter = new PromptsAdapter(promptList, this);
        promptsRecyclerView.setAdapter(promptsAdapter);

        addPromptButton.setOnClickListener(v -> saveOrUpdatePrompt());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPrompts();
    }

    private void loadPrompts() {
        promptList.clear();
        promptList.addAll(promptManager.getPrompts());
        promptsAdapter.updatePrompts(promptList); // Use a method that updates the adapter's list reference
        
        // Reset editing state
        currentEditingPrompt = null;
        promptInputText.setText("");
        addPromptButton.setText("Add");
    }

    private void saveOrUpdatePrompt() {
        String text = promptInputText.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Prompt text cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentEditingPrompt != null) {
            // Update existing prompt
            currentEditingPrompt.setText(text);
            // isActive state is managed by the checkbox in the adapter directly
            promptManager.updatePrompt(currentEditingPrompt);
            Toast.makeText(this, "Prompt updated", Toast.LENGTH_SHORT).show();
        } else {
            // Add new prompt
            promptManager.addPrompt(text); // New prompts are inactive by default as per PromptManager
            Toast.makeText(this, "Prompt added", Toast.LENGTH_SHORT).show();
        }
        loadPrompts(); // Reload and refresh list, also resets input field and button
    }

    @Override
    public void onPromptActivateToggle(Prompt prompt, boolean isActive) {
        // The checkbox in the adapter directly changes the state for immediate visual feedback.
        // We need to persist this change.
        prompt.setActive(isActive); // Ensure the prompt object reflects the change
        promptManager.updatePrompt(prompt); // Save the updated prompt
        // No need to call loadPrompts() here if only active state changed, 
        // unless other visual cues depend on it. The checkbox is already updated.
        // For simplicity, a full reload ensures consistency if other derived states existed.
        // However, to avoid list flicker, a more targeted update would be better if performance issues arise.
        // For now, let's keep it simple:
        // int index = promptList.indexOf(prompt); // This relies on Prompt.equals() based on ID
        // if (index != -1) {
        //     promptsAdapter.notifyItemChanged(index);
        // }
        // The PromptManager.togglePromptActiveStatus(prompt.getId()) could also be used here
        // if the adapter didn't handle the state change directly for the checkbox.
        // Since the adapter's checkbox listener is the source of truth for the UI event,
        // updating the object and saving is the main task.
    }

    @Override
    public void onEditPrompt(Prompt prompt) {
        currentEditingPrompt = prompt;
        promptInputText.setText(prompt.getText());
        promptInputText.requestFocus();
        addPromptButton.setText("Save");
    }

    @Override
    public void onDeletePrompt(Prompt prompt) {
        promptManager.deletePrompt(prompt.getId());
        Toast.makeText(this, "Prompt deleted", Toast.LENGTH_SHORT).show();
        loadPrompts(); // Reload and refresh list
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Go back to the previous activity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
