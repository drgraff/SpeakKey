package com.drgraff.speakkey.formattingtags;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.drgraff.speakkey.R;
// Import EditFormattingTagActivity once it's created
// import com.drgraff.speakkey.formattingtags.EditFormattingTagActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class FormattingTagsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FormattingTagAdapter adapter;
    private FormattingTagManager tagManager;
    private TextView emptyView;
    private FloatingActionButton fabAddTag;

    public static final int REQUEST_CODE_ADD_TAG = 1;
    public static final int REQUEST_CODE_EDIT_TAG = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formatting_tags);

        Toolbar toolbar = findViewById(R.id.toolbar_formatting_tags);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.formatting_tags_activity_title));
        }

        recyclerView = findViewById(R.id.formatting_tags_recycler_view);
        emptyView = findViewById(R.id.empty_formatting_tags_text_view);
        fabAddTag = findViewById(R.id.fab_add_formatting_tag);

        tagManager = new FormattingTagManager(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Initialize with an empty list; loadFormattingTags will populate it.
        adapter = new FormattingTagAdapter(this, new ArrayList<>(), tagManager);
        recyclerView.setAdapter(adapter);

        fabAddTag.setOnClickListener(v -> {
            // Intent intent = new Intent(FormattingTagsActivity.this, EditFormattingTagActivity.class);
            // startActivityForResult(intent, REQUEST_CODE_ADD_TAG);
            // For now, as EditFormattingTagActivity doesn't exist:
            Intent intent = new Intent(FormattingTagsActivity.this, EditFormattingTagActivity.class); // Assuming EditFormattingTagActivity will be created
            startActivityForResult(intent, REQUEST_CODE_ADD_TAG);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFormattingTags();
    }

    private void loadFormattingTags() {
        List<FormattingTag> tags = new ArrayList<>(); // Default to empty list
        try {
            tagManager.open();
            // Assuming getAllTags will be implemented in FormattingTagManager
            // For now, it might return an empty list or throw if not implemented.
             if (tagManager.isOpen()) { // Ensure manager is open
                tags = tagManager.getAllTags(); // This method needs to be implemented in FormattingTagManager
             }
        } catch (Exception e) {
            // Log error or show a toast
            android.util.Log.e("FormattingTagsActivity", "Error loading tags", e);
        } finally {
            if (tagManager.isOpen()) {
                tagManager.close();
            }
        }

        adapter.setFormattingTags(tags);

        if (tags.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_ADD_TAG || requestCode == REQUEST_CODE_EDIT_TAG) {
                // loadFormattingTags(); // Implicitly called by onResume, but can be explicit if needed
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // onDestroy is not strictly required here as FormattingTagManager is opened/closed per operation
    // in both the adapter and loadFormattingTags. If tagManager were kept open for the activity's
    // lifecycle, then closing it in onDestroy would be crucial.
}
