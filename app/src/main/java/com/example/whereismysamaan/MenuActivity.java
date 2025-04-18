package com.example.whereismysamaan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MenuActivity extends AppCompatActivity {
    private static final String TAG = "MenuActivity";

    // UI Components
    private ImageView btnBack;
    private LinearLayout menuReminder, menuShareSamaan, menuDarkMode;
    private LinearLayout menuLanguage, menuSettings, menuShareApp;
    private LinearLayout menuRateUs, menuReportIssues, menuSuggestFeature;
    private LinearLayout menuTerms, menuHelp;
    private SwitchCompat switchDarkMode;
    private TextView tvLastSync, tvVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nav_menu);
        
        try {
            // Initialize views
            initViews();
            
            // Set click listeners
            setupClickListeners();
            
            // Handle back press with the new API
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    setResult(RESULT_OK);
                    finish();
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error during initialization: " + e.getMessage(), e);
            Toast.makeText(this, "Error during initialization: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    
    private void initViews() {
        try {
            // Back button
            btnBack = findViewById(R.id.btn_back);
            
            // Version info
            tvLastSync = findViewById(R.id.tv_last_sync);
            tvVersion = findViewById(R.id.tv_version);
            
            // Set version and last sync
            String version = "Version 1.0";
            tvVersion.setText(version);
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault());
            String lastSync = "Last Sync: " + sdf.format(new Date());
            tvLastSync.setText(lastSync);
            
            // Menu items
            menuReminder = findViewById(R.id.menu_reminder);
            menuShareSamaan = findViewById(R.id.menu_share_samaan);
            menuDarkMode = findViewById(R.id.menu_dark_mode);
            menuLanguage = findViewById(R.id.menu_language);
            menuSettings = findViewById(R.id.menu_settings);
            menuShareApp = findViewById(R.id.menu_share_app);
            menuRateUs = findViewById(R.id.menu_rate_us);
            menuReportIssues = findViewById(R.id.menu_report_issues);
            menuSuggestFeature = findViewById(R.id.menu_suggest_feature);
            menuTerms = findViewById(R.id.menu_terms);
            menuHelp = findViewById(R.id.menu_help);
            
            // Dark mode switch
            switchDarkMode = findViewById(R.id.switch_dark_mode);
            switchDarkMode.setChecked(isDarkModeEnabled(this));
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
    
    private void setupClickListeners() {
        try {
            // Back button
            btnBack.setOnClickListener(v -> {
                finish();
            });
            
            // Reminder menu item
            menuReminder.setOnClickListener(v -> {
                try {
                    Toast.makeText(MenuActivity.this, "Opening periodic reminder settings", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MenuActivity.this, PeriodicReminderActivity.class));
                    setResult(RESULT_OK);
                } catch (Exception e) {
                    Log.e(TAG, "Error in menuReminder click: " + e.getMessage(), e);
                }
            });
            
            // Share Samaan menu item
            menuShareSamaan.setOnClickListener(v -> {
                try {
                    Toast.makeText(MenuActivity.this, "Share Samaan with friends clicked", Toast.LENGTH_SHORT).show();
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out my Samaan list");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "I've organized my items using WhereIsMySamaan app. You should try it too!");
                    startActivity(Intent.createChooser(shareIntent, "Share via"));
                } catch (Exception e) {
                    Log.e(TAG, "Error in menuShareSamaan click: " + e.getMessage(), e);
                }
            });
            
            // Dark Mode Switch
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                try {
                    // Save dark mode preference
                    getSharedPreferences("app_preferences", MODE_PRIVATE)
                        .edit()
                        .putBoolean("dark_mode", isChecked)
                        .apply();
                    
                    Toast.makeText(MenuActivity.this, "Dark mode: " + (isChecked ? "On" : "Off") + "\nRestart app to apply changes", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                } catch (Exception e) {
                    Log.e(TAG, "Error in switchDarkMode change: " + e.getMessage(), e);
                }
            });
            
            // Language menu item
            menuLanguage.setOnClickListener(v -> {
                try {
                    Toast.makeText(MenuActivity.this, "Language settings clicked", Toast.LENGTH_SHORT).show();
                    // TODO: Implement language selection dialog or activity
                } catch (Exception e) {
                    Log.e(TAG, "Error in menuLanguage click: " + e.getMessage(), e);
                }
            });
            
            // Settings menu item
            menuSettings.setOnClickListener(v -> {
                try {
                    Toast.makeText(MenuActivity.this, "Opening settings...", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MenuActivity.this, SettingsActivity.class));
                } catch (Exception e) {
                    Log.e(TAG, "Error in menuSettings click: " + e.getMessage(), e);
                    Toast.makeText(MenuActivity.this, "Error opening settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            
            // Share App menu item
            menuShareApp.setOnClickListener(v -> {
                try {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out " + getString(R.string.app_name) + 
                            " - The best way to keep track of your belongings: " +
                            "https://play.google.com/store/apps/details?id=" + getPackageName());
                    startActivity(Intent.createChooser(shareIntent, "Share via"));
                } catch (Exception e) {
                    Log.e(TAG, "Error in menuShareApp click: " + e.getMessage(), e);
                }
            });
            
            // Rate Us menu item
            menuRateUs.setOnClickListener(v -> {
                try {
                    Toast.makeText(MenuActivity.this, "Rate Us clicked", Toast.LENGTH_SHORT).show();
                    // TODO: Implement app rating functionality using Play Store API
                } catch (Exception e) {
                    Log.e(TAG, "Error in menuRateUs click: " + e.getMessage(), e);
                }
            });
            
            // Report Issues menu item
            menuReportIssues.setOnClickListener(v -> {
                try {
                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("message/rfc822");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@whereismysamaan.com"});
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Issue Report - WhereIsMySamaan App");
                    startActivity(Intent.createChooser(emailIntent, "Send Email"));
                } catch (Exception e) {
                    Log.e(TAG, "Error in menuReportIssues click: " + e.getMessage(), e);
                    Toast.makeText(MenuActivity.this, "No email app installed", Toast.LENGTH_SHORT).show();
                }
            });
            
            // Suggest Feature menu item
            menuSuggestFeature.setOnClickListener(v -> {
                try {
                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("message/rfc822");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"suggestions@whereismysamaan.com"});
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feature Suggestion - WhereIsMySamaan App");
                    startActivity(Intent.createChooser(emailIntent, "Send Email"));
                } catch (Exception e) {
                    Log.e(TAG, "Error in menuSuggestFeature click: " + e.getMessage(), e);
                    Toast.makeText(MenuActivity.this, "No email app installed", Toast.LENGTH_SHORT).show();
                }
            });
            
            // Terms & Conditions menu item
            menuTerms.setOnClickListener(v -> {
                try {
                    Toast.makeText(MenuActivity.this, "Terms & Conditions clicked", Toast.LENGTH_SHORT).show();
                    // TODO: Implement Terms & Conditions activity
                } catch (Exception e) {
                    Log.e(TAG, "Error in menuTerms click: " + e.getMessage(), e);
                }
            });
            
            // Help & Support menu item
            menuHelp.setOnClickListener(v -> {
                try {
                    Toast.makeText(MenuActivity.this, "Help & Support clicked", Toast.LENGTH_SHORT).show();
                    // TODO: Implement Help & Support activity
                } catch (Exception e) {
                    Log.e(TAG, "Error in menuHelp click: " + e.getMessage(), e);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners: " + e.getMessage(), e);
            Toast.makeText(this, "Error setting up click listeners: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    
    // Method to check if dark mode is enabled
    public static boolean isDarkModeEnabled(AppCompatActivity activity) {
        return activity.getSharedPreferences("app_preferences", MODE_PRIVATE)
                .getBoolean("dark_mode", false);
    }
} 