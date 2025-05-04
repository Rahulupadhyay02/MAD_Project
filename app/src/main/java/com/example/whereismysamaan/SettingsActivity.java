package com.example.whereismysamaan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    private static final String PREFS_NAME = "app_preferences";
    
    // UI Components
    private ImageView btnBack;
    private LinearLayout settingsLanguage;
    private TextView settingsWallpaper;
    private TextView settingsThemes;
    private TextView settingsFontSize;
    private SwitchCompat switchAllNotifications;
    private SwitchCompat switchReminderNotifications;
    private LinearLayout settingsNotificationTone;
    private LinearLayout settingsSharedHistory;
    private TextView settingsClearShared;
    private TextView settingsTerms;
    private LinearLayout settingsLicenses;
    private TextView tvSettingsVersion;
    
    // Activity Result Launchers
    private ActivityResultLauncher<Intent> ringtonePickerLauncher;
    private ActivityResultLauncher<Intent> wallpaperPickerLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Log.d(TAG, "Starting SettingsActivity onCreate");
            
            setContentView(R.layout.activity_settings);
            
            // Initialize views
            initViews();
            
            // Setup click listeners
            setupClickListeners();
            
            // Load saved preferences
            loadPreferences();
            
            // Register for ringtone picker result
            registerRingtonePicker();
            
            // Register for wallpaper picker result
            registerWallpaperPicker();
            
            // Set version info
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvSettingsVersion.setText(getString(R.string.app_version) + " " + versionName);
            
            // Setup back press handling with the new API
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    setResult(RESULT_OK);
                    finish();
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            e.printStackTrace();
            Toast.makeText(this, "Error initializing settings: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    private void initViews() {
        // Initialize all UI components
        btnBack = findViewById(R.id.btn_back);
        settingsLanguage = findViewById(R.id.settings_language);
        settingsWallpaper = findViewById(R.id.settings_wallpaper);
        settingsThemes = findViewById(R.id.settings_themes);
        settingsFontSize = findViewById(R.id.settings_font_size);
        switchAllNotifications = findViewById(R.id.switch_all_notifications);
        switchReminderNotifications = findViewById(R.id.switch_reminder_notifications);
        settingsNotificationTone = findViewById(R.id.settings_notification_tone);
        settingsSharedHistory = findViewById(R.id.settings_shared_history);
        settingsClearShared = findViewById(R.id.settings_clear_shared);
        settingsTerms = findViewById(R.id.settings_terms);
        settingsLicenses = findViewById(R.id.settings_licenses);
        tvSettingsVersion = findViewById(R.id.tv_settings_version);
    }
    
    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });
        
        // Language settings
        settingsLanguage.setOnClickListener(v -> showLanguageSelection());
        
        // Wallpaper settings
        settingsWallpaper.setOnClickListener(v -> showWallpaperOptions());
        
        // Theme settings
        settingsThemes.setOnClickListener(v -> showThemeOptions());
        
        // Font size settings
        settingsFontSize.setOnClickListener(v -> showFontSizeOptions());
        
        // Notification switches
        switchAllNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationSettings("all_notifications", isChecked);
            // If main notifications are turned off, disable reminder notifications
            if (!isChecked) {
                switchReminderNotifications.setChecked(false);
                switchReminderNotifications.setEnabled(false);
            } else {
                switchReminderNotifications.setEnabled(true);
            }
        });
        
        switchReminderNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationSettings("reminder_notifications", isChecked);
        });
        
        // Notification tone
        settingsNotificationTone.setOnClickListener(v -> selectNotificationTone());
        
        // Shared history
        settingsSharedHistory.setOnClickListener(v -> showSharedHistory());
        
        // Clear shared history
        settingsClearShared.setOnClickListener(v -> confirmClearSharedHistory());
        
        // Terms & Conditions
        settingsTerms.setOnClickListener(v -> showTermsAndConditions());
        
        // Open Source Licenses
        settingsLicenses.setOnClickListener(v -> showOpenSourceLicenses());
    }
    
    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Load notification preferences
        boolean allNotifications = prefs.getBoolean("all_notifications", true);
        boolean reminderNotifications = prefs.getBoolean("reminder_notifications", true);
        
        switchAllNotifications.setChecked(allNotifications);
        switchReminderNotifications.setChecked(reminderNotifications);
        switchReminderNotifications.setEnabled(allNotifications);
    }
    
    private void registerRingtonePicker() {
        ringtonePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // Use the modern type-safe approach for Android 13+ compatibility
                    Uri ringtoneUri;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        ringtoneUri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.class);
                    } else {
                        @SuppressWarnings("deprecation")
                        Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                        ringtoneUri = uri;
                    }
                    if (ringtoneUri != null) {
                        saveRingtoneUri(ringtoneUri.toString());
                        
                        // Show the selected ringtone name
                        Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
                        String ringtoneName = ringtone.getTitle(this);
                        Toast.makeText(this, "Selected: " + ringtoneName, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }
    
    private void registerWallpaperPicker() {
        wallpaperPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        saveWallpaperImage(selectedImageUri.toString());
                        applyWallpaper(selectedImageUri);
                        Toast.makeText(this, "Custom wallpaper applied", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }
    
    private void showLanguageSelection() {
        final String[] languages = {"English", "Hindi", "Bengali", "Telugu", "Tamil", "Marathi", "Gujarati"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Language");
        
        // Get current language preference
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int selectedLanguage = prefs.getInt("selected_language", 0); // Default to English (index 0)
        
        builder.setSingleChoiceItems(languages, selectedLanguage, (dialog, which) -> {
            // Save language preference
            prefs.edit().putInt("selected_language", which).apply();
            
            Toast.makeText(SettingsActivity.this, 
                    "Language set to " + languages[which] + "\nRestart app to apply changes", 
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showWallpaperOptions() {
        final String[] options = {"Choose from Gallery", "Default Wallpapers", "Solid Colors", "Custom Wallpaper"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Wallpaper");
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Choose from Gallery
                    openGalleryForWallpaper();
                    break;
                case 1: // Default Wallpapers
                    showDefaultWallpapers();
                    break;
                case 2: // Solid Colors
                    showColorPicker();
                    break;
                case 3: // Custom Wallpaper
                    showCustomWallpaperOptions();
                    break;
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void openGalleryForWallpaper() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        try {
            wallpaperPickerLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening gallery: " + e.getMessage(), e);
            Toast.makeText(this, "Error opening gallery", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveWallpaperImage(String imageUriString) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString("wallpaper_uri", imageUriString).apply();
        prefs.edit().putString("wallpaper_type", "image").apply();
    }
    
    private void applyWallpaper(Uri imageUri) {
        // Send broadcast to notify wallpaper change - MainActivity will handle this
        Intent intent = new Intent("com.example.whereismysamaan.WALLPAPER_CHANGED");
        intent.putExtra("wallpaper_uri", imageUri.toString());
        intent.putExtra("wallpaper_type", "image");
        sendBroadcast(intent);
    }
    
    private void showDefaultWallpapers() {
        // Default wallpaper resource IDs
        final int[] wallpaperResources = {
            R.drawable.ic_home, // Replace with actual wallpapers
            R.drawable.ic_office,
            R.drawable.ic_warehouse,
            R.drawable.ic_shop,
            R.drawable.ic_car
        };
        
        // Create a grid view or recycler view to display wallpaper thumbnails
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Default Wallpaper");
        
        // Simplified for now - would typically use a GridView
        builder.setItems(new String[]{"Wallpaper 1", "Wallpaper 2", "Wallpaper 3", "Wallpaper 4", "Wallpaper 5"}, 
            (dialog, which) -> {
                // Save selected default wallpaper
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putInt("default_wallpaper_resource", wallpaperResources[which]).apply();
                prefs.edit().putString("wallpaper_type", "default").apply();
                
                // Notify MainActivity about wallpaper change
                Intent intent = new Intent("com.example.whereismysamaan.WALLPAPER_CHANGED");
                intent.putExtra("wallpaper_type", "default");
                intent.putExtra("wallpaper_resource_id", wallpaperResources[which]);
                sendBroadcast(intent);
                
                Toast.makeText(this, "Default wallpaper applied", Toast.LENGTH_SHORT).show();
            }
        );
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showCustomWallpaperOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Custom Wallpaper Options");
        
        final String[] customOptions = {"Create from Text", "Create Pattern", "Use URL"};
        
        builder.setItems(customOptions, (dialog, which) -> {
            switch (which) {
                case 0: // Create from Text
                    showTextWallpaperCreator();
                    break;
                case 1: // Create Pattern
                    showPatternWallpaperCreator();
                    break;
                case 2: // Use URL
                    showUrlWallpaperInput();
                    break;
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showTextWallpaperCreator() {
        // Implementation for custom text wallpaper
        Toast.makeText(this, "Text wallpaper feature coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void showPatternWallpaperCreator() {
        // Implementation for pattern wallpaper
        Toast.makeText(this, "Pattern wallpaper feature coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void showUrlWallpaperInput() {
        // Implementation for URL wallpaper
        Toast.makeText(this, "URL wallpaper feature coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void showColorPicker() {
        final String[] colors = {"White", "Light Gray", "Light Blue", "Light Green", "Light Yellow", "Orange"};
        final String[] colorValues = {"#FFFFFF", "#F5F5F5", "#E3F2FD", "#E8F5E9", "#FFFDE7", "#FFF3E0"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Background Color");
        
        builder.setItems(colors, (dialog, which) -> {
            saveBackgroundColor(colorValues[which]);
            
            // Notify MainActivity about color change
            Intent intent = new Intent("com.example.whereismysamaan.WALLPAPER_CHANGED");
            intent.putExtra("wallpaper_type", "color");
            intent.putExtra("color_value", colorValues[which]);
            sendBroadcast(intent);
            
            Toast.makeText(SettingsActivity.this, 
                    "Background color set to " + colors[which], 
                    Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void saveBackgroundColor(String colorValue) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString("background_color", colorValue).apply();
        prefs.edit().putString("wallpaper_type", "color").apply();
    }
    
    private void showThemeOptions() {
        final String[] themes = {"Light Mode", "Dark Mode", "High Contrast", "Orange", "Blue", "Green"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Theme");
        
        // Get current theme preference
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int selectedTheme = prefs.getInt("selected_theme", 0); // Default to Light Mode
        
        builder.setSingleChoiceItems(themes, selectedTheme, (dialog, which) -> {
            // Save theme preference
            prefs.edit().putInt("selected_theme", which).apply();
            
            // Apply the theme
            applyTheme(which);
            
            Toast.makeText(SettingsActivity.this, 
                    "Theme set to " + themes[which], 
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void applyTheme(int themeIndex) {
        // Send broadcast to notify theme change
        Intent intent = new Intent("com.example.whereismysamaan.THEME_CHANGED");
        intent.putExtra("theme_index", themeIndex);
        sendBroadcast(intent);
        
        // You can implement immediate theme change here
        // This would require implementing a ThemeManager class to handle theme application
        // For demonstration purposes, we're just showing a toast
        Toast.makeText(this, "Theme will apply on app restart", Toast.LENGTH_SHORT).show();
    }
    
    private void showFontSizeOptions() {
        final String[] fontSizes = {"Small", "Medium (Default)", "Large", "Extra Large"};
        final float[] fontScaleValues = {0.85f, 1.0f, 1.15f, 1.3f};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Font Size");
        
        // Get current font size preference
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        float currentScale = prefs.getFloat("font_scale", 1.0f);
        int selectedIndex = 1; // Default to Medium
        
        // Find the closest match to the current scale
        for (int i = 0; i < fontScaleValues.length; i++) {
            if (Math.abs(currentScale - fontScaleValues[i]) < 0.05f) {
                selectedIndex = i;
                break;
            }
        }
        
        builder.setSingleChoiceItems(fontSizes, selectedIndex, (dialog, which) -> {
            // Save font size preference
            prefs.edit().putFloat("font_scale", fontScaleValues[which]).apply();
            
            // Apply font size
            applyFontSize(fontScaleValues[which]);
            
            Toast.makeText(SettingsActivity.this, 
                    "Font size set to " + fontSizes[which], 
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void applyFontSize(float scale) {
        // Send broadcast to notify font size change
        Intent intent = new Intent("com.example.whereismysamaan.FONT_SIZE_CHANGED");
        intent.putExtra("font_scale", scale);
        sendBroadcast(intent);
        
        // Display a message about app restart
        if (scale != 1.0f) {
            Toast.makeText(this, "Font size will fully apply on app restart", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveNotificationSettings(String key, boolean isEnabled) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(key, isEnabled).apply();
        
        Toast.makeText(this, 
                key.replace("_", " ").toUpperCase(Locale.getDefault()) + " " + (isEnabled ? "enabled" : "disabled"), 
                Toast.LENGTH_SHORT).show();
    }
    
    private void selectNotificationTone() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Tone");
        
        // Get current ringtone
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String ringtoneUriString = prefs.getString("notification_tone", null);
        
        if (ringtoneUriString != null) {
            Uri ringtoneUri = Uri.parse(ringtoneUriString);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri);
        } else {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Settings.System.DEFAULT_NOTIFICATION_URI);
        }
        
        ringtonePickerLauncher.launch(intent);
    }
    
    private void saveRingtoneUri(String ringtoneUriString) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString("notification_tone", ringtoneUriString).apply();
    }
    
    private void showSharedHistory() {
        Toast.makeText(this, "Shared history feature coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void confirmClearSharedHistory() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear Shared History");
        builder.setMessage("Are you sure you want to clear all shared history? This action cannot be undone.");
        
        builder.setPositiveButton("Clear", (dialog, which) -> {
            // Clear shared history from preferences
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().remove("shared_history").apply();
            
            Toast.makeText(SettingsActivity.this, "Shared history cleared", Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showTermsAndConditions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Terms & Conditions");
        
        // Create a ScrollView to hold the text
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        TextView textView = new TextView(this);
        
        // Set padding and text
        textView.setPadding(30, 30, 30, 30);
        textView.setText(
                "Terms and Conditions for WhereIsMySamaan App\n\n" +
                "Last Updated: 2023-09-01\n\n" +
                "1. ACCEPTANCE OF TERMS\n\n" +
                "By downloading, installing, or using WhereIsMySamaan application, you agree to be bound by these Terms and Conditions. If you do not agree to these terms, please do not use the app.\n\n" +
                "2. DESCRIPTION OF SERVICE\n\n" +
                "WhereIsMySamaan is a personal inventory management application that allows users to track and organize their belongings.\n\n" +
                "3. PRIVACY POLICY\n\n" +
                "Your privacy is important to us. Please review our Privacy Policy to understand how we collect, use, and protect your personal information.\n\n" +
                "4. USER CONTENT\n\n" +
                "You retain ownership of any content you upload to the application. However, you grant us a non-exclusive license to use, modify, and display such content for the purpose of providing our services.\n\n" +
                "5. PROHIBITED ACTIVITIES\n\n" +
                "When using WhereIsMySamaan, you agree not to:\n" +
                "- Violate any applicable laws or regulations\n" +
                "- Infringe upon the rights of others\n" +
                "- Use the app for illegal activities\n" +
                "- Attempt to gain unauthorized access to the app's systems\n\n" +
                "6. LIMITATION OF LIABILITY\n\n" +
                "To the maximum extent permitted by law, we shall not be liable for any indirect, incidental, special, consequential, or punitive damages resulting from your use of or inability to use the app.\n\n" +
                "7. CHANGES TO TERMS\n\n" +
                "We reserve the right to modify these Terms at any time. Your continued use of the app after such changes constitutes your acceptance of the new Terms.\n\n" +
                "8. CONTACT INFORMATION\n\n" +
                "If you have any questions about these Terms, please contact us at support@whereismysamaan.com."
        );
        
        scrollView.addView(textView);
        builder.setView(scrollView);
        
        builder.setPositiveButton("I Agree", (dialog, which) -> {
            dialog.dismiss();
        });
        
        builder.show();
    }
    
    private void showOpenSourceLicenses() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Open Source Licenses");
        
        // Create a ScrollView to hold the text
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        TextView textView = new TextView(this);
        
        // Set padding and text
        textView.setPadding(30, 30, 30, 30);
        textView.setText(
                "Open Source Libraries Used:\n\n" +
                "1. AndroidX AppCompat\n" +
                "License: Apache License 2.0\n\n" +
                "2. Material Components for Android\n" +
                "License: Apache License 2.0\n\n" +
                "3. AndroidX ConstraintLayout\n" +
                "License: Apache License 2.0\n\n" +
                "4. AndroidX DrawerLayout\n" +
                "License: Apache License 2.0\n\n" +
                "5. AndroidX CardView\n" +
                "License: Apache License 2.0\n\n" +
                "Full license texts available at:\n" +
                "https://www.apache.org/licenses/LICENSE-2.0"
        );
        
        scrollView.addView(textView);
        builder.setView(scrollView);
        
        builder.setPositiveButton("Close", (dialog, which) -> {
            dialog.dismiss();
        });
        
        builder.show();
    }
} 