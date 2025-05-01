package com.example.whereismysamaan.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.whereismysamaan.R;
import com.example.whereismysamaan.firebase.FirebaseHelper;
import com.example.whereismysamaan.model.Location;

/**
 * Utility class for handling app initialization tasks.
 */
public class AppInitializer {
    private static final String TAG = "AppInitializer";
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_APP_INITIALIZED = "appInitialized";

    /**
     * Initialize the app with default locations if needed.
     *
     * @param context Context to access SharedPreferences
     * @param firebaseHelper FirebaseHelper instance for database operations
     */
    public static void initializeAppIfNeeded(Context context, FirebaseHelper firebaseHelper) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        if (!prefs.getBoolean(KEY_APP_INITIALIZED, false)) {
            Log.d(TAG, "App not initialized, adding default locations to Firebase");
            
            // Add default locations to Firebase
            addDefaultLocationsToFirebase(firebaseHelper);
            
            // Mark app as initialized
            prefs.edit().putBoolean(KEY_APP_INITIALIZED, true).apply();
            Log.d(TAG, "App marked as initialized");
        } else {
            Log.d(TAG, "App already initialized, skipping default locations setup");
        }
    }

    /**
     * Add default locations to Firebase database.
     *
     * @param firebaseHelper FirebaseHelper instance for database operations
     */
    private static void addDefaultLocationsToFirebase(FirebaseHelper firebaseHelper) {
        // Create and add default locations
        String[] defaultLocationNames = {
                "My Home",
                "Office",
                "Warehouse",
                "Shop",
                "Car"
        };
        
        int[] defaultLocationIcons = {
                R.drawable.ic_home,
                R.drawable.ic_office,
                R.drawable.ic_warehouse,
                R.drawable.ic_shop,
                R.drawable.ic_car
        };
        
        for (int i = 0; i < defaultLocationNames.length; i++) {
            final String locationName = defaultLocationNames[i];
            final int iconResId = defaultLocationIcons[i];
            
            // Create location object
            Location location = new Location(locationName);
            location.setIconResId(iconResId); // You may need to add this field to your Location class
            
            // Add location to Firebase
            firebaseHelper.addLocation(location, task -> {
                if (task != null && task.isSuccessful()) {
                    Log.d(TAG, "Added default location: " + locationName);
                } else {
                    Log.e(TAG, "Failed to add default location: " + locationName + 
                          (task != null && task.getException() != null ? 
                          " - " + task.getException().getMessage() : ""));
                }
            });
        }
    }
} 