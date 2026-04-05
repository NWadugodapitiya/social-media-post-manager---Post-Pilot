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
import android.widget.ImageView;
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
import androidx.recyclerview.widget.GridLayoutManager;
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

public class EditPostActivity extends AppCompatActivity {

    private EditText etTitle, etContent;
    private ImageView ivBack, ivDelete;
    private View btnAttachImage, btnPickDateTime;
    private TextView tvSelectedDateTime, tvMediaCount, tvSelectedDateTimeStatus;
    private RecyclerView rvSelectedImages;
    private SelectedImageAdapter imageAdapter;
    private List<Uri> selectedImages = new ArrayList<>();
    private Uri photoUri;
    private String postDate;
    private int postId;
    private DatabaseHelper dbHelper;
    
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
        setContentView(R.layout.activity_edit_post);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        ivBack = findViewById(R.id.iv_back);
        ivDelete = findViewById(R.id.iv_delete_post);
        btnAttachImage = findViewById(R.id.btn_attach_image);
        btnPickDateTime = findViewById(R.id.btn_pick_date_time);
        tvSelectedDateTime = findViewById(R.id.tv_selected_date_time);
        tvMediaCount = findViewById(R.id.tv_media_count);
        rvSelectedImages = findViewById(R.id.rv_selected_images);

        dbHelper = new DatabaseHelper(this);

        // Setup RecyclerView for images with 3 columns
        imageAdapter = new SelectedImageAdapter(selectedImages, position -> {
            selectedImages.remove(position);
            updateImageUI();
        });
        rvSelectedImages.setLayoutManager(new GridLayoutManager(this, 3));
        rvSelectedImages.setAdapter(imageAdapter);

        // Get data from intent
        postId = getIntent().getIntExtra("post_id", -1);
        String title = getIntent().getStringExtra("post_title");
        String content = getIntent().getStringExtra("post_desc");
        String images = getIntent().getStringExtra("post_images");
        postDate = getIntent().getStringExtra("post_date");
        if (postDate == null) postDate = "";
        
        if (title != null) etTitle.setText(title);
        if (content != null) etContent.setText(content);
        
        // Parse and load images
        if (images != null && !images.isEmpty()) {
            String[] imagePaths = images.split(",");
            for (String path : imagePaths) {
                if (!path.trim().isEmpty()) {
                    selectedImages.add(Uri.parse(path.trim()));
                }
            }
            updateImageUI();
        }

        ivBack.setOnClickListener(v -> finish());
        ivDelete.setOnClickListener(v -> {
            dbHelper.deletePost(postId);
            Toast.makeText(this, "Post Deleted", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnAttachImage.setOnClickListener(v -> {
            if (selectedImages.size() < MAX_IMAGES) {
                showImageSourceDialog();
            } else {
                Toast.makeText(this, "Maximum 4 images allowed", Toast.LENGTH_SHORT).show();
            }
        });

        btnPickDateTime.setOnClickListener(v -> showDateTimePicker());
        findViewById(R.id.btn_publish_now).setOnClickListener(v -> savePost("Published"));
        findViewById(R.id.btn_publish_later).setOnClickListener(v -> savePost("Auto-Publish"));
        findViewById(R.id.btn_schedule).setOnClickListener(v -> showDateTimePicker());
        
        tvSelectedDateTimeStatus = findViewById(R.id.tv_selected_date_time_status);

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

    private void savePost(String status) {
        String title = etTitle.getText().toString();
        String content = etContent.getText().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert selected images back to a comma-separated string
        StringBuilder imagesBuilder = new StringBuilder();
        for (int i = 0; i < selectedImages.size(); i++) {
            imagesBuilder.append(selectedImages.get(i).toString());
            if (i < selectedImages.size() - 1) {
                imagesBuilder.append(",");
            }
        }

        Post post = new Post(postDate, title, content, "PostPilot", status, imagesBuilder.toString());
        post.setId(postId);
        dbHelper.updatePost(post);

        Toast.makeText(this, "Post Updated as " + status, Toast.LENGTH_SHORT).show();
        
        // Auto-publish to Facebook if status is Published
        if (status.equals("Published")) {
            new FacebookHelper(this).publishPost(content, imagesBuilder.toString());
        }
        
        finish();
    }

    private void showImageSourceDialog() {
        String[] options = {getString(R.string.camera), getString(R.string.gallery)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.select_image_source)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermission();
                    } else {
                        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        galleryLauncher.launch(pickPhotoIntent);
                    }
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

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalCacheDir();
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
        tvMediaCount.setText(selectedImages.size() + " / " + MAX_IMAGES + " images");
        btnAttachImage.setVisibility(selectedImages.size() >= MAX_IMAGES ? View.GONE : View.VISIBLE);
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                String dateTime = dayOfMonth + "/" + (month + 1) + "/" + year + " " + hourOfDay + ":" + minute;
                tvSelectedDateTime.setText(dateTime);
                if (tvSelectedDateTimeStatus != null) {
                    tvSelectedDateTimeStatus.setText("Scheduled for: " + dateTime);
                    tvSelectedDateTimeStatus.setVisibility(View.VISIBLE);
                }
                savePost("Scheduled");
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
}
