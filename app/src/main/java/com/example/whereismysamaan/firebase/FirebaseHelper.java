package com.example.whereismysamaan.firebase;

import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import com.example.whereismysamaan.model.Location;
import com.example.whereismysamaan.model.Saaman;
import com.example.whereismysamaan.model.Sublocation;
import com.example.whereismysamaan.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private static FirebaseHelper instance;
    private final FirebaseAuth auth;
    private final DatabaseReference database;
    private final FirebaseStorage storage;

    // Private constructor for singleton pattern
    private FirebaseHelper() {
        auth = FirebaseAuth.getInstance();
        
        // Use the specific database URL for the Asia Southeast 1 region
        database = FirebaseDatabase.getInstance("https://madprj-42949-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                  .getReference();
        storage = FirebaseStorage.getInstance();
    }

    // Singleton instance getter
    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    // =========================
    // Authentication Methods
    // =========================
    
    public void signIn(String email, String password, OnTaskCompleteListener<AuthResult> listener) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
    }

    public void signUp(String email, String password, OnTaskCompleteListener<AuthResult> listener) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
    }

    /**
     * Signs out the current user
     */
    public void signOut() {
        Log.d(TAG, "Signing out user");
        if (auth != null) {
            auth.signOut();
            Log.d(TAG, "User signed out successfully");
        } else {
            Log.e(TAG, "Unable to sign out: auth is null");
        }
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // =========================
    // Location Methods
    // =========================
    
    public void addLocation(Location location, OnTaskCompleteListener<Void> listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "addLocation: User not logged in");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }

        Log.d(TAG, "addLocation: Adding location with ID: " + location.getId() + ", name: " + location.getName());
        
        DatabaseReference locationRef = database.child("users").child(userId).child("locations").child(location.getId());
        locationRef.setValue(location)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "addLocation: Successfully added location: " + location.getName());
                } else {
                    Log.e(TAG, "addLocation: Failed to add location", task.getException());
                }
                
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
    }

    /**
     * Get all locations for the current user, optionally excluding default locations
     *
     * @param listener Listener to handle the loaded locations
     * @param excludeDefaults Whether to exclude default locations (e.g., "My Home", "Office", etc.)
     */
    public void getLocations(OnLocationsLoadedListener listener, boolean excludeDefaults) {
        Log.d(TAG, "Getting locations for user, excludeDefaults: " + excludeDefaults);
        if (!isUserLoggedIn()) {
            Log.e(TAG, "Cannot get locations: User not logged in");
            if (listener != null) {
                listener.onError("User not logged in");
            }
            return;
        }

        // Get current user ID
        String userId = auth.getCurrentUser().getUid();
        
        // Reference to the user's locations
        database.child("users").child(userId).child("locations").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Location> locationList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Location location = snapshot.getValue(Location.class);
                    if (location != null) {
                        // Set the ID from the database key
                        location.setId(snapshot.getKey());
                        
                        // Add to the list if we're not excluding default locations or if it's not a default location
                        if (!excludeDefaults || !isDefaultLocation(location.getName())) {
                            locationList.add(location);
                        }
                    }
                }
                Log.d(TAG, "Retrieved " + locationList.size() + " locations");
                if (listener != null) {
                    listener.onLocationsLoaded(locationList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to get locations: " + databaseError.getMessage());
                if (listener != null) {
                    listener.onError(databaseError.getMessage());
                }
            }
        });
    }

    /**
     * Get all locations for the current user, including default locations
     *
     * @param listener Listener to handle the loaded locations
     */
    public void getLocations(OnLocationsLoadedListener listener) {
        getLocations(listener, false);
    }

    /**
     * Checks if a location name is one of the default locations
     *
     * @param name The location name to check
     * @return True if it's a default location name, false otherwise
     */
    private boolean isDefaultLocation(String name) {
        return name.equals("My Home") || 
               name.equals("Office") || 
               name.equals("Warehouse") || 
               name.equals("Shop") || 
               name.equals("Car");
    }

    // =========================
    // Sublocation Methods
    // =========================
    
    public void addSublocation(Sublocation sublocation, OnTaskCompleteListener<Void> listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "addSublocation: User not logged in");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }

        if (sublocation.getLocationId() == null || sublocation.getLocationId().isEmpty()) {
            Log.e(TAG, "addSublocation: Invalid location ID");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }

        Log.d(TAG, "addSublocation: Adding sublocation with ID: " + sublocation.getId() + 
               ", name: " + sublocation.getName() + 
               ", to locationId: " + sublocation.getLocationId());
        
        DatabaseReference sublocationRef = database.child("users").child(userId)
            .child("locations").child(sublocation.getLocationId())
            .child("sublocations").child(sublocation.getId());
            
        sublocationRef.setValue(sublocation)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "addSublocation: Successfully added sublocation: " + sublocation.getName());
                } else {
                    Log.e(TAG, "addSublocation: Failed to add sublocation", task.getException());
                }
                
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
    }

    public void getSublocations(String locationId, OnSublocationsLoadedListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (listener != null) {
                listener.onError("User not logged in");
            }
            return;
        }

        DatabaseReference sublocationsRef = database.child("users").child(userId)
            .child("locations").child(locationId)
            .child("sublocations");
            
        sublocationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Sublocation> sublocations = new ArrayList<>();
                for (DataSnapshot sublocationSnapshot : snapshot.getChildren()) {
                    Sublocation sublocation = sublocationSnapshot.getValue(Sublocation.class);
                    if (sublocation != null) {
                        sublocations.add(sublocation);
                    }
                }
                if (listener != null) {
                    listener.onSublocationsLoaded(sublocations);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (listener != null) {
                    listener.onError(error.getMessage());
                }
            }
        });
    }

    // =========================
    // Saaman Methods
    // =========================
    
    public void addSaaman(Saaman saaman, OnTaskCompleteListener<Void> listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }

        DatabaseReference saamanRef = database.child("users").child(userId)
            .child("locations").child(saaman.getLocationId())
            .child("sublocations").child(saaman.getSublocationId())
            .child("saaman").child(saaman.getId());
            
        saamanRef.setValue(saaman)
            .addOnCompleteListener(task -> {
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
    }

    public void getSaamanItems(String locationId, String sublocationId, OnSaamanLoadedListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (listener != null) {
                listener.onError("User not logged in");
            }
            return;
        }

        DatabaseReference saamanRef = database.child("users").child(userId)
            .child("locations").child(locationId)
            .child("sublocations").child(sublocationId)
            .child("saaman");
            
        saamanRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Saaman> saamanList = new ArrayList<>();
                for (DataSnapshot saamanSnapshot : snapshot.getChildren()) {
                    Saaman saaman = saamanSnapshot.getValue(Saaman.class);
                    if (saaman != null) {
                        saamanList.add(saaman);
                    }
                }
                if (listener != null) {
                    listener.onSaamanLoaded(saamanList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (listener != null) {
                    listener.onError(error.getMessage());
                }
            }
        });
    }

    // =========================
    // User Profile Methods
    // =========================
    
    public void createUserProfile(User user, OnTaskCompleteListener<Void> listener) {
        if (user == null || user.getId() == null) {
            Log.e(TAG, "createUserProfile: User or User ID is null");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }
        
        Log.d(TAG, "createUserProfile: Creating user profile for: " + user.getEmail());
        
        DatabaseReference userRef = database.child("users").child(user.getId()).child("profile");
        userRef.setValue(user)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "createUserProfile: Successfully created profile for: " + user.getEmail());
                } else {
                    Log.e(TAG, "createUserProfile: Failed to create profile", task.getException());
                }
                
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
    }

    public void getUserProfile(OnUserProfileLoadedListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "getUserProfile: User not logged in");
            if (listener != null) {
                listener.onError("User not logged in");
            }
            return;
        }
        
        Log.d(TAG, "getUserProfile: Getting profile for user ID: " + userId);
        
        DatabaseReference userRef = database.child("users").child(userId).child("profile");
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user == null) {
                    // Create a new user profile if not exists
                    FirebaseUser firebaseUser = getCurrentUser();
                    user = new User(userId, firebaseUser.getEmail());
                    createUserProfile(user, null);
                }
                
                if (listener != null) {
                    listener.onUserProfileLoaded(user);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "getUserProfile: Error loading profile", error.toException());
                if (listener != null) {
                    listener.onError(error.getMessage());
                }
            }
        });
    }
    
    public void updateUserProfile(User user, OnTaskCompleteListener<Void> listener) {
        if (user == null || user.getId() == null) {
            Log.e(TAG, "updateUserProfile: User or User ID is null");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }
        
        String userId = getCurrentUserId();
        if (userId == null || !userId.equals(user.getId())) {
            Log.e(TAG, "updateUserProfile: User not logged in or user ID mismatch");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }
        
        Log.d(TAG, "updateUserProfile: Updating profile for: " + user.getEmail());
        
        DatabaseReference userRef = database.child("users").child(userId).child("profile");
        userRef.setValue(user)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "updateUserProfile: Successfully updated profile for: " + user.getEmail());
                } else {
                    Log.e(TAG, "updateUserProfile: Failed to update profile", task.getException());
                }
                
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
    }
    
    public void uploadProfileImage(Uri imageUri, OnTaskCompleteListener<String> listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "uploadProfileImage: User not logged in");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }
        
        if (imageUri == null) {
            Log.e(TAG, "uploadProfileImage: Image URI is null");
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }
        
        Log.d(TAG, "uploadProfileImage: Uploading profile image for user ID: " + userId);
        
        StorageReference storageRef = storage.getReference().child("profile_images").child(userId + ".jpg");
        UploadTask uploadTask = storageRef.putFile(imageUri);
        
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            
            // Continue with the task to get the download URL
            return storageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                String downloadUrl = downloadUri.toString();
                
                // Create a wrapper Task with the String URL
                Task<String> stringTask = Tasks.forResult(downloadUrl);
                
                // Update user profile with image URL
                getUserProfile(new OnUserProfileLoadedListener() {
                    @Override
                    public void onUserProfileLoaded(User user) {
                        if (user != null) {
                            user.setProfileImageUrl(downloadUrl);
                            updateUserProfile(user, null);
                        }
                        
                        if (listener != null) {
                            listener.onComplete(stringTask);
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "uploadProfileImage: Error getting user profile: " + error);
                        if (listener != null) {
                            listener.onComplete(null);
                        }
                    }
                });
            } else {
                Log.e(TAG, "uploadProfileImage: Failed to get download URL", task.getException());
                if (listener != null) {
                    listener.onComplete(null);
                }
            }
        });
    }

    // =========================
    // Interfaces
    // =========================
    
    public interface OnTaskCompleteListener<T> {
        void onComplete(Task<T> task);
    }

    public interface OnLocationsLoadedListener {
        void onLocationsLoaded(List<Location> locations);
        void onError(String error);
    }

    public interface OnSublocationsLoadedListener {
        void onSublocationsLoaded(List<Sublocation> sublocations);
        void onError(String error);
    }

    public interface OnSaamanLoadedListener {
        void onSaamanLoaded(List<Saaman> saamanList);
        void onError(String error);
    }

    public interface OnUserProfileLoadedListener {
        void onUserProfileLoaded(User user);
        void onError(String error);
    }
} 