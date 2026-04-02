package com.postpilot.app;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PostPreviewActivity extends AppCompatActivity {

    private int postId;
    private String postTitle, postDesc, postImages;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_post_preview);

        dbHelper = new DatabaseHelper(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get data from intent
        postId = getIntent().getIntExtra("post_id", -1);
        postTitle = getIntent().getStringExtra("post_title");
        postDesc = getIntent().getStringExtra("post_desc");
        postImages = getIntent().getStringExtra("post_images");

        // UI components
        TextView tvTitle = findViewById(R.id.tv_preview_title);
        TextView tvContent = findViewById(R.id.tv_preview_content);
        ImageView ivBack = findViewById(R.id.iv_back);
        ImageView ivPreview = findViewById(R.id.iv_preview_main);

        if (postImages != null && !postImages.isEmpty()) {
            String[] uris = postImages.split(",");
            if (uris.length > 0) {
                try {
                    Uri imageUri = Uri.parse(uris[0]);
                    if (imageUri.getScheme().equals("file")) {
                        ivPreview.setImageBitmap(BitmapFactory.decodeFile(imageUri.getPath()));
                    } else {
                        ivPreview.setImageURI(imageUri);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (postTitle != null) tvTitle.setText(postTitle);
        if (postDesc != null) tvContent.setText(postDesc);

        ivBack.setOnClickListener(v -> finish());

        // Action buttons
        findViewById(R.id.btn_edit).setOnClickListener(v -> {
            Intent intent = new Intent(this, EditPostActivity.class);
            intent.putExtra("post_id", postId);
            intent.putExtra("post_title", postTitle);
            intent.putExtra("post_desc", postDesc);
            intent.putExtra("post_images", postImages);
            intent.putExtra("post_date", getIntent().getStringExtra("post_date"));
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btn_delete).setOnClickListener(v -> {
            dbHelper.deletePost(postId);
            Toast.makeText(this, "Post Deleted", Toast.LENGTH_SHORT).show();
            finish();
        });

        findViewById(R.id.btn_share).setOnClickListener(v -> {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, postTitle + "\n\n" + postDesc);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "Share post via"));
        });

        findViewById(R.id.btn_publish).setOnClickListener(v -> {
            Toast.makeText(this, "Publishing...", Toast.LENGTH_SHORT).show();
        });
    }
}