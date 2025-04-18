package com.example.whereismysamaan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    
    // Preference constants
    private static final String PROFILE_PREFS = "ProfilePrefs";
    private static final String PREF_NAME = "user_name";
    private static final String PREF_EMAIL = "user_email";
    private static final String PREF_USERNAME = "user_username";
    private static final String PREF_PHONE = "user_phone";
    private static final String PREF_ABOUT = "user_about";
    
    // UI Components
    private ImageView ivProfilePhoto;
    private ImageView ivEditPhoto;
    private TextView tvProfileName;
    private TextView tvProfileSubtitle;
    private EditText etProfileName;
    private EditText etProfileUsername;
    private EditText etProfileEmail;
    private EditText etProfilePhone;
    private EditText etProfileAbout;
    private LinearLayout profileLogout;
    private LinearLayout profileDeleteAccount;
    private TextView tvProfileVersion;
    
    // For image selection
    private ActivityResultLauncher<String> imagePickerLauncher;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nav_profile);
        
        try {
            initViews();
            setupListeners();
            loadUserProfile();
            setupImagePicker();
            
            // Handle back press with the new API
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    setResult(RESULT_OK);
                    finish();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing profile activity: " + e.getMessage());
            Toast.makeText(this, "Error loading profile. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void initViews() {
        ivProfilePhoto = findViewById(R.id.iv_profile_photo);
        ivEditPhoto = findViewById(R.id.iv_edit_photo);
        tvProfileName = findViewById(R.id.tv_profile_name);
        tvProfileSubtitle = findViewById(R.id.tv_profile_subtitle);
        etProfileName = findViewById(R.id.et_profile_name);
        etProfileUsername = findViewById(R.id.et_profile_username);
        etProfileEmail = findViewById(R.id.et_profile_email);
        etProfilePhone = findViewById(R.id.et_profile_phone);
        etProfileAbout = findViewById(R.id.et_profile_about);
        profileLogout = findViewById(R.id.profile_logout);
        profileDeleteAccount = findViewById(R.id.profile_delete_account);
        tvProfileVersion = findViewById(R.id.tv_profile_version);
    }
    
    private void setupListeners() {
        ivEditPhoto.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });
        
        profileLogout.setOnClickListener(v -> {
            showLogoutConfirmation();
        });
        
        profileDeleteAccount.setOnClickListener(v -> {
            showDeleteAccountConfirmation();
        });
    }
    
    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivProfilePhoto.setImageURI(uri);
                    // Store the URI in SharedPreferences or save to internal storage
                    saveProfileImageUri(uri.toString());
                }
            }
        );
    }
    
    private void saveProfileImageUri(String uriString) {
        SharedPreferences prefs = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        prefs.edit().putString("profile_image_uri", uriString).apply();
    }
    
    private void loadUserProfile() {
        // Load profile from SharedPreferences
        try {
            SharedPreferences prefs = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
            
            // Check if user profile exists, if not create default values
            if (!prefs.contains(PREF_NAME)) {
                prefs.edit()
                    .putString(PREF_NAME, "John Doe")
                    .putString(PREF_EMAIL, "john.doe@example.com")
                    .putString(PREF_USERNAME, "johndoe123")
                    .putString(PREF_PHONE, "+91 9876543210")
                    .putString(PREF_ABOUT, "")
                    .apply();
            }
            
            // Load values
            String name = prefs.getString(PREF_NAME, "");
            String email = prefs.getString(PREF_EMAIL, "");
            String username = prefs.getString(PREF_USERNAME, "");
            String phone = prefs.getString(PREF_PHONE, "");
            String about = prefs.getString(PREF_ABOUT, "");
            
            // Set values to UI
            tvProfileName.setText(name);
            etProfileName.setText(name);
            etProfileEmail.setText(email);
            etProfileUsername.setText(username);
            etProfilePhone.setText(phone);
            etProfileAbout.setText(about);
            
            // Set app version
            tvProfileVersion.setText("Version 1.0.0");
            
            // Load profile picture if exists
            String imageUriString = prefs.getString("profile_image_uri", null);
            if (imageUriString != null) {
                try {
                    Uri imageUri = Uri.parse(imageUriString);
                    ivProfilePhoto.setImageURI(imageUri);
                    selectedImageUri = imageUri;
                } catch (Exception e) {
                    Log.e(TAG, "Error loading profile image: " + e.getMessage());
                    // Use default profile image
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading user profile: " + e.getMessage());
            Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes", (dialog, which) -> {
                logout();
            })
            .setNegativeButton("No", null)
            .show();
    }
    
    private void logout() {
        try {
            // Clear user session/preferences (but keep profile data)
            SharedPreferences appPrefs = getSharedPreferences("app_preferences", MODE_PRIVATE);
            appPrefs.edit()
                .remove("is_logged_in")
                .apply();
            
            // Show success message
            Toast.makeText(this, "You have been logged out", Toast.LENGTH_SHORT).show();
            
            // Navigate to login screen
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error during logout: " + e.getMessage());
            Toast.makeText(this, "Failed to logout", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showDeleteAccountConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                deleteAccount();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void deleteAccount() {
        try {
            // Clear all user data
            SharedPreferences profilePrefs = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
            profilePrefs.edit().clear().apply();
            
            SharedPreferences appPrefs = getSharedPreferences("app_preferences", MODE_PRIVATE);
            appPrefs.edit()
                .remove("is_logged_in")
                .apply();
            
            // Show success message
            Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
            
            // Navigate to login screen
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting account: " + e.getMessage());
            Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show();
        }
    }
} 