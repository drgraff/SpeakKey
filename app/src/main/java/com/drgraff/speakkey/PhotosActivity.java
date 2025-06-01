package com.drgraff.speakkey;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhotosActivity extends AppCompatActivity {

    private static final String TAG = "PhotosActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private static final String KEY_PHOTO_PATH = "currentPhotoPath";

    private ImageView imageViewPhoto;
    private Button btnTakePhotoArea; // This is the initial "Take Photo" button
    private Button btnClearPhoto;

    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        Toolbar toolbar = findViewById(R.id.toolbar_photos);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Photos");
        }

        imageViewPhoto = findViewById(R.id.image_view_photo);
        btnTakePhotoArea = findViewById(R.id.btn_take_photo_area); // Initial Take Photo Button
        btnClearPhoto = findViewById(R.id.btn_clear_photo);

        btnTakePhotoArea.setOnClickListener(v -> checkCameraPermissionAndDispatch());
        imageViewPhoto.setOnClickListener(v -> {
            if (currentPhotoPath != null) {
                File oldFile = new File(currentPhotoPath);
                if (oldFile.exists()) {
                    if (oldFile.delete()) {
                        Log.d(TAG, "Old photo deleted: " + currentPhotoPath);
                    } else {
                        Log.e(TAG, "Failed to delete old photo: " + currentPhotoPath);
                    }
                }
                currentPhotoPath = null;
            }
            checkCameraPermissionAndDispatch();
        });

        btnClearPhoto.setOnClickListener(v -> {
            clearPhoto();
        });

        if (savedInstanceState != null) {
            currentPhotoPath = savedInstanceState.getString(KEY_PHOTO_PATH);
            if (currentPhotoPath != null) {
                setPic();
            }
        }
    }

    private void checkCameraPermissionAndDispatch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            dispatchTakePictureIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission is required to take photos.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error occurred while creating the File", ex);
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Toast.makeText(this, "No camera app found.", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                setPic();
            } else {
                // If the user cancelled or an error occurred, delete the empty file.
                if (currentPhotoPath != null) {
                    File photoFile = new File(currentPhotoPath);
                    if (photoFile.exists() && photoFile.length() == 0) {
                        photoFile.delete();
                    }
                }
                // Optionally, if currentPhotoPath was from a previous successful capture,
                // you might want to keep it. For this flow, we assume a new capture attempt.
                // If no new image, and an old one was there, clearPhoto() might be too aggressive.
                // For now, just deleting the empty file is fine.
            }
        }
    }

    private void setPic() {
        if (currentPhotoPath == null || currentPhotoPath.isEmpty()) {
            Log.w(TAG, "currentPhotoPath is null or empty in setPic");
            clearPhoto(); // Ensure UI is in a consistent state if path is invalid
            return;
        }

        File imgFile = new File(currentPhotoPath);
        if (imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            if (myBitmap != null) {
                imageViewPhoto.setImageBitmap(myBitmap);
                imageViewPhoto.setVisibility(View.VISIBLE);
                btnTakePhotoArea.setVisibility(View.GONE); // Hide initial button
                btnClearPhoto.setVisibility(View.VISIBLE);
            } else {
                Log.e(TAG, "Failed to decode bitmap from path: " + currentPhotoPath);
                Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show();
                clearPhoto(); // Reset if image can't be loaded
            }
        } else {
            Log.w(TAG, "Image file does not exist at path: " + currentPhotoPath);
            clearPhoto(); // Reset if file doesn't exist
        }
    }

    private void clearPhoto() {
        if (currentPhotoPath != null) {
            File photoFile = new File(currentPhotoPath);
            if (photoFile.exists()) {
                photoFile.delete();
            }
            currentPhotoPath = null;
        }
        imageViewPhoto.setImageBitmap(null); // Clear the image
        imageViewPhoto.setVisibility(View.GONE);
        btnTakePhotoArea.setVisibility(View.VISIBLE); // Show initial button
        btnClearPhoto.setVisibility(View.GONE);
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentPhotoPath != null) {
            outState.putString(KEY_PHOTO_PATH, currentPhotoPath);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Navigate back to the previous activity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
