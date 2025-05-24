package com.drgraff.speakkey;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class FullScreenEditTextDialogFragment extends DialogFragment {

    private static final String ARG_TEXT_TO_EDIT = "textToEdit";
    private EditText editTextFullScreenContent;
    private Button btnSaveFullScreenEdit;
    private OnSaveListener onSaveListener;

    // 1. Interface OnSaveListener
    public interface OnSaveListener {
        void onSave(String editedText);
    }

    // 2. newInstance static factory method
    public static FullScreenEditTextDialogFragment newInstance(String textToEdit) {
        FullScreenEditTextDialogFragment fragment = new FullScreenEditTextDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TEXT_TO_EDIT, textToEdit);
        fragment.setArguments(args);
        return fragment;
    }

    // 3. onAttach to ensure Activity implements listener
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            onSaveListener = (OnSaveListener) getTargetFragment(); // If shown by a Fragment
            if (onSaveListener == null && context instanceof OnSaveListener) {
                onSaveListener = (OnSaveListener) context; // If shown by an Activity
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnSaveListener or target Fragment must implement OnSaveListener");
        }
    }

    // 4. onCreateView to inflate layout
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_edit_text_fullscreen, container, false);

        // 5. Find views
        editTextFullScreenContent = view.findViewById(R.id.edit_text_fullscreen_content);
        btnSaveFullScreenEdit = view.findViewById(R.id.btn_save_fullscreen_edit);

        // 6. Retrieve textToEdit and set to EditText
        if (getArguments() != null) {
            String textToEdit = getArguments().getString(ARG_TEXT_TO_EDIT);
            if (textToEdit != null) {
                editTextFullScreenContent.setText(textToEdit);
                // Move cursor to the end
                editTextFullScreenContent.setSelection(textToEdit.length());
            }
        }

        // 7. Set click listener for Save button
        btnSaveFullScreenEdit.setOnClickListener(v -> {
            if (onSaveListener != null) {
                String editedText = editTextFullScreenContent.getText().toString();
                onSaveListener.onSave(editedText);
            }
            dismiss();
        });

        return view;
    }

    // 8. onStart to make dialog full-screen
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
            // Optional: remove the title bar that might still be there
            // dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onSaveListener = null; // Avoid memory leaks
    }
}
