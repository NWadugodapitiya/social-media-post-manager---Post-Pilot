package com.postpilot.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private Handler redirectHandler;
    private Runnable redirectRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup the "Start Writing" button click
        Button btnStart = findViewById(R.id.btn_start);
        btnStart.setOnClickListener(v -> {
            // Cancel the automatic redirect if button is clicked
            if (redirectHandler != null && redirectRunnable != null) {
                redirectHandler.removeCallbacks(redirectRunnable);
            }
            navigateToCreatePost();
        });

        // 3 seconds delay then redirect to Dashboard automatically
        redirectHandler = new Handler(Looper.getMainLooper());
        redirectRunnable = () -> {
            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
        };
        redirectHandler.postDelayed(redirectRunnable, 3000);
    }

    private void navigateToCreatePost() {
        Intent intent = new Intent(MainActivity.this, CreatePostActivity.class);
        startActivity(intent);
        // We can keep MainActivity in stack or finish it depending on preference
        // finish(); 
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Prevent memory leaks
        if (redirectHandler != null && redirectRunnable != null) {
            redirectHandler.removeCallbacks(redirectRunnable);
        }
    }
}
