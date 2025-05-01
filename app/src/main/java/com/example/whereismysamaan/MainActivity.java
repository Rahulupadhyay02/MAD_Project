package com.example.whereismysamaan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import android.app.Dialog;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.GridLayout;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
    private ActivityResultLauncher<Intent> profileActivityLauncher;
    private ActivityResultLauncher<Intent> reminderActivityLauncher;
    private ActivityResultLauncher<Intent> settingsActivityLauncher;

    private GridLayout gridLocations;

    // Firebase helper
    private FirebaseHelper firebaseHelper;

    // New properties
    private Uri selectedImageUri;
    private ActivityResultLauncher<String> pickImage;

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

        // Grid layout
        gridLocations = findViewById(R.id.grid_locations);
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
                // Load user profile data
                loadUserProfile();
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        // Set click listeners for location items
        locationHome.setOnClickListener(v -> {
            // Create a Location with a valid ID for My Home
            Location homeLocation = new Location("My Home");
            openSubLocationActivity(homeLocation);
        });
        locationOffice.setOnClickListener(v -> {
            // Create a Location with a valid ID for Office
            Location officeLocation = new Location("Office");
            openSubLocationActivity(officeLocation);
        });
        locationWarehouse.setOnClickListener(v -> {
            // Create a Location with a valid ID for Warehouse
            Location warehouseLocation = new Location("Warehouse");
            openSubLocationActivity(warehouseLocation);
        });
        locationShop.setOnClickListener(v -> {
            // Create a Location with a valid ID for Shop
            Location shopLocation = new Location("Shop");
            openSubLocationActivity(shopLocation);
        });
        locationCar.setOnClickListener(v -> {
            // Create a Location with a valid ID for Car
            Location carLocation = new Location("Car");
            openSubLocationActivity(carLocation);
        });
        locationXyz.setOnClickListener(v -> {
            // Create a Location with a valid ID for XYZ
            Location xyzLocation = new Location("XYZ");
            openSubLocationActivity(xyzLocation);
        });

        // Floating action button click
        fabAdd.setOnClickListener(v -> showAddLocationDialog());
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
                    logoutLayout.setOnClickListener(view -> logoutUser());
                    
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
        etProfilePhone.setEnabled(enabled);
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
}