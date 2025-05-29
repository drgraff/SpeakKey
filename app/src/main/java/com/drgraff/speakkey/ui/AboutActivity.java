package com.drgraff.speakkey.ui;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.drgraff.speakkey.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = findViewById(R.id.toolbar_about);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.about_activity_title);
        }

        TextView inputStickLinkTextView = findViewById(R.id.about_inputstick_link);
        // Ensure the text actually contains a link for this to be effective
        // The string resource about_inputstick_link_text should be HTML formatted if specific parts are to be links
        // For now, setting MovementMethod enables general link clicking if the text were auto-linked by Android.
        inputStickLinkTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Or NavUtils.navigateUpFromSameTask(this); if specific up navigation is needed
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
