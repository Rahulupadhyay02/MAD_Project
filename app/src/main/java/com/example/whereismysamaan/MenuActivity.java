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
                    // Create a dialog for language selection
                    final String[] languages = {"English", "Hindi", "Bengali", "Telugu", "Tamil", "Marathi", "Gujarati"};
                    
                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
                    builder.setTitle("Select Language");
                    
                    // Get current language preference
                    SharedPreferences prefs = getSharedPreferences("app_preferences", MODE_PRIVATE);
                    int selectedLanguage = prefs.getInt("selected_language", 0); // Default to English (index 0)
                    
                    builder.setSingleChoiceItems(languages, selectedLanguage, (dialog, which) -> {
                        // Save language preference
                        prefs.edit().putInt("selected_language", which).apply();
                        
                        Toast.makeText(MenuActivity.this, 
                                "Language set to " + languages[which] + "\nRestart app to apply changes", 
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                    
                    builder.setNegativeButton("Cancel", null);
                    builder.show();
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
                    // Open Play Store to rate the app
                    Uri uri = Uri.parse("market://details?id=" + getPackageName());
                    Intent rateIntent = new Intent(Intent.ACTION_VIEW, uri);
                    
                    // To catch if the Play Store app is not installed
                    if (rateIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(rateIntent);
                    } else {
                        // If Play Store not available, open browser
                        Uri webUri = Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName());
                        Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
                        startActivity(webIntent);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in menuRateUs click: " + e.getMessage(), e);
                    Toast.makeText(MenuActivity.this, "Could not open app store", Toast.LENGTH_SHORT).show();
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
                    // Create an AlertDialog to show terms and conditions
                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
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
                } catch (Exception e) {
                    Log.e(TAG, "Error in menuTerms click: " + e.getMessage(), e);
                }
            });
            
            // Help & Support menu item
            menuHelp.setOnClickListener(v -> {
                try {
                    // Create an AlertDialog with help topics
                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
                    builder.setTitle("Help & Support");
                    
                    final String[] helpTopics = {
                            "How to add items", 
                            "Managing locations", 
                            "Setting reminders", 
                            "Searching for items",
                            "Sharing items",
                            "Contact support"
                    };
                    
                    builder.setItems(helpTopics, (dialog, which) -> {
                        switch (which) {
                            case 0: // How to add items
                            case 1: // Managing locations
                            case 2: // Setting reminders
                            case 3: // Searching for items
                            case 4: // Sharing items
                                showHelpContent(helpTopics[which]);
                                break;
                            case 5: // Contact support
                                // Open email client to contact support
                                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                                emailIntent.setType("message/rfc822");
                                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@whereismysamaan.com"});
                                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Help Request - WhereIsMySamaan App");
                                try {
                                    startActivity(Intent.createChooser(emailIntent, "Send Email"));
                                } catch (android.content.ActivityNotFoundException ex) {
                                    Toast.makeText(MenuActivity.this, "No email app installed", Toast.LENGTH_SHORT).show();
                                }
                                break;
                        }
                    });
                    
                    builder.setNegativeButton("Close", null);
                    builder.show();
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
    
    // Helper method to show help content for different topics
    private void showHelpContent(String topic) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(topic);
        
        // Create a ScrollView with content
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        TextView textView = new TextView(this);
        textView.setPadding(30, 30, 30, 30);
        
        // Set content based on selected topic
        switch (topic) {
            case "How to add items":
                textView.setText(
                        "How to Add Items\n\n" +
                        "1. Tap the '+' button at the bottom of the home screen\n" +
                        "2. Enter the item name and details\n" +
                        "3. Select a location for the item\n" +
                        "4. Optionally add a photo of the item\n" +
                        "5. Tap 'Save' to add the item to your inventory\n\n" +
                        "You can also add multiple items at once by using the 'Add Multiple' option."
                );
                break;
            case "Managing locations":
                textView.setText(
                        "Managing Locations\n\n" +
                        "1. Tap on any location on the home screen\n" +
                        "2. View all items in that location\n" +
                        "3. To add a new location, go to Settings > Manage Locations\n" +
                        "4. Tap the '+' button to add a new location\n" +
                        "5. Enter location name and optional details\n" +
                        "6. Tap 'Save' to add the new location\n\n" +
                        "You can edit or delete existing locations from the Manage Locations screen."
                );
                break;
            case "Setting reminders":
                textView.setText(
                        "Setting Reminders\n\n" +
                        "1. Open the menu by tapping the menu icon\n" +
                        "2. Select 'Reminder Settings'\n" +
                        "3. Toggle 'Periodic Reminders' to enable or disable\n" +
                        "4. Select reminder frequency (daily, weekly, monthly)\n" +
                        "5. Set preferred time for reminders\n" +
                        "6. Tap 'Save' to apply changes\n\n" +
                        "You can also set individual reminders for specific items from the item details screen."
                );
                break;
            case "Searching for items":
                textView.setText(
                        "Searching for Items\n\n" +
                        "1. Use the search bar at the top of the home screen\n" +
                        "2. Enter the name or partial name of the item\n" +
                        "3. Tap the search icon or press Enter\n" +
                        "4. View search results showing matching items\n" +
                        "5. Tap on any item to view details\n\n" +
                        "You can also filter search results by location using the filter option."
                );
                break;
            case "Sharing items":
                textView.setText(
                        "Sharing Items\n\n" +
                        "1. From the home screen, tap on the item you want to share\n" +
                        "2. In the item details screen, tap the 'Share' icon\n" +
                        "3. Select sharing method (WhatsApp, Email, etc.)\n" +
                        "4. Add recipients and optional message\n" +
                        "5. Tap 'Share' to send the information\n\n" +
                        "You can also share your entire inventory from Menu > Share Samaan."
                );
                break;
            default:
                textView.setText("No information available for this topic.");
                break;
        }
        
        scrollView.addView(textView);
        builder.setView(scrollView);
        
        builder.setPositiveButton("Close", null);
        builder.show();
    }
} 