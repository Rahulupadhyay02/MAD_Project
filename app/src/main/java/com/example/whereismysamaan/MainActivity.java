package com.example.whereismysamaan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
    private ActivityResultLauncher<Intent> profileActivityLauncher;
    private ActivityResultLauncher<Intent> reminderActivityLauncher;
    private ActivityResultLauncher<Intent> settingsActivityLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
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

        // Set click listeners
        setupClickListeners();
        
        // Set menu item click listeners
        setupMenuClickListeners();
        
        // Setup back press handling
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
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
        
        // Register profile activity launcher
        profileActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Handle any actions needed after returning from ProfileActivity
                }
            });
            
        // Register reminder activity launcher
        reminderActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Handle any actions needed after returning from PeriodicReminderActivity
                    Toast.makeText(this, "Reminder settings updated", Toast.LENGTH_SHORT).show();
                }
            });
            
        // Register settings activity launcher
        settingsActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Handle any actions needed after returning from SettingsActivity
                    Toast.makeText(this, "Settings updated", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void initViews() {
        // Drawer layout
        drawerLayout = findViewById(R.id.drawer_layout);
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
        
        // Initialize menu items if navMenuView is available
        if (navMenuView != null) {
            // Menu items - accessed through the included layout
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
            
            Log.d(TAG, "Menu items initialization attempt completed");
            
            // Debug logging for menu items
            Log.d(TAG, "menuReminder: " + (menuReminder != null ? "found" : "null"));
            Log.d(TAG, "menuSettings: " + (menuSettings != null ? "found" : "null"));
            Log.d(TAG, "switchDarkMode: " + (switchDarkMode != null ? "found" : "null"));
        } else {
            Log.e(TAG, "navMenuView is null, cannot initialize menu items");
        }
    }

    private void setupClickListeners() {
        // Menu button - open left navigation drawer
        ivMenu.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Search click
        ivSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                Toast.makeText(MainActivity.this, "Searching for: " + query, Toast.LENGTH_SHORT).show();
                // Implement search functionality here
            } else {
                Toast.makeText(MainActivity.this, "Please enter search text", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Notification click
        ivNotification.setOnClickListener(v -> 
            Toast.makeText(MainActivity.this, "Notifications clicked", Toast.LENGTH_SHORT).show());
        
        // Message click
        ivMessage.setOnClickListener(v -> 
            Toast.makeText(MainActivity.this, "Messages clicked", Toast.LENGTH_SHORT).show());

        // Profile button - open right navigation drawer
        ivProfile.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                // Make sure the profile view is visible before opening
                if (navProfileView.getVisibility() == View.GONE) {
                    navProfileView.setVisibility(View.VISIBLE);
                }
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        // Set click listeners for location items
        locationHome.setOnClickListener(v -> 
            Toast.makeText(MainActivity.this, "My Home selected", Toast.LENGTH_SHORT).show());
        
        locationOffice.setOnClickListener(v -> 
            Toast.makeText(MainActivity.this, "Office selected", Toast.LENGTH_SHORT).show());
        
        locationWarehouse.setOnClickListener(v -> 
            Toast.makeText(MainActivity.this, "Warehouse selected", Toast.LENGTH_SHORT).show());
        
        locationShop.setOnClickListener(v -> 
            Toast.makeText(MainActivity.this, "Shop selected", Toast.LENGTH_SHORT).show());
        
        locationCar.setOnClickListener(v -> 
            Toast.makeText(MainActivity.this, "Car selected", Toast.LENGTH_SHORT).show());
        
        locationXyz.setOnClickListener(v -> 
            Toast.makeText(MainActivity.this, "XYZ selected", Toast.LENGTH_SHORT).show());

        // Floating action button click
        fabAdd.setOnClickListener(v -> 
            Toast.makeText(MainActivity.this, "Add new location", Toast.LENGTH_SHORT).show());
    }
    
    private void setupMenuClickListeners() {
        Log.d(TAG, "Setting up menu click listeners");
        
        // Setup only if menu views are available
        if (navMenuView == null) {
            Log.e(TAG, "Cannot setup menu click listeners - navMenuView is null");
            return;
        }
        
        // Reminder menu item click listener
        if (menuReminder != null) {
            menuReminder.setOnClickListener(view -> {
                Log.d(TAG, "menuReminder clicked");
                try {
                    reminderActivityLauncher.launch(new Intent(MainActivity.this, PeriodicReminderActivity.class));
                    drawerLayout.closeDrawer(GravityCompat.START);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching PeriodicReminderActivity: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Error opening reminders", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.e(TAG, "menuReminder is null, cannot set click listener");
        }

        // Settings menu item click listener
        if (menuSettings != null) {
            menuSettings.setOnClickListener(view -> {
                Log.d(TAG, "menuSettings clicked");
                try {
                    Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                    // Using the registered launcher instead of direct startActivity to handle result
                    settingsActivityLauncher.launch(settingsIntent);
                    drawerLayout.closeDrawer(GravityCompat.START);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching SettingsActivity: " + e.getMessage(), e);
                    Toast.makeText(MainActivity.this, "Error opening settings", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.e(TAG, "menuSettings is null, cannot set click listener");
        }

        // Setup dark mode switch if available
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
        } else {
            Log.e(TAG, "switchDarkMode is null, cannot set listener");
        }
    }
}