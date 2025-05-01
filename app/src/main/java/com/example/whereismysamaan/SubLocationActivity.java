package com.example.whereismysamaan;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.whereismysamaan.firebase.FirebaseHelper;
import com.example.whereismysamaan.model.Sublocation;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class SubLocationActivity extends AppCompatActivity {
    private static final String TAG = "SubLocationActivity";
    
    // Intent extras
    public static final String EXTRA_LOCATION_NAME = "location_name";
    public static final String EXTRA_LOCATION_ID = "location_id";
    
    // UI components
    private TextView tvLocationTitle;
    private ImageView ivBack;
    private GridLayout gridSublocations;
    private FloatingActionButton fabAddSublocation;
    
    // Firebase helper
    private FirebaseHelper firebaseHelper;
    
    // Location info
    private String locationName;
    private String locationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sublocation);
        
        // Set system UI behavior
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sublocation_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Get location info from intent
        locationName = getIntent().getStringExtra(EXTRA_LOCATION_NAME);
        locationId = getIntent().getStringExtra(EXTRA_LOCATION_ID);
        if (locationName == null) {
            locationName = "Location";
        }
        
        // Initialize Firebase helper
        firebaseHelper = FirebaseHelper.getInstance();
        
        // Initialize views
        initViews();
        
        // Set click listeners
        setupClickListeners();
        
        // Load sublocations
        loadSublocations();
    }
    
    private void initViews() {
        tvLocationTitle = findViewById(R.id.tv_location_title);
        ivBack = findViewById(R.id.iv_back);
        gridSublocations = findViewById(R.id.grid_sublocations);
        fabAddSublocation = findViewById(R.id.fab_add_sublocation);
        
        // Set location name as title
        tvLocationTitle.setText(locationName);
    }
    
    private void setupClickListeners() {
        // Back button - finish activity
        ivBack.setOnClickListener(v -> finish());
        
        // FAB - show add sublocation dialog
        fabAddSublocation.setOnClickListener(v -> showAddSublocationDialog());
    }
    
    private void loadSublocations() {
        // Check if locationId is valid before trying to load
        if (locationId == null || locationId.isEmpty()) {
            Toast.makeText(this, "Error: Invalid location ID", Toast.LENGTH_SHORT).show();
            gridSublocations.removeAllViews();
            // Show an empty state or message
            TextView emptyText = new TextView(this);
            emptyText.setText("No sublocations available");
            emptyText.setGravity(android.view.Gravity.CENTER);
            emptyText.setTextSize(16);
            gridSublocations.addView(emptyText);
            return;
        }

        // Now proceed with loading using the valid locationId
        firebaseHelper.getSublocations(locationId, new FirebaseHelper.OnSublocationsLoadedListener() {
            @Override
            public void onSublocationsLoaded(List<Sublocation> sublocations) {
                // Clear existing views
                gridSublocations.removeAllViews();
                
                // Add each sublocation to the grid
                for (Sublocation sublocation : sublocations) {
                    addSublocationToGrid(sublocation);
                }
                
                // Show empty message if no sublocations
                if (sublocations.isEmpty()) {
                    TextView emptyText = new TextView(SubLocationActivity.this);
                    emptyText.setText("No sublocations yet. Add one using the + button.");
                    emptyText.setGravity(android.view.Gravity.CENTER);
                    emptyText.setTextSize(16);
                    gridSublocations.addView(emptyText);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(SubLocationActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                // Show empty state
                gridSublocations.removeAllViews();
                TextView errorText = new TextView(SubLocationActivity.this);
                errorText.setText("Error loading sublocations");
                errorText.setGravity(android.view.Gravity.CENTER);
                errorText.setTextSize(16);
                gridSublocations.addView(errorText);
            }
        });
    }
    
    private void showAddSublocationDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_sublocation, null);
        EditText etSublocationName = dialogView.findViewById(R.id.et_sublocation_name);
        
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create();
        
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnAdd = dialogView.findViewById(R.id.btn_add);
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnAdd.setOnClickListener(v -> {
            String sublocationName = etSublocationName.getText().toString().trim();
            if (!sublocationName.isEmpty()) {
                addNewSublocation(sublocationName);
                dialog.dismiss();
            } else {
                Toast.makeText(SubLocationActivity.this, "Please enter a sublocation name", Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }
    
    private void addNewSublocation(String sublocationName) {
        // Check if locationId is valid before adding a sublocation
        if (locationId == null || locationId.isEmpty()) {
            Toast.makeText(this, "Error: Cannot add sublocation - Invalid location ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new Sublocation object
        Sublocation sublocation = new Sublocation(sublocationName, locationId);
        
        // Add to Firebase
        firebaseHelper.addSublocation(sublocation, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(SubLocationActivity.this, "Added new sublocation: " + sublocationName, Toast.LENGTH_SHORT).show();
                // Add to the UI
                addSublocationToGrid(sublocation);
            } else {
                String errorMessage = task.getException() != null ? 
                    task.getException().getMessage() : "Failed to add sublocation";
                Toast.makeText(SubLocationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void addSublocationToGrid(Sublocation sublocation) {
        // Create a new TextView
        TextView newSublocation = new TextView(this);
        
        // Get the GridLayout's row and column counts
        int columns = gridSublocations.getColumnCount();
        int rows = gridSublocations.getRowCount();
        
        // Calculate row and column for the new sublocation
        int childCount = gridSublocations.getChildCount();
        
        // Check if we need to add a new row
        if (childCount / columns >= rows) {
            gridSublocations.setRowCount(rows + 1);
            rows = gridSublocations.getRowCount();
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
        newSublocation.setLayoutParams(params);
        
        // Set layout properties
        newSublocation.setGravity(android.view.Gravity.CENTER);
        newSublocation.setPadding(20, 20, 20, 20);
        newSublocation.setText(sublocation.getName());
        newSublocation.setTextColor(getResources().getColor(R.color.black, getTheme()));
        newSublocation.setTextSize(14);
        
        // Set default icon
        newSublocation.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_xyz, 0, 0);
        newSublocation.setCompoundDrawablePadding(8);
        
        // Add click listener
        newSublocation.setOnClickListener(v -> openSaamanListActivity(sublocation));
        
        // Add to grid
        gridSublocations.addView(newSublocation);
    }
    
    private void openSaamanListActivity(Sublocation sublocation) {
        Intent intent = new Intent(this, SaamanListActivity.class);
        intent.putExtra(SaamanListActivity.EXTRA_SUBLOCATION_ID, sublocation.getId());
        intent.putExtra(SaamanListActivity.EXTRA_SUBLOCATION_NAME, sublocation.getName());
        intent.putExtra(SaamanListActivity.EXTRA_LOCATION_ID, locationId);
        startActivity(intent);
    }
} 