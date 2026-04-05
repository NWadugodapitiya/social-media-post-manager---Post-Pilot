package com.postpilot.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.Arrays;

import com.facebook.login.LoginManager;

public class ProfileActivity extends AppCompatActivity {

    private EditText etName;
    private TextView tvFbStatus;
    private SwitchCompat switchFingerprint, switchDarkMode;
    private Button btnSave;
    private SharedPreferences sharedPreferences;
    private CallbackManager callbackManager;

    private static final String SHARED_PREF_NAME = "PostPilotPrefs";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_FINGERPRINT = "fingerprintEnabled";
    private static final String KEY_DARK_MODE = "isDarkMode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        etName = findViewById(R.id.et_profile_name);
        tvFbStatus = findViewById(R.id.tv_fb_status);
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

        // Initialize Facebook Login
        callbackManager = CallbackManager.Factory.create();
        LoginButton loginButton = findViewById(R.id.login_button);
        loginButton.setPermissions(Arrays.asList("pages_show_list", "pages_read_engagement", "pages_manage_posts"));

        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                String token = loginResult.getAccessToken().getToken();
                getPages(token);
            }

            @Override
            public void onCancel() {
                Toast.makeText(ProfileActivity.this, "Login Cancelled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull FacebookException error) {
                Toast.makeText(ProfileActivity.this, "Login Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Listen for Logout to clear our internal page data
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) { } // Already handled

            @Override
            public void onCancel() { }

            @Override
            public void onError(@NonNull FacebookException error) { }
        });
        
        // Manual check if logged out or need to refresh
        com.facebook.AccessToken currentToken = com.facebook.AccessToken.getCurrentAccessToken();
        if (currentToken == null) {
            clearFacebookPrefs();
        } else if (sharedPreferences.getString("fb_page_id", null) == null) {
            // Logged in to FB but Page info not saved, refresh it
            getPages(currentToken.getToken());
        }
    }


    private void loadProfileData() {
        String name = sharedPreferences.getString(KEY_USER_NAME, "User Name");
        boolean fingerprint = sharedPreferences.getBoolean(KEY_FINGERPRINT, false);
        boolean darkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false);
        
        String fbPageName = sharedPreferences.getString("fb_page_name", null);
        if (fbPageName != null) {
            tvFbStatus.setText("Connected to: " + fbPageName);
            tvFbStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvFbStatus.setText("Not connected");
            tvFbStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }

        etName.setText(name);
        switchFingerprint.setChecked(fingerprint);
        switchDarkMode.setChecked(darkMode);
    }

    private void clearFacebookPrefs() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("fb_page_id");
        editor.remove("fb_page_token");
        editor.remove("fb_page_name");
        editor.apply();
        tvFbStatus.setText("Not connected");
        tvFbStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
    }

    private void saveProfileData() {
        String name = etName.getText().toString().trim();
        boolean fingerprint = switchFingerprint.isChecked();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_NAME, name);
        editor.putBoolean(KEY_FINGERPRINT, fingerprint);
        editor.putBoolean(KEY_DARK_MODE, switchDarkMode.isChecked());
        editor.apply();

        Toast.makeText(this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void getPages(String userToken) {
        String url = "https://graph.facebook.com/v25.0/me/accounts?access_token=" + userToken;
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray data = response.getJSONArray("data");
                        if (data.length() > 0) {
                            JSONObject page = data.getJSONObject(0);
                            String pageName = page.getString("name");
                            String pageId = page.getString("id");
                            String pageToken = page.getString("access_token");

                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("fb_page_id", pageId);
                            editor.putString("fb_page_token", pageToken);
                            editor.putString("fb_page_name", pageName);
                            editor.apply();

                            tvFbStatus.setText("Connected to: " + pageName);
                            tvFbStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                            Toast.makeText(ProfileActivity.this, "Connected to: " + pageName, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ProfileActivity.this, "No Facebook Pages found.", Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("FB_SYNC", error.toString())
        );
        queue.add(request);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (callbackManager != null) {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }
}
