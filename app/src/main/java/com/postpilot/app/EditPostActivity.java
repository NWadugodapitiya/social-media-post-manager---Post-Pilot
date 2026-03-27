package com.postpilot.app;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EditPostActivity extends AppCompatActivity {

    private EditText etTitle, etContent;
    private ImageView ivBack, ivDelete;
    private View btnAttachImage, btnPickDateTime;
    private TextView tvSelectedDateTime, tvMediaCount;
    private RecyclerView rvSelectedImages;
    private SelectedImageAdapter imageAdapter;
    private List<Uri> selectedImages = new ArrayList<>();
    
    private static final int MAX_IMAGES = 4;

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

        // Setup RecyclerView for images
        imageAdapter = new SelectedImageAdapter(selectedImages, position -> {
            selectedImages.remove(position);
            updateImageUI();
        });
        rvSelectedImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSelectedImages.setAdapter(imageAdapter);

        // Get data from intent
        String title = getIntent().getStringExtra("post_title");
        String content = getIntent().getStringExtra("post_desc");
        if (title != null) etTitle.setText(title);
        if (content != null) etContent.setText(content);

        ivBack.setOnClickListener(v -> finish());
        ivDelete.setOnClickListener(v -> {
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
        findViewById(R.id.btn_save_draft).setOnClickListener(v -> finish());
        findViewById(R.id.btn_queue).setOnClickListener(v -> finish());
    }

    private void showImageSourceDialog() {
        String[] options = {getString(R.string.camera), getString(R.string.gallery)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.select_image_source)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        cameraLauncher.launch(takePictureIntent);
                    } else {
                        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        galleryLauncher.launch(pickPhotoIntent);
                    }
                }).show();
    }

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // Note: For real apps, you should save the bitmap to a file and get Uri
                    // Here we handle it simply for the UI demo
                    Toast.makeText(this, "Image captured (Uri logic needed for persistent list)", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    if (selectedImage != null) {
                        selectedImages.add(selectedImage);
                        updateImageUI();
                    }
                }
            });

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
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
}