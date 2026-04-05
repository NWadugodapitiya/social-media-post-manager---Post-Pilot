package com.postpilot.app;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CreatePostActivity extends AppCompatActivity {

    private EditText etTitle, etContent;
    private DatabaseHelper dbHelper;
    private View btnAttachImage;
    private TextView tvMediaCount, tvSelectedDateTime;
    private RecyclerView rvSelectedImages;
    private SelectedImageAdapter imageAdapter;
    private List<Uri> selectedImages = new ArrayList<>();
    private Uri photoUri;
    private String selectedDateTime = "";
    
    private static final int MAX_IMAGES = 4;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_post);

        dbHelper = new DatabaseHelper(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etTitle = findViewById(R.id.et_post_title);
        etContent = findViewById(R.id.et_post_content);
        btnAttachImage = findViewById(R.id.btn_attach_image);
        tvMediaCount = findViewById(R.id.tv_media_count);
        tvSelectedDateTime = findViewById(R.id.tv_selected_date_time);
        rvSelectedImages = findViewById(R.id.rv_selected_images);

        // Setup Image Adapter
        imageAdapter = new SelectedImageAdapter(selectedImages, position -> {
            selectedImages.remove(position);
            updateImageUI();
        });
        rvSelectedImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSelectedImages.setAdapter(imageAdapter);

        // Cancel Button
        findViewById(R.id.tv_cancel).setOnClickListener(v -> finish());
        
        // Save Draft Button
        findViewById(R.id.btn_save_draft).setOnClickListener(v -> savePost("Draft"));

        // Add Media click
        btnAttachImage.setOnClickListener(v -> {
            if (selectedImages.size() < MAX_IMAGES) {
                showImageSourceDialog();
            } else {
                Toast.makeText(this, "Maximum 4 images allowed", Toast.LENGTH_SHORT).show();
            }
        });

        // Publish buttons
        findViewById(R.id.btn_publish_now).setOnClickListener(v -> savePost("Published"));
        findViewById(R.id.btn_publish_later).setOnClickListener(v -> savePost("Auto-Publish"));
        findViewById(R.id.btn_schedule).setOnClickListener(v -> showDateTimePicker());
        
        if (savedInstanceState != null) {
            photoUri = savedInstanceState.getParcelable("photo_uri");
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (photoUri != null) {
            outState.putParcelable("photo_uri", photoUri);
        }
    }

    private void showImageSourceDialog() {
        String[] options = {"Camera", "Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) checkCameraPermission();
                    else launchGallery();
                }).show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        
        // Safety check to see if there's a camera app to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating photo file", Toast.LENGTH_SHORT).show();
            }
            
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this, 
                        getApplicationContext().getPackageName() + ".fileprovider", 
                        photoFile);
                
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                cameraLauncher.launch(takePictureIntent);
            }
        } else {
            Toast.makeText(this, "No Camera App found on your device", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGallery() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(pickPhotoIntent);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalCacheDir(); // Better for temporary camera images
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Uri sourceUri = photoUri;
                    if (sourceUri != null) {
                        try {
                            Uri internalUri = copyImageToInternalStorage(sourceUri);
                            selectedImages.add(internalUri);
                            updateImageUI();
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to store captured image", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(this, "Camera cancelled", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    if (selectedImage != null) {
                        try {
                            // Copy gallery image to app's internal storage
                            Uri internalUri = copyImageToInternalStorage(selectedImage);
                            selectedImages.add(internalUri);
                            updateImageUI();
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to copy image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    private Uri copyImageToInternalStorage(Uri uri) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "IMG_" + timeStamp + ".jpg";
        File file = new File(getFilesDir(), fileName);

        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(file)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
        return Uri.fromFile(file);
    }

    private void updateImageUI() {
        imageAdapter.notifyDataSetChanged();
        tvMediaCount.setText(selectedImages.size() + "/4 Photos");
        btnAttachImage.setVisibility(selectedImages.size() >= MAX_IMAGES ? View.GONE : View.VISIBLE);
    }

    private void showDateTimePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String date = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                    showTimePicker(date);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void showTimePicker(String date) {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    selectedDateTime = date + " " + String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
                    tvSelectedDateTime.setText("Scheduled for: " + selectedDateTime);
                    tvSelectedDateTime.setVisibility(View.VISIBLE);
                }, hour, minute, true);
        timePickerDialog.show();
    }

    private void savePost(String status) {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Title is required");
            return;
        }

        String postDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        
        // Convert selected images to a comma-separated string
        StringBuilder imagesBuilder = new StringBuilder();
        for (int i = 0; i < selectedImages.size(); i++) {
            imagesBuilder.append(selectedImages.get(i).toString());
            if (i < selectedImages.size() - 1) {
                imagesBuilder.append(",");
            }
        }

        Post post = new Post(postDate, title, content, "PostPilot", status, imagesBuilder.toString());
        long id = dbHelper.addPost(post);

        if (id > 0) {
            Toast.makeText(this, "Post saved as " + status, Toast.LENGTH_SHORT).show();
            
            // Auto-publish to Facebook if status is Published
            if (status.equals("Published")) {
                new FacebookHelper(this).publishPost(content, imagesBuilder.toString());
            }
            
            finish();
        } else {

            Toast.makeText(this, "Error saving post", Toast.LENGTH_SHORT).show();
        }
    }
}
