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
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.util.HashMap;
import java.util.Map;


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
            SharedPreferences sharedPref = getSharedPreferences("PostPilotPrefs", MODE_PRIVATE);
            String pageId = sharedPref.getString("fb_page_id", null);
            String pageToken = sharedPref.getString("fb_page_token", null);

            if (pageId == null || pageToken == null) {
                Toast.makeText(this, "Connect to Facebook first in the Welcome screen!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Publishing to Facebook...", Toast.LENGTH_SHORT).show();
                
                if (postImages != null && !postImages.isEmpty()) {
                    // Post image if available (using sample URL logic for now)
                    postImage(pageId, pageToken);
                } else {
                    // Just post text
                    postToPage(pageId, pageToken);
                }
            }
        });
    }

    private void postToPage(String pageId, String pageToken) {
        String url = "https://graph.facebook.com/v20.0/" + pageId + "/feed";

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d("POST_SUCCESS", "Response: " + response);
                    Toast.makeText(this, "Post successfully published to Facebook Page!", Toast.LENGTH_LONG).show();
                },
                error -> {
                    String errorMsg = "Error";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            errorMsg = new String(error.networkResponse.data, "UTF-8");
                        } catch (Exception e) {
                            errorMsg = error.toString();
                        }
                    } else {
                        errorMsg = error.toString();
                    }
                    Log.e("POST_ERROR", errorMsg);
                    Toast.makeText(this, "FB Error: " + errorMsg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("message", postDesc);
                params.put("access_token", pageToken);
                return params;
            }
        };

        queue.add(request);
    }

    private void postImage(String pageId, String pageToken) {
        String url = "https://graph.facebook.com/v20.0/" + pageId + "/photos";

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d("IMG_SUCCESS", "Response: " + response);
                    Toast.makeText(this, "Image published to Facebook!", Toast.LENGTH_LONG).show();
                },
                error -> {
                    String errorMsg = "Error";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            errorMsg = new String(error.networkResponse.data, "UTF-8");
                        } catch (Exception e) {
                            errorMsg = error.toString();
                        }
                    } else {
                        errorMsg = error.toString();
                    }
                    Log.e("IMG_ERROR", errorMsg);
                    Toast.makeText(this, "FB Image Error: " + errorMsg, Toast.LENGTH_LONG).show();
                }
        ) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                // For demonstration, we use the sample URL from the user request
                params.put("url", "https://picsum.photos/500"); 
                params.put("caption", postDesc);
                params.put("access_token", pageToken);
                return params;
            }
        };

        queue.add(request);
    }

}