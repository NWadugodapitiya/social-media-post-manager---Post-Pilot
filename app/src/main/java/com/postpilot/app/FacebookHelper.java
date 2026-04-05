package com.postpilot.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class FacebookHelper {

    private Context context;
    private SharedPreferences sharedPreferences;

    public FacebookHelper(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences("PostPilotPrefs", Context.MODE_PRIVATE);
    }

    public void publishPost(String content, String imageUrls) {
        String pageId = sharedPreferences.getString("fb_page_id", null);
        String pageToken = sharedPreferences.getString("fb_page_token", null);

        if (pageId == null || pageToken == null) {
            Log.e("FB_HELPER", "Not connected to Facebook");
            return;
        }

        if (imageUrls != null && !imageUrls.isEmpty()) {
            // Get the first image URI from the comma-separated string
            String firstImageUri = imageUrls.split(",")[0];
            postImage(pageId, pageToken, content, Uri.parse(firstImageUri));
        } else {
            postText(pageId, pageToken, content);
        }
    }

    private void postText(String pageId, String pageToken, String content) {
        String url = "https://graph.facebook.com/v20.0/" + pageId + "/feed";
        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> Log.d("FB_SUCCESS", "Post Published: " + response),
                error -> Log.e("FB_ERROR", "Error: " + error.toString())
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("message", content);
                params.put("access_token", pageToken);
                return params;
            }
        };
        queue.add(request);
    }

    private void postImage(String pageId, String pageToken, String content, Uri imageUri) {
        String url = "https://graph.facebook.com/v20.0/" + pageId + "/photos";
        
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            byte[] imageData = bos.toByteArray();

            VolleyMultipartRequest request = new VolleyMultipartRequest(Request.Method.POST, url,
                    response -> Log.d("FB_SUCCESS", "Image Upload Success: " + new String(response.data)),
                    error -> Log.e("FB_ERROR", "Image Upload Error: " + error.toString())
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("message", content);
                    params.put("access_token", pageToken);
                    return params;
                }

                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    params.put("source", new DataPart("post_image.jpg", imageData));
                    return params;
                }
            };
            
            Volley.newRequestQueue(context).add(request);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

