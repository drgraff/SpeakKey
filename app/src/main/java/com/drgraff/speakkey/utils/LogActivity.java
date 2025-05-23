package com.drgraff.speakkey.utils; // Or com.drgraff.speakkey.ui if preferred

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drgraff.speakkey.R; // Ensure R is imported correctly

import java.util.ArrayList;

public class LogActivity extends AppCompatActivity {

    private RecyclerView logRecyclerView;
    private LogAdapter logAdapter;
    private Button clearLogButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        // Enable the Up button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Application Log"); // Set a title for the activity
        }

        logRecyclerView = findViewById(R.id.log_recycler_view);
        clearLogButton = findViewById(R.id.clear_log_button);

        // Setup RecyclerView
        logRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Initialize with an empty list, will be populated in onResume
        logAdapter = new LogAdapter(new ArrayList<>()); 
        logRecyclerView.setAdapter(logAdapter);

        // Set up Clear Log button listener
        clearLogButton.setOnClickListener(v -> {
            AppLogManager.getInstance().clearEntries();
            refreshLogView();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLogView();
    }

    private void refreshLogView() {
        if (logAdapter != null) {
            logAdapter.updateLogEntries(AppLogManager.getInstance().getEntries());
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.getItemId() == android.R.id.home) {
            finish(); // Go back to the previous activity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
