package com.postpilot.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

public class ProfileActivity extends AppCompatActivity {

    private EditText etName, etFbToken;
    private SwitchCompat switchFingerprint, switchDarkMode;
    private Button btnSave;
    private SharedPreferences sharedPreferences;

    private static final String SHARED_PREF_NAME = "PostPilotPrefs";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_FB_TOKEN = "fbToken";
    private static final String KEY_FINGERPRINT = "fingerprintEnabled";
    private static final String KEY_DARK_MODE = "isDarkMode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        etName = findViewById(R.id.et_profile_name);
        etFbToken = findViewById(R.id.et_facebook_token);
        switchFingerprint = findViewById(R.id.switch_fingerprint);
        switchDarkMode = findViewById(R.id.switch_dark_mode);
        btnSave = findViewById(R.id.btn_save_profile);

        sharedPreferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);

        // Load existing data
        loadProfileData();

        findViewById(R.id.iv_back_profile).setOnClickListener(v -> finish());

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        btnSave.setOnClickListener(v -> saveProfileData());
    }

    private void loadProfileData() {
        String name = sharedPreferences.getString(KEY_USER_NAME, "User Name");
        String token = sharedPreferences.getString(KEY_FB_TOKEN, "");
        boolean fingerprint = sharedPreferences.getBoolean(KEY_FINGERPRINT, false);
        boolean darkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false);

        etName.setText(name);
        etFbToken.setText(token);
        switchFingerprint.setChecked(fingerprint);
        switchDarkMode.setChecked(darkMode);
    }

    private void saveProfileData() {
        String name = etName.getText().toString().trim();
        String token = etFbToken.getText().toString().trim();
        boolean fingerprint = switchFingerprint.isChecked();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_FB_TOKEN, token);
        editor.putBoolean(KEY_FINGERPRINT, fingerprint);
        editor.putBoolean(KEY_DARK_MODE, switchDarkMode.isChecked());
        editor.apply();

        Toast.makeText(this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}
