package com.postpilot.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private Handler redirectHandler;
    private Runnable redirectRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before anything else
        SharedPreferences sharedPref = getSharedPreferences("PostPilotPrefs", MODE_PRIVATE);
        boolean isDarkMode = sharedPref.getBoolean("isDarkMode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

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

        // Check if Fingerprint is enabled
        SharedPreferences sharedPreferences = getSharedPreferences("PostPilotPrefs", MODE_PRIVATE);
        boolean isFingerprintEnabled = sharedPreferences.getBoolean("fingerprintEnabled", false);

        if (isFingerprintEnabled) {
            // Wait a small bit then show prompt
            new Handler(Looper.getMainLooper()).postDelayed(this::showBiometricPrompt, 1000);
        } else {
            // 3 seconds delay then redirect to Dashboard automatically
            redirectHandler = new Handler(Looper.getMainLooper());
            redirectRunnable = () -> {
                Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                startActivity(intent);
                finish();
            };
            redirectHandler.postDelayed(redirectRunnable, 3000);
        }
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Login")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Cancel")
                .build();

        biometricPrompt.authenticate(promptInfo);
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
