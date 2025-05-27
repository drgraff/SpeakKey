package com.drgraff.speakkey.data;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
// Removed Button, EditText, Toast (Toast might be re-added if needed for other purposes)

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Added for Toolbar
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drgraff.speakkey.R;
import com.drgraff.speakkey.ui.prompts.PromptEditorActivity; // Correct path for PromptEditorActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Added for FAB

import java.util.ArrayList;
import java.util.List;

public class PromptsActivity extends AppCompatActivity { // Removed PromptsAdapter.OnPromptInteractionListener

    private RecyclerView promptsRecyclerView;
    private PromptsAdapter promptsAdapter;
    private PromptManager promptManager;
    // Removed promptList as adapter will hold its own list
    private TextView emptyPromptsTextView; // Added for empty state
    private FloatingActionButton fabAddPrompt; // Added for FAB

    // Removed promptInputText, addPromptButton, currentEditingPrompt

    private static final int ADD_PROMPT_REQUEST = 1;
    // private static final int EDIT_PROMPT_REQUEST = 2; // For future use if adapter starts activity for result

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prompts); // New layout

        Toolbar toolbar = findViewById(R.id.toolbar_prompts); // New Toolbar ID
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Prompts"); // Updated title
        }

        promptManager = new PromptManager(this);

        promptsRecyclerView = findViewById(R.id.prompts_recycler_view);
        emptyPromptsTextView = findViewById(R.id.empty_prompts_text_view); // Initialize empty view
        fabAddPrompt = findViewById(R.id.fab_add_prompt); // Initialize FAB

        promptsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // PromptsAdapter constructor was changed to PromptsAdapter(Context context, List<Prompt> prompts, PromptManager promptManager)
        promptsAdapter = new PromptsAdapter(this, new ArrayList<>(), promptManager);
        promptsRecyclerView.setAdapter(promptsAdapter);

        fabAddPrompt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PromptsActivity.this, PromptEditorActivity.class);
                // For adding a new prompt, we don't pass an ID
                startActivityForResult(intent, ADD_PROMPT_REQUEST);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPrompts();
    }

    private void loadPrompts() {
        List<Prompt> loadedPrompts = promptManager.getPrompts();
        promptsAdapter.setPrompts(loadedPrompts); // Use the new setPrompts method in adapter

        if (loadedPrompts == null || loadedPrompts.isEmpty()) {
            promptsRecyclerView.setVisibility(View.GONE);
            emptyPromptsTextView.setVisibility(View.VISIBLE);
        } else {
            promptsRecyclerView.setVisibility(View.VISIBLE);
            emptyPromptsTextView.setVisibility(View.GONE);
        }
    }

    // Removed saveOrUpdatePrompt method

    // Removed onPromptActivateToggle, onEditPrompt, onDeletePrompt listener methods

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_PROMPT_REQUEST && resultCode == Activity.RESULT_OK) {
            // Prompts were added/edited, onResume will call loadPrompts()
            // loadPrompts(); // Explicitly calling here is also fine, but onResume handles it
        }
        // Handle EDIT_PROMPT_REQUEST if implemented
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
