package com.drgraff.speakkey.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast; // Temporary for testing clicks

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.drgraff.speakkey.R; // For R.layout.pref_color_picker
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

public class ColorPickerPreference extends Preference {

    private View colorPreviewSwatch;
    private int defaultColor = Color.BLACK;
    private int selectedColor;

    public ColorPickerPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public ColorPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorPickerPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        // Set the layout resource for this Preference
        setLayoutResource(R.layout.pref_color_picker);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        colorPreviewSwatch = holder.findViewById(R.id.color_preview_swatch);
        loadColor();
        updateColorPreview();
    }

    private void loadColor() {
        if (getSharedPreferences() != null) {
            selectedColor = getSharedPreferences().getInt(getKey(), defaultColor);
        } else {
            selectedColor = defaultColor;
        }
    }

    private void saveColor(int color) {
        selectedColor = color;
        if (getSharedPreferences() != null) {
            SharedPreferences.Editor editor = getSharedPreferences().edit();
            editor.putInt(getKey(), color);
            editor.apply();
            notifyChanged(); // To update the preview
        }
    }

    private void updateColorPreview() {
        if (colorPreviewSwatch != null) {
            GradientDrawable background = (GradientDrawable) colorPreviewSwatch.getBackground();
            if (background == null || !(background instanceof GradientDrawable)) {
                // If the background is not a GradientDrawable or is null, create a new one.
                // This might happen if the initial background was just a color or a different drawable.
                // For simplicity, we'll assume it's a shape drawable that can be mutated or replaced.
                // A more robust way would be to ensure the background is always a GradientDrawable.
                // For now, let's try setting a simple color filter or creating a new drawable.
                // Creating a new drawable is safer.
                GradientDrawable newSwatch = new GradientDrawable();
                newSwatch.setShape(GradientDrawable.OVAL); // Match the border shape
                newSwatch.setColor(selectedColor);
                newSwatch.setStroke(2, Color.GRAY); // Re-apply a border programmatically if needed
                colorPreviewSwatch.setBackground(newSwatch);
            } else {
                 // If it is a GradientDrawable, we can try to set its color.
                try {
                    background.setColor(selectedColor);
                } catch (Exception e) {
                     // Fallback if setColor is not available or fails
                    GradientDrawable newSwatch = new GradientDrawable();
                    newSwatch.setShape(GradientDrawable.OVAL);
                    newSwatch.setColor(selectedColor);
                    newSwatch.setStroke(2, Color.GRAY);
                    colorPreviewSwatch.setBackground(newSwatch);
                }
            }
        }
    }

    @Override
    protected void onClick() {
        super.onClick();
        ColorPickerDialogBuilder
                .with(getContext())
                .setTitle("Choose color")
                .initialColor(selectedColor)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setOnColorSelectedListener(new OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int newSelectedColor) {
                        // Optionally show live preview or toast
                        // Toast.makeText(getContext(), "onColorSelected: 0x" + Integer.toHexString(newSelectedColor), Toast.LENGTH_SHORT).show();
                    }
                })
                .setPositiveButton("OK", new ColorPickerClickListener() {
                    @Override
                    public void onClick(com.flask.colorpicker.DialogInterface d, int lastSelectedColor, Integer[] allColors) {
                        saveColor(lastSelectedColor);
                        updateColorPreview(); // Make sure preview updates
                    }
                })
                .setNegativeButton("Cancel", new com.flask.colorpicker.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(com.flask.colorpicker.DialogInterface d, int i) {
                        d.dismiss();
                    }
                })
                .build()
                .show();
    }

    // It's good practice to handle default values if defined in XML, though we are doing it programmatically here.
    // For this preference, the default color is set via the loadColor method if not found in SharedPreferences.
    // If you wanted to support android:defaultValue in XML for a color string, you'd parse it in onGetDefaultValue.
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        // Attempt to get the default value as an integer (color)
        // This allows using android:defaultValue="#RRGGBB" or similar if needed,
        // but requires parsing. For simplicity, we are setting defaultColor in code.
        // For now, we return the super's behavior.
        return super.onGetDefaultValue(a, index);
    }

    // Call this from SettingsFragment to set initial default color if not in SharedPreferences
    public void setDefaultColorValue(int color) {
        this.defaultColor = color;
        // If the preference hasn't been set yet, this default will be used by loadColor()
        // and then persisted if the user opens and saves the dialog.
        // To ensure it's immediately available if no persisted value exists:
        if (getSharedPreferences() != null && !getSharedPreferences().contains(getKey())) {
             selectedColor = defaultColor;
             updateColorPreview(); // Update preview if it's showing the old default
        }
    }
}
