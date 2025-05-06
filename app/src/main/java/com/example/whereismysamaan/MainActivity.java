package com.example.whereismysamaan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.activity.OnBackPressedCallback;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.GridLayout;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.graphics.ImageDecoder;

import com.example.whereismysamaan.firebase.FirebaseHelper;
import com.example.whereismysamaan.model.Location;
import com.example.whereismysamaan.model.User;
import com.example.whereismysamaan.util.AppInitializer;

import java.util.List;

import android.app.Activity;

import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    // Shared preferences constants
    private static final String PREFS_NAME = "app_preferences";
    private static final String PREF_DARK_MODE = "dark_mode";
    
    // Request codes for activity results
    private static final int REQUEST_MENU = 100;
    private static final int REQUEST_PROFILE = 101;

    // UI Components
    private TextView locationHome, locationOffice, locationWarehouse;
    private TextView locationShop, locationCar, locationXyz;
    private ImageView ivMenu, ivSearch, ivProfile;
    private ImageView ivNotification, ivMessage;
    private EditText etSearch;
    private FloatingActionButton fabAdd;
    private DrawerLayout drawerLayout;
    private View navMenuView;
    private View navProfileView;
    
    // Menu Items
    private LinearLayout menuReminder, menuSettings, menuShareSamaan;
    private LinearLayout menuLanguage, menuShareApp, menuRateUs;
    private LinearLayout menuReportIssues, menuSuggestFeature;
    private LinearLayout menuTerms, menuHelp;
    private SwitchCompat switchDarkMode;
    
    // Activity Result Launchers
    private ActivityResultLauncher<Intent> menuActivityLauncher;
    private ActivityResultLauncher<Intent> reminderActivityLauncher;
    private ActivityResultLauncher<Intent> settingsActivityLauncher;

    private GridLayout gridLocations;

    // Firebase helper
    private FirebaseHelper firebaseHelper;

    // New properties
    private Uri selectedImageUri;
    private ActivityResultLauncher<String> pickImage;

    // Constants for broadcasts
    private static final String ACTION_THEME_CHANGED = "com.example.whereismysamaan.THEME_CHANGED";
    private static final String ACTION_FONT_SIZE_CHANGED = "com.example.whereismysamaan.FONT_SIZE_CHANGED";
    private static final String ACTION_WALLPAPER_CHANGED = "com.example.whereismysamaan.WALLPAPER_CHANGED";
    
    // BroadcastReceivers for settings changes
    private BroadcastReceiver themeChangeReceiver;
    private BroadcastReceiver fontSizeChangeReceiver;
    private BroadcastReceiver wallpaperChangeReceiver;
    
    // Main content view
    private LinearLayout toolbarLayout;
    private View mainContentView;

    // Profile update receiver
    private BroadcastReceiver profileUpdateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            
            // Apply saved settings before setting content view
            applyThemeFromPreferences();
            
            setContentView(R.layout.activity_main);
            
            // Set system UI behavior
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
            
            // Register Activity Result Launchers
            registerActivityResultLaunchers();

            // Initialize views
            initViews();

            // Debug the menu view hierarchy
            debugNavMenuViewHierarchy();

            // Set click listeners
            setupClickListeners();
            
            // Set menu item click listeners
            setupMenuClickListeners();
            
            // Setup back press handling
            setupBackPressHandling();

            // Register broadcast receivers
            registerSettingsBroadcastReceivers();
            
            // Register profile update receiver
            registerProfileUpdateReceiver();
            
            // Apply saved wallpaper if exists
            applyWallpaperFromPreferences();
            
            // Apply saved font size
            applyFontSizeFromPreferences();

            // Update profile UI
            updateProfileUI();
            
            // Initialize Firebase helper
            firebaseHelper = FirebaseHelper.getInstance();
            
            // Check if user is logged in, if not redirect to login activity
            if (!firebaseHelper.isUserLoggedIn()) {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }
            
            // Load locations
            loadLocations();

            // Initialize Navigation Views
            navProfileView = findViewById(R.id.nav_profile_view);

            // Initialize the app with default locations if needed
            AppInitializer.initializeAppIfNeeded(this, firebaseHelper);

            // Set up the image picker launcher
            pickImage = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    // Show the image in the profile photo view
                    ImageView ivProfilePhoto = navProfileView.findViewById(R.id.iv_profile_photo);
                    if (ivProfilePhoto != null) {
                        ivProfilePhoto.setImageURI(uri);
                        
                        // Upload the image to Firebase
                        uploadProfileImage();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error starting app", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister receivers safely
        try {
            if (themeChangeReceiver != null) {
                unregisterReceiver(themeChangeReceiver);
            }
            if (fontSizeChangeReceiver != null) {
                unregisterReceiver(fontSizeChangeReceiver);
            }
            if (wallpaperChangeReceiver != null) {
                unregisterReceiver(wallpaperChangeReceiver);
            }
            if (profileUpdateReceiver != null) {
                unregisterReceiver(profileUpdateReceiver);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receivers: " + e.getMessage());
        }
    }
    
    private void registerActivityResultLaunchers() {
        // Register menu activity launcher
        menuActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Apply dark mode if enabled
                    boolean isDarkMode = MenuActivity.isDarkModeEnabled(this);
                    if (isDarkMode) {
                        // Handle dark mode application if needed
                        // Note: For proper dark mode implementation, 
                        // you should use AppCompatDelegate.setDefaultNightMode() 
                        // or consider using Theme.AppCompat.DayNight theme
                    }
                }
            });
        
        // Register reminder activity launcher
        reminderActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Handle successful return from PeriodicReminderActivity
                    Toast.makeText(this, "Reminder settings updated", Toast.LENGTH_SHORT).show();
                    
                    // Apply any changes if needed
                    // For example, we might need to reload preferences or update UI
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    boolean remindersEnabled = prefs.getBoolean("reminderEnabled", false);
                    
                    // Log the change
                    Log.d(TAG, "Reminders " + (remindersEnabled ? "enabled" : "disabled"));
                }
            });
            
        // Register settings activity launcher
        settingsActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Handle successful return from SettingsActivity
                    Toast.makeText(this, "Settings updated", Toast.LENGTH_SHORT).show();
                    
                    // Apply any theme or font changes that may have happened
                    applyThemeFromPreferences();
                    applyFontSizeFromPreferences();
                    applyWallpaperFromPreferences();
                    
                    // Log that settings were updated
                    Log.d(TAG, "Settings were updated, applied changes");
                }
            });
    }

    private void initViews() {
        try {
            // Drawer layout
            drawerLayout = findViewById(R.id.drawer_layout);
            
            // Navigation views - get direct references to the inflated included layouts
            navMenuView = findViewById(R.id.nav_menu_view);
            navProfileView = findViewById(R.id.nav_profile_view);
            
            // Toolbar views
            ivMenu = findViewById(R.id.iv_menu);
            ivSearch = findViewById(R.id.iv_search);
            ivProfile = findViewById(R.id.iv_profile);
            ivNotification = findViewById(R.id.iv_notification);
            ivMessage = findViewById(R.id.iv_message);
            etSearch = findViewById(R.id.et_search);
            
            // Grid location views
            locationHome = findViewById(R.id.location_home);
            locationOffice = findViewById(R.id.location_office);
            locationWarehouse = findViewById(R.id.location_warehouse);
            locationShop = findViewById(R.id.location_shop);
            locationCar = findViewById(R.id.location_car);
            locationXyz = findViewById(R.id.location_xyz);
            
            // Floating action button
            fabAdd = findViewById(R.id.fab_add);
            
            // Grid layout
            gridLocations = findViewById(R.id.grid_locations);
            
            // Main content view for wallpaper application
            mainContentView = findViewById(R.id.main);
            toolbarLayout = findViewById(R.id.toolbar);
            
            // Initialize menu items if navMenuView is available
            Log.d(TAG, "navMenuView is " + (navMenuView == null ? "null" : "not null"));
            if (navMenuView != null) {
                // Get explicit references to the menu items in the included nav_menu layout
                menuReminder = navMenuView.findViewById(R.id.menu_reminder);
                menuSettings = navMenuView.findViewById(R.id.menu_settings);
                menuShareSamaan = navMenuView.findViewById(R.id.menu_share_samaan);
                menuLanguage = navMenuView.findViewById(R.id.menu_language);
                menuShareApp = navMenuView.findViewById(R.id.menu_share_app);
                menuRateUs = navMenuView.findViewById(R.id.menu_rate_us);
                menuReportIssues = navMenuView.findViewById(R.id.menu_report_issues);
                menuSuggestFeature = navMenuView.findViewById(R.id.menu_suggest_feature);
                menuTerms = navMenuView.findViewById(R.id.menu_terms);
                menuHelp = navMenuView.findViewById(R.id.menu_help);
                switchDarkMode = navMenuView.findViewById(R.id.switch_dark_mode);
                
                Log.d(TAG, "menuReminder is " + (menuReminder == null ? "null" : "not null"));
                Log.d(TAG, "menuSettings is " + (menuSettings == null ? "null" : "not null"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
        }
    }

    private void setupClickListeners() {
        // Menu button - open left navigation drawer
        if (ivMenu != null) {
            ivMenu.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    } else {
                        drawerLayout.openDrawer(GravityCompat.START);
                    }
                }
            });
        }

        // Search click
        if (ivSearch != null && etSearch != null) {
            ivSearch.setOnClickListener(v -> {
                String query = etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Searching for: " + query, Toast.LENGTH_SHORT).show();
                    // Implement search functionality here
                } else {
                    Toast.makeText(MainActivity.this, "Please enter search text", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Notification click
        if (ivNotification != null) {
            ivNotification.setOnClickListener(v -> 
                Toast.makeText(MainActivity.this, "Notifications clicked", Toast.LENGTH_SHORT).show());
        }
        
        // Message click
        if (ivMessage != null) {
            ivMessage.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MainActivity.this, MessageActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error opening messages: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Error opening messages", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Profile button - open right navigation drawer or ProfileActivity
        if (ivProfile != null) {
            ivProfile.setOnClickListener(v -> {
                try {
                    // Open the drawer with the profile view instead of launching ProfileActivity
                    if (drawerLayout != null && navProfileView != null) {
                        // Load user profile data to update the UI
                        loadUserProfile();
                        drawerLayout.openDrawer(GravityCompat.END);
                    } else {
                        Log.e(TAG, "Cannot open profile drawer: drawer layout or profile view is null");
                        Toast.makeText(MainActivity.this, "Error opening profile", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error opening profile drawer: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Error opening profile", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Set click listeners for location items
        if (locationHome != null) locationHome.setOnClickListener(v -> openSubLocationActivity("My Home"));
        if (locationOffice != null) locationOffice.setOnClickListener(v -> openSubLocationActivity("Office"));
        if (locationWarehouse != null) locationWarehouse.setOnClickListener(v -> openSubLocationActivity("Warehouse"));
        if (locationShop != null) locationShop.setOnClickListener(v -> openSubLocationActivity("Shop"));
        if (locationCar != null) locationCar.setOnClickListener(v -> openSubLocationActivity("Car"));
        if (locationXyz != null) locationXyz.setOnClickListener(v -> openSubLocationActivity("XYZ"));

        // Floating action button click
        if (fabAdd != null) fabAdd.setOnClickListener(v -> showAddLocationDialog());
    }
    
    private void openSubLocationActivity(Location location) {
        Intent intent = new Intent(this, SubLocationActivity.class);
        intent.putExtra(SubLocationActivity.EXTRA_LOCATION_NAME, location.getName());
        intent.putExtra(SubLocationActivity.EXTRA_LOCATION_ID, location.getId());
        startActivity(intent);
    }
    
    // Keep the existing method for backward compatibility with dynamic locations
    private void openSubLocationActivity(String locationName) {
        Location location = new Location(locationName);
        openSubLocationActivity(location);
    }
    
    private void setupMenuClickListeners() {
        Log.d(TAG, "Setting up menu click listeners");
        
        // Setup only if menu views are available
        if (navMenuView == null) {
            Log.e(TAG, "Cannot setup menu click listeners - navMenuView is null");
            return;
        }
        
        // Get fresh references to menu items to ensure we're working with the correct views
        final LinearLayout menuReminder = navMenuView.findViewById(R.id.menu_reminder);
        final LinearLayout menuSettings = navMenuView.findViewById(R.id.menu_settings);
        
        Log.d(TAG, "Fresh menuReminder reference: " + (menuReminder != null ? "found" : "null"));
        Log.d(TAG, "Fresh menuSettings reference: " + (menuSettings != null ? "found" : "null"));
        
        // Periodic Reminder menu item click listener
        if (menuReminder != null) {
            Log.d(TAG, "Setting click listener for menuReminder");
            
            // First remove any existing listeners to avoid conflicts
            menuReminder.setOnClickListener(null);
            
            // Set new click listener
            menuReminder.setOnClickListener(view -> {
                Log.d(TAG, "menuReminder clicked");
                try {
                    // Create an explicit intent with package name for clarity
                    Intent intent = new Intent();
                    intent.setClassName(getPackageName(), "com.example.whereismysamaan.PeriodicReminderActivity");
                    Log.d(TAG, "Starting PeriodicReminderActivity with intent: " + intent.toString());
                    startActivity(intent);
                    
                    // Close the drawer after clicking
                    if (drawerLayout != null) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error launching PeriodicReminderActivity: " + e.getMessage(), e);
                    Toast.makeText(MainActivity.this, "Error opening reminders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            
            // Verify the listener is attached
            Log.d(TAG, "menuReminder has OnClickListener: " + (menuReminder.hasOnClickListeners() ? "yes" : "no"));
        } else {
            Log.e(TAG, "menuReminder is null, cannot set click listener");
        }

        // Share Samaan With Friends menu item click listener
        if (menuShareSamaan != null) {
            menuShareSamaan.setOnClickListener(view -> {
                try {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                    shareIntent.putExtra(Intent.EXTRA_TEXT, 
                            "Check out my items in Where Is My Samaan app!");
                    startActivity(Intent.createChooser(shareIntent, "Share via"));
                    
                    if (drawerLayout != null) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error sharing samaan: " + e.getMessage());
                    Toast.makeText(this, "Error sharing samaan", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Dark Mode Toggle
        if (switchDarkMode != null) {
            switchDarkMode.setChecked(MenuActivity.isDarkModeEnabled(this));
            switchDarkMode.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                // Save dark mode preference
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putBoolean(PREF_DARK_MODE, isChecked);
                editor.apply();
                
                // Notify user
                Toast.makeText(MainActivity.this, 
                        "Theme will change on next app restart", 
                        Toast.LENGTH_SHORT).show();
            });
        }

        // Language menu item click listener
        if (menuLanguage != null) {
            menuLanguage.setOnClickListener(view -> {
                // Show language selection dialog
                showLanguageDialog();
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
            });
        }

        // Settings menu item click listener
        if (menuSettings != null) {
            Log.d(TAG, "Setting click listener for menuSettings");
            
            // First remove any existing listeners to avoid conflicts
            menuSettings.setOnClickListener(null);
            
            // Set new click listener
            menuSettings.setOnClickListener(view -> {
                Log.d(TAG, "menuSettings clicked");
                try {
                    // Create an explicit intent with package name for clarity
                    Intent intent = new Intent();
                    intent.setClassName(getPackageName(), "com.example.whereismysamaan.SettingsActivity");
                    Log.d(TAG, "Starting SettingsActivity with intent: " + intent.toString());
                    startActivity(intent);
                    
                    // Close the drawer after clicking
                    if (drawerLayout != null) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error launching SettingsActivity: " + e.getMessage(), e);
                    Toast.makeText(MainActivity.this, "Error opening settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            
            // Verify the listener is attached
            Log.d(TAG, "menuSettings has OnClickListener: " + (menuSettings.hasOnClickListeners() ? "yes" : "no"));
        } else {
            Log.e(TAG, "menuSettings is null, cannot set click listener");
        }

        // Share App menu item click listener
        if (menuShareApp != null) {
            menuShareApp.setOnClickListener(view -> {
                try {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                    shareIntent.putExtra(Intent.EXTRA_TEXT, 
                            "Check out Where Is My Samaan app: https://play.google.com/store/apps/details?id=" + getPackageName());
                    startActivity(Intent.createChooser(shareIntent, "Share via"));
                    
                    if (drawerLayout != null) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error sharing app: " + e.getMessage());
                    Toast.makeText(this, "Error sharing app", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Rate Us menu item click listener
        if (menuRateUs != null) {
            menuRateUs.setOnClickListener(view -> {
                try {
                    Intent rateIntent = new Intent(Intent.ACTION_VIEW);
                    rateIntent.setData(Uri.parse("market://details?id=" + getPackageName()));
                    // Fallback to Google Play website if Play Store is not available
                    if (rateIntent.resolveActivity(getPackageManager()) == null) {
                        rateIntent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName()));
                    }
                    startActivity(rateIntent);
                    
                    if (drawerLayout != null) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error opening rate page: " + e.getMessage());
                    Toast.makeText(this, "Error opening rate page", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Report Issues menu item click listener
        if (menuReportIssues != null) {
            menuReportIssues.setOnClickListener(view -> {
                try {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(Uri.parse("mailto:support@whereismysamaan.com"));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Issue Report: Where Is My Samaan");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "App version: 1.0.0" 
                            + "\nDevice: " + android.os.Build.MODEL 
                            + "\nAndroid version: " + android.os.Build.VERSION.RELEASE
                            + "\n\nIssue details: ");
                    startActivity(emailIntent);
                    
                    if (drawerLayout != null) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error reporting issue: " + e.getMessage());
                    Toast.makeText(this, "Error reporting issue", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Suggest Feature menu item click listener
        if (menuSuggestFeature != null) {
            menuSuggestFeature.setOnClickListener(view -> {
                try {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(Uri.parse("mailto:support@whereismysamaan.com"));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feature Suggestion: Where Is My Samaan");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "App version: 1.0.0" 
                            + "\n\nFeature suggestion: ");
                    startActivity(emailIntent);
                    
                    if (drawerLayout != null) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error suggesting feature: " + e.getMessage());
                    Toast.makeText(this, "Error suggesting feature", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Terms menu item click listener
        if (menuTerms != null) {
            menuTerms.setOnClickListener(view -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://whereismysamaan.com/terms"));
                    startActivity(intent);
                    
                    if (drawerLayout != null) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error opening terms: " + e.getMessage());
                    Toast.makeText(this, "Error opening terms", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Help menu item click listener
        if (menuHelp != null) {
            menuHelp.setOnClickListener(view -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://whereismysamaan.com/help"));
                    startActivity(intent);
                    
                    if (drawerLayout != null) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error opening help: " + e.getMessage());
                    Toast.makeText(this, "Error opening help", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    private void showLanguageDialog() {
        final String[] languages = {"English", "Hindi", "Bengali", "Tamil", "Telugu", "Marathi", "Gujarati"};
        final int[] selectedLanguage = {0}; // Default selection: English
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Language")
               .setSingleChoiceItems(languages, selectedLanguage[0], (dialog, which) -> {
                   selectedLanguage[0] = which;
               })
               .setPositiveButton("OK", (dialog, id) -> {
                   // Save selected language
                   SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                   editor.putString("selected_language", languages[selectedLanguage[0]]);
                   editor.apply();
                   
                   // Notify user
                   Toast.makeText(MainActivity.this, 
                           "Language set to " + languages[selectedLanguage[0]], 
                           Toast.LENGTH_SHORT).show();
                   
                   // In a real app, you would apply the language change here
                   // For example: updateLocale(languages[selectedLanguage[0]]);
               })
               .setNegativeButton("Cancel", (dialog, id) -> {
                   dialog.dismiss();
               });
        builder.create().show();
    }

    private void showAddLocationDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_location, null);
        EditText etLocationName = dialogView.findViewById(R.id.et_location_name);
        
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create();
        
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnAdd = dialogView.findViewById(R.id.btn_add);
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnAdd.setOnClickListener(v -> {
            String locationName = etLocationName.getText().toString().trim();
            if (!locationName.isEmpty()) {
                addNewLocation(locationName);
                dialog.dismiss();
            } else {
                Toast.makeText(MainActivity.this, "Please enter a location name", Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }
    
    private void addNewLocation(String locationName) {
        if (locationName.isEmpty()) {
            Toast.makeText(this, "Location name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user is logged in
        if (!firebaseHelper.isUserLoggedIn()) {
            Toast.makeText(this, "You need to be logged in to add locations", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Create new Location
        Location location = new Location(locationName);
        
        // Debug log
        Log.d(TAG, "Adding new location: " + locationName + " with ID: " + location.getId());
        
        // Add location to Firebase
        firebaseHelper.addLocation(location, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Successfully added location to Firebase: " + locationName);
                Toast.makeText(MainActivity.this, "Added new location: " + locationName, Toast.LENGTH_SHORT).show();
                
                // Try-catch to prevent any UI crashes
                try {
                    addLocationToGrid(location);
                } catch (Exception e) {
                    Log.e(TAG, "Error adding location to grid: " + e.getMessage());
                    // Refresh locations from Firebase instead
                    loadLocations();
                }
            } else {
                String errorMessage = task.getException() != null ? 
                    task.getException().getMessage() : "Failed to add location";
                Log.e(TAG, "Error adding location: " + errorMessage);
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLocations() {
        // Clear existing views
        gridLocations.removeAllViews();
        
        // Add default locations first
        addDefaultLocations();
        
        // Then load user-specific locations from Firebase
        firebaseHelper.getLocations(new FirebaseHelper.OnLocationsLoadedListener() {
            @Override
            public void onLocationsLoaded(List<Location> locations) {
                // Add each location to the grid
                for (Location location : locations) {
                    // Skip null locations or locations with null names
                    if (location == null || location.getName() == null) {
                        Log.w(TAG, "Skipping null location or location with null name");
                        continue;
                    }
                    
                    // Skip locations that match default names to avoid duplicates
                    if (!isDefaultLocationName(location.getName())) {
                        addLocationToGrid(location);
                    }
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "Error loading locations: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void addLocationToGrid(Location location) {
        // Create a new TextView
        TextView newLocation = new TextView(this);
        
        // Get the GridLayout's row and column counts
        int columns = gridLocations.getColumnCount();
        int rows = gridLocations.getRowCount();
        
        // Calculate row and column for the new location
        int childCount = gridLocations.getChildCount();
        
        // Check if we need to add a new row
        if (childCount / columns >= rows) {
            gridLocations.setRowCount(rows + 1);
            rows = gridLocations.getRowCount();
        }
        
        int row = childCount / columns;
        int column = childCount % columns;
        
        // Set layout params
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(row, 1f),
                GridLayout.spec(column, 1f)
        );
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.topMargin = (int) getResources().getDimension(R.dimen.location_item_margin_top);
        newLocation.setLayoutParams(params);
        
        // Set layout properties
        newLocation.setGravity(android.view.Gravity.CENTER);
        newLocation.setPadding(20, 20, 20, 20);
        newLocation.setText(location.getName());
        newLocation.setTextColor(getResources().getColor(R.color.black, getTheme()));
        newLocation.setTextSize(14);
        
        // Set icon based on location's iconResId or use default
        int iconResId = location.getIconResId();
        if (iconResId == 0) {
            // Default icon if not specified
            iconResId = R.drawable.ic_location;
        }
        newLocation.setCompoundDrawablesWithIntrinsicBounds(0, iconResId, 0, 0);
        newLocation.setCompoundDrawablePadding(8);
        
        // Add click listener
        newLocation.setOnClickListener(v -> openSubLocationActivity(location));
        
        // Add to grid
        gridLocations.addView(newLocation);
    }

    // Add a method to load user profile and update the UI
    private void loadUserProfile() {
        if (navProfileView == null) {
            Log.e(TAG, "navProfileView is null, cannot load profile");
            return;
        }
        
        firebaseHelper.getUserProfile(new FirebaseHelper.OnUserProfileLoadedListener() {
            @Override
            public void onUserProfileLoaded(User user) {
                if (user != null) {
                    // Find views in the nav profile
                    TextView tvProfileName = navProfileView.findViewById(R.id.tv_profile_name);
                    TextView tvProfileSubtitle = navProfileView.findViewById(R.id.tv_profile_subtitle);
                    EditText etProfileName = navProfileView.findViewById(R.id.et_profile_name);
                    EditText etProfileUsername = navProfileView.findViewById(R.id.et_profile_username);
                    EditText etProfileEmail = navProfileView.findViewById(R.id.et_profile_email);
                    EditText etProfilePhone = navProfileView.findViewById(R.id.et_profile_phone);
                    EditText etProfileAbout = navProfileView.findViewById(R.id.et_profile_about);
                    ImageView ivProfilePhoto = navProfileView.findViewById(R.id.iv_profile_photo);
                    ImageView ivEditPhoto = navProfileView.findViewById(R.id.iv_edit_photo);
                    LinearLayout logoutLayout = navProfileView.findViewById(R.id.profile_logout);
                    LinearLayout deleteAccountLayout = navProfileView.findViewById(R.id.profile_delete_account);
                    
                    // Update UI with user data
                    tvProfileName.setText(user.getName());
                    tvProfileSubtitle.setText(user.getEmail());
                    etProfileName.setText(user.getName());
                    etProfileUsername.setText(user.getUsername());
                    etProfileEmail.setText(user.getEmail());
                    
                    if (user.getPhone() != null) {
                        etProfilePhone.setText(user.getPhone());
                    }
                    
                    if (user.getAbout() != null) {
                        etProfileAbout.setText(user.getAbout());
                    }
                    
                    // Set a focus change listener to save phone when user finishes editing
                    etProfilePhone.setOnFocusChangeListener((v, hasFocus) -> {
                        if (!hasFocus) {
                            savePhoneNumber();
                        }
                    });
                    
                    // Set up edit buttons
                    Button btnEditProfile = navProfileView.findViewById(R.id.btn_edit_profile);
                    if (btnEditProfile != null) {
                        btnEditProfile.setOnClickListener(view -> enableProfileEditing(true));
                    }
                    
                    Button btnSaveProfile = navProfileView.findViewById(R.id.btn_save_profile);
                    if (btnSaveProfile != null) {
                        btnSaveProfile.setOnClickListener(view -> saveUserProfile());
                    }
                    
                    // Set up image editing
                    ivEditPhoto.setOnClickListener(view -> selectProfileImage());
                    
                    // Set up logout
                    logoutLayout.setOnClickListener(view -> showLogoutConfirmation());
                    
                    // Set up delete account
                    if (deleteAccountLayout != null) {
                        deleteAccountLayout.setOnClickListener(view -> showDeleteAccountConfirmation());
                    }
                    
                    // Load profile photo if available
                    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                        // Use Glide to load the image
                        Glide.with(MainActivity.this)
                             .load(user.getProfileImageUrl())
                             .placeholder(R.drawable.ic_profile)
                             .error(R.drawable.ic_profile)
                             .circleCrop()
                             .into(ivProfilePhoto);
                    } else {
                        // Set default profile image
                        ivProfilePhoto.setImageResource(R.drawable.ic_profile);
                    }
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e("MainActivity", "Error getting user profile: " + error);
                Toast.makeText(MainActivity.this, "Error loading profile: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Show logout confirmation dialog
    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes", (dialog, which) -> {
                logoutUser();
            })
            .setNegativeButton("No", null)
            .show();
    }

    // Show delete account confirmation dialog
    private void showDeleteAccountConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Yes", (dialog, which) -> {
                deleteAccount();
            })
            .setNegativeButton("No", null)
            .show();
    }

    // Delete user account
    private void deleteAccount() {
        try {
            // First, confirm with another dialog to prevent accidental deletion
            new AlertDialog.Builder(this)
                .setTitle("Confirm Deletion")
                .setMessage("This will permanently delete your account and all your data. Are you absolutely sure?")
                .setPositiveButton("Delete My Account", (dialog, which) -> {
                    // Call Firebase helper to delete account
                    firebaseHelper.deleteUserAccount(task -> {
                        if (task != null && task.isSuccessful()) {
                            Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                            // Navigate to login screen
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                        } else {
                            String errorMsg = task != null && task.getException() != null ? 
                                task.getException().getMessage() : "Unknown error";
                            Toast.makeText(this, "Failed to delete account: " + errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting account: " + e.getMessage());
            Toast.makeText(this, "Error deleting account", Toast.LENGTH_SHORT).show();
        }
    }

    // Add these methods for profile functionality
    private void enableProfileEditing(boolean enabled) {
        if (navProfileView == null) {
            Log.e(TAG, "navProfileView is null, cannot enable editing");
            return;
        }
        
        EditText etProfileName = navProfileView.findViewById(R.id.et_profile_name);
        EditText etProfilePhone = navProfileView.findViewById(R.id.et_profile_phone);
        EditText etProfileAbout = navProfileView.findViewById(R.id.et_profile_about);
        
        etProfileName.setEnabled(enabled);
        // Phone is always editable
        etProfilePhone.setEnabled(true);
        etProfileAbout.setEnabled(enabled);
        
        Button btnEditProfile = navProfileView.findViewById(R.id.btn_edit_profile);
        Button btnSaveProfile = navProfileView.findViewById(R.id.btn_save_profile);
        
        if (btnEditProfile != null) {
            btnEditProfile.setVisibility(enabled ? View.GONE : View.VISIBLE);
        }
        
        if (btnSaveProfile != null) {
            btnSaveProfile.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }
    }

    private void saveUserProfile() {
        if (navProfileView == null) {
            Log.e(TAG, "navProfileView is null, cannot save profile");
            return;
        }
        
        firebaseHelper.getUserProfile(new FirebaseHelper.OnUserProfileLoadedListener() {
            @Override
            public void onUserProfileLoaded(User user) {
                if (user != null) {
                    EditText etProfileName = navProfileView.findViewById(R.id.et_profile_name);
                    EditText etProfilePhone = navProfileView.findViewById(R.id.et_profile_phone);
                    EditText etProfileAbout = navProfileView.findViewById(R.id.et_profile_about);
                    
                    user.setName(etProfileName.getText().toString());
                    user.setPhone(etProfilePhone.getText().toString());
                    user.setAbout(etProfileAbout.getText().toString());
                    
                    firebaseHelper.updateUserProfile(user, task -> {
                        if (task != null && task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                            enableProfileEditing(false);
                            loadUserProfile(); // Reload to show updated data
                        } else {
                            Toast.makeText(MainActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectProfileImage() {
        // Launch image picker
        pickImage.launch("image/*");
    }

    private void logoutUser() {
        firebaseHelper.signOut();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    // Now, add a method to add default locations to the grid
    private void addDefaultLocations() {
        // Add default locations with custom logos
        addDefaultLocation("My Home", R.drawable.ic_home);
        addDefaultLocation("Office", R.drawable.ic_office);
        addDefaultLocation("Warehouse", R.drawable.ic_warehouse);
        addDefaultLocation("Shop", R.drawable.ic_shop);
        addDefaultLocation("Car", R.drawable.ic_car);
    }

    private void addDefaultLocation(String name, int iconResId) {
        // Create a location object
        Location location = new Location(name);
        
        // Create a new TextView
        TextView newLocation = new TextView(this);
        
        // Get the GridLayout's row and column counts
        int columns = gridLocations.getColumnCount();
        int rows = gridLocations.getRowCount();
        
        // Calculate row and column for the new location
        int childCount = gridLocations.getChildCount();
        
        // Check if we need to add a new row
        if (childCount / columns >= rows) {
            gridLocations.setRowCount(rows + 1);
            rows = gridLocations.getRowCount();
        }
        
        int row = childCount / columns;
        int column = childCount % columns;
        
        // Set layout params
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(row, 1f),
                GridLayout.spec(column, 1f)
        );
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.topMargin = (int) getResources().getDimension(R.dimen.location_item_margin_top);
        newLocation.setLayoutParams(params);
        
        // Set layout properties
        newLocation.setGravity(android.view.Gravity.CENTER);
        newLocation.setPadding(20, 20, 20, 20);
        newLocation.setText(name);
        newLocation.setTextColor(getResources().getColor(R.color.black, getTheme()));
        newLocation.setTextSize(14);
        
        // Set custom icon
        newLocation.setCompoundDrawablesWithIntrinsicBounds(0, iconResId, 0, 0);
        newLocation.setCompoundDrawablePadding(8);
        
        // Add click listener
        newLocation.setOnClickListener(v -> openSubLocationActivity(location));
        
        // Add to grid
        gridLocations.addView(newLocation);
    }

    private boolean isDefaultLocationName(String name) {
        if (name == null) {
            return false; // Return false for null names
        }
        return name.equals("My Home") || 
               name.equals("Office") || 
               name.equals("Warehouse") || 
               name.equals("Shop") || 
               name.equals("Car");
    }

    // Add a method to upload the profile image
    private void uploadProfileImage() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading indicator
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();
        
        // Upload the image to Firebase
        firebaseHelper.uploadProfileImage(selectedImageUri, task -> {
            if (task != null && task.isSuccessful()) {
                String imageUrl = task.getResult(); // This is now a String
                Toast.makeText(MainActivity.this, "Profile photo updated", Toast.LENGTH_SHORT).show();
                
                // Update the user profile with the new image URL
                firebaseHelper.getUserProfile(new FirebaseHelper.OnUserProfileLoadedListener() {
                    @Override
                    public void onUserProfileLoaded(User user) {
                        if (user != null) {
                            user.setProfileImageUrl(imageUrl);
                            firebaseHelper.updateUserProfile(user, updateTask -> {
                                if (updateTask != null && updateTask.isSuccessful()) {
                                    Log.d("MainActivity", "User profile updated with new image URL");
                                } else {
                                    Log.e("MainActivity", "Failed to update user profile with image URL");
                                }
                            });
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e("MainActivity", "Error getting user profile: " + error);
                        Toast.makeText(MainActivity.this, "Error loading profile: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                String errorMsg = (task != null && task.getException() != null) ? 
                    task.getException().getMessage() : "Unknown error";
                Toast.makeText(MainActivity.this, "Failed to update profile photo: " + errorMsg, Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", "Failed to update profile photo: " + errorMsg);
            }
        });
    }

    // Method to save just the phone number
    private void savePhoneNumber() {
        if (navProfileView == null) {
            Log.e(TAG, "navProfileView is null, cannot save phone number");
            return;
        }
        
        EditText etProfilePhone = navProfileView.findViewById(R.id.et_profile_phone);
        if (etProfilePhone == null) {
            return;
        }
        
        String phoneNumber = etProfilePhone.getText().toString().trim();
        
        // Get current user profile and update phone number
        firebaseHelper.getUserProfile(new FirebaseHelper.OnUserProfileLoadedListener() {
            @Override
            public void onUserProfileLoaded(User user) {
                if (user != null) {
                    // Only update if the phone number has changed
                    if (!phoneNumber.equals(user.getPhone())) {
                        user.setPhone(phoneNumber);
                        firebaseHelper.updateUserProfile(user, task -> {
                            if (task != null && task.isSuccessful()) {
                                Log.d(TAG, "Phone number updated: " + phoneNumber);
                            } else {
                                Log.e(TAG, "Failed to update phone number");
                            }
                        });
                    }
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error getting user profile to update phone: " + error);
            }
        });
    }

    private void registerSettingsBroadcastReceivers() {
        // Theme change receiver
        themeChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int themeIndex = intent.getIntExtra("theme_index", 0);
                applyTheme(themeIndex);
            }
        };
        
        // Font size change receiver
        fontSizeChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                float fontScale = intent.getFloatExtra("font_scale", 1.0f);
                applyFontSize(fontScale);
            }
        };
        
        // Wallpaper change receiver
        wallpaperChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String wallpaperType = intent.getStringExtra("wallpaper_type");
                
                if (wallpaperType != null) {
                    switch (wallpaperType) {
                        case "image":
                            String imageUriString = intent.getStringExtra("wallpaper_uri");
                            if (imageUriString != null) {
                                applyImageWallpaper(Uri.parse(imageUriString));
                            }
                            break;
                        case "color":
                            String colorValue = intent.getStringExtra("color_value");
                            if (colorValue != null) {
                                applyColorWallpaper(colorValue);
                            }
                            break;
                        case "default":
                            int resourceId = intent.getIntExtra("wallpaper_resource_id", 0);
                            if (resourceId != 0) {
                                applyDefaultWallpaper(resourceId);
                            }
                            break;
                    }
                }
            }
        };
        
        try {
            // Register receivers with their respective actions
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // For Android 13 (API 33) and above, add the RECEIVER_NOT_EXPORTED flag
                registerReceiver(themeChangeReceiver, new IntentFilter(ACTION_THEME_CHANGED), Context.RECEIVER_NOT_EXPORTED);
                registerReceiver(fontSizeChangeReceiver, new IntentFilter(ACTION_FONT_SIZE_CHANGED), Context.RECEIVER_NOT_EXPORTED);
                registerReceiver(wallpaperChangeReceiver, new IntentFilter(ACTION_WALLPAPER_CHANGED), Context.RECEIVER_NOT_EXPORTED);
            } else {
                // For older Android versions
                registerReceiver(themeChangeReceiver, new IntentFilter(ACTION_THEME_CHANGED));
                registerReceiver(fontSizeChangeReceiver, new IntentFilter(ACTION_FONT_SIZE_CHANGED));
                registerReceiver(wallpaperChangeReceiver, new IntentFilter(ACTION_WALLPAPER_CHANGED));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering broadcast receivers: " + e.getMessage());
        }
    }

    /**
     * Theme management methods
     */
    private void applyThemeFromPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int themeIndex = prefs.getInt("selected_theme", 0); // Default to Light Mode
        applyTheme(themeIndex);
    }
    
    private void applyTheme(int themeIndex) {
        // Save theme index to preferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putInt("selected_theme", themeIndex).apply();
        
        // Apply theme based on index
        switch (themeIndex) {
            case 0: // Light Mode
                setTheme(R.style.Theme_Whereismysamaan_Light);
                break;
            case 1: // Dark Mode
                setTheme(R.style.Theme_Whereismysamaan_Dark);
                break;
            case 2: // High Contrast
                setTheme(R.style.Theme_Whereismysamaan_HighContrast);
                break;
            case 3: // Orange
                setTheme(R.style.Theme_Whereismysamaan_Orange);
                break;
            case 4: // Blue
                setTheme(R.style.Theme_Whereismysamaan_Blue);
                break;
            case 5: // Green
                setTheme(R.style.Theme_Whereismysamaan_Green);
                break;
            default:
                setTheme(R.style.Theme_Whereismysamaan_Light);
                break;
        }
        
        // For immediate theme changes, recreate the activity
        // Note: For complete theme change, activity recreation is often necessary
        // This would be called in a real implementation but commented out for this example
        // recreate();
    }
    
    /**
     * Font size management methods
     */
    private void applyFontSizeFromPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        float fontScale = prefs.getFloat("font_scale", 1.0f);
        applyFontSize(fontScale);
    }
    
    @SuppressWarnings("deprecation")
    private void applyFontSize(float scale) {
        // Save font scale to preferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putFloat("font_scale", scale).apply();
        
        // Apply font scale to configuration
        // Note: This affects the entire application
        android.content.res.Configuration configuration = new android.content.res.Configuration();
        configuration.fontScale = scale;
        
        // Create a new context with the adjusted configuration
        Context context = createConfigurationContext(configuration);
        
        // Using non-deprecated method to update configuration
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            getApplicationContext().getResources().updateConfiguration(configuration, 
                getResources().getDisplayMetrics());
        } else {
            // For older Android versions, use the deprecated method
            getResources().updateConfiguration(configuration, 
                getResources().getDisplayMetrics());
        }
        
        // For immediate font size changes, recreate the activity
        // Note: For complete font change, activity recreation is often necessary
        // This would be called in a real implementation but commented out for this example
        // recreate();
    }
    
    /**
     * Wallpaper management methods
     */
    private void applyWallpaperFromPreferences() {
        if (mainContentView == null) {
            Log.e(TAG, "Main content view is null, cannot apply wallpaper");
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String wallpaperType = prefs.getString("wallpaper_type", null);
        
        if (wallpaperType != null) {
            switch (wallpaperType) {
                case "image":
                    String imageUriString = prefs.getString("wallpaper_uri", null);
                    if (imageUriString != null) {
                        applyImageWallpaper(Uri.parse(imageUriString));
                    }
                    break;
                case "color":
                    String colorValue = prefs.getString("background_color", "#FFFFFF");
                    applyColorWallpaper(colorValue);
                    break;
                case "default":
                    int resourceId = prefs.getInt("default_wallpaper_resource", 0);
                    if (resourceId != 0) {
                        applyDefaultWallpaper(resourceId);
                    }
                    break;
            }
        }
    }
    
    @SuppressWarnings("deprecation")
    private void applyImageWallpaper(Uri imageUri) {
        try {
            // Load image as a bitmap using non-deprecated method
            Bitmap bitmap;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                // For Android 9 (API 28) and above
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), imageUri);
                bitmap = ImageDecoder.decodeBitmap(source);
            } else {
                // For older Android versions, use the deprecated method
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            }
            
            Drawable wallpaperDrawable = new BitmapDrawable(getResources(), bitmap);
            
            // Apply as background to main content
            mainContentView.setBackground(wallpaperDrawable);
            
            // Save to preferences
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putString("wallpaper_uri", imageUri.toString()).apply();
            prefs.edit().putString("wallpaper_type", "image").apply();
        } catch (Exception e) {
            Log.e(TAG, "Error applying image wallpaper: " + e.getMessage(), e);
            Toast.makeText(this, "Error applying wallpaper", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void applyColorWallpaper(String colorValue) {
        try {
            // Parse color and apply as background
            int color = Color.parseColor(colorValue);
            mainContentView.setBackgroundColor(color);
            
            // Save to preferences
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putString("background_color", colorValue).apply();
            prefs.edit().putString("wallpaper_type", "color").apply();
        } catch (Exception e) {
            Log.e(TAG, "Error applying color wallpaper: " + e.getMessage(), e);
            Toast.makeText(this, "Error applying color", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void applyDefaultWallpaper(int resourceId) {
        try {
            // Load the drawable resource and apply as background
            Drawable wallpaperDrawable = getResources().getDrawable(resourceId, getTheme());
            mainContentView.setBackground(wallpaperDrawable);
            
            // Save to preferences
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putInt("default_wallpaper_resource", resourceId).apply();
            prefs.edit().putString("wallpaper_type", "default").apply();
        } catch (Exception e) {
            Log.e(TAG, "Error applying default wallpaper: " + e.getMessage(), e);
            Toast.makeText(this, "Error applying default wallpaper", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBackPressHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                try {
                    if (drawerLayout != null) {
                        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                            drawerLayout.closeDrawer(GravityCompat.START);
                        } else if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                            drawerLayout.closeDrawer(GravityCompat.END);
                        } else {
                            setEnabled(false);
                            getOnBackPressedDispatcher().onBackPressed();
                        }
                    } else {
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in back press: " + e.getMessage());
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void debugNavMenuViewHierarchy() {
        Log.d(TAG, "Debugging nav_menu_view hierarchy");
        
        View navMenuView = findViewById(R.id.nav_menu_view);
        if (navMenuView == null) {
            Log.e(TAG, "nav_menu_view is null!");
            return;
        }
        
        Log.d(TAG, "nav_menu_view found, class: " + navMenuView.getClass().getName());
        
        // Try to find menu_settings directly from the activity
        View settingsFromActivity = findViewById(R.id.menu_settings);
        Log.d(TAG, "menu_settings found directly from activity: " + (settingsFromActivity != null));
        
        // Try to find menu_reminder directly from the activity
        View reminderFromActivity = findViewById(R.id.menu_reminder);
        Log.d(TAG, "menu_reminder found directly from activity: " + (reminderFromActivity != null));
        
        // Try to find from navMenuView
        View settingsFromNavMenu = navMenuView.findViewById(R.id.menu_settings);
        Log.d(TAG, "menu_settings found from navMenuView: " + (settingsFromNavMenu != null));
        
        View reminderFromNavMenu = navMenuView.findViewById(R.id.menu_reminder);
        Log.d(TAG, "menu_reminder found from navMenuView: " + (reminderFromNavMenu != null));
        
        // Check if clickable
        if (settingsFromNavMenu != null) {
            Log.d(TAG, "menu_settings is clickable: " + settingsFromNavMenu.isClickable());
        }
        if (reminderFromNavMenu != null) {
            Log.d(TAG, "menu_reminder is clickable: " + reminderFromNavMenu.isClickable());
        }
    }

    // Add this to update the UI with user information
    private void updateProfileUI() {
        try {
            // Update profile information if the drawer profile is available
            if (navProfileView != null) {
                // Find the TextView in the profile navigation drawer
                TextView tvProfileName = navProfileView.findViewById(R.id.tv_profile_name);
                if (tvProfileName != null) {
                    String name = getUserName(this);
                    if (name != null && !name.isEmpty()) {
                        tvProfileName.setText(name);
                    }
                }
                
                // Set profile image if available
                ImageView ivProfilePhoto = navProfileView.findViewById(R.id.iv_profile_photo);
                if (ivProfilePhoto != null) {
                    Uri profileImageUri = getProfileImageUri(this);
                    if (profileImageUri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), profileImageUri);
                            ivProfilePhoto.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            Log.e(TAG, "Error loading profile image: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating profile UI: " + e.getMessage());
        }
    }

    // Static methods to replace those from ProfileActivity
    public static String getUserName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("user_profile", Context.MODE_PRIVATE);
        return prefs.getString("user_name", "");
    }
    
    public static String getUserNumber(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("user_profile", Context.MODE_PRIVATE);
        return prefs.getString("user_number", "");
    }
    
    public static String getUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("user_profile", Context.MODE_PRIVATE);
        return prefs.getString("user_id", "");
    }
    
    public static Uri getProfileImageUri(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("user_profile", Context.MODE_PRIVATE);
        String uriString = prefs.getString("profile_image_uri", null);
        if (uriString != null) {
            return Uri.parse(uriString);
        }
        return null;
    }

    private void registerProfileUpdateReceiver() {
        try {
            // Initialize the receiver
            profileUpdateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "Profile update received");
                    updateProfileUI();
                }
            };
            
            // Register the receiver
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // For Android 13 (API 33) and above, add the RECEIVER_NOT_EXPORTED flag
                registerReceiver(profileUpdateReceiver, new IntentFilter("com.example.whereismysamaan.PROFILE_UPDATED"), Context.RECEIVER_NOT_EXPORTED);
            } else {
                // For older Android versions
                registerReceiver(profileUpdateReceiver, new IntentFilter("com.example.whereismysamaan.PROFILE_UPDATED"));
            }
            Log.d(TAG, "Profile update receiver registered");
        } catch (Exception e) {
            Log.e(TAG, "Error registering profile update receiver: " + e.getMessage());
        }
    }
}