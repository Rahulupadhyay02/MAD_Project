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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class SubLocationActivity extends AppCompatActivity {
    private static final String TAG = "SubLocationActivity";
    
    // Intent extras
    public static final String EXTRA_LOCATION_NAME = "location_name";
    
    // UI components
    private TextView tvLocationTitle;
    private ImageView ivBack;
    private GridLayout gridSublocations;
    private FloatingActionButton fabAddSublocation;
    
    // Location info
    private String locationName;

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
        
        // Get location name from intent
        locationName = getIntent().getStringExtra(EXTRA_LOCATION_NAME);
        if (locationName == null) {
            locationName = "Location";
        }
        
        // Initialize views
        initViews();
        
        // Set click listeners
        setupClickListeners();
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
                addNewSublocationToGrid(sublocationName);
                dialog.dismiss();
            } else {
                Toast.makeText(SubLocationActivity.this, "Please enter a sublocation name", Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }
    
    private void addNewSublocationToGrid(String sublocationName) {
        // Create a new TextView
        TextView newSublocation = new TextView(this);
        
        // Get the GridLayout's row and column counts
        int columns = gridSublocations.getColumnCount();
        int rows = gridSublocations.getRowCount();
        
        // Calculate row and column for the new sublocation
        int childCount = gridSublocations.getChildCount();
        
        // Check if we need to add a new row
        if (childCount / columns >= rows) {
            // We need to increase the row count before adding the view
            gridSublocations.setRowCount(rows + 1);
            rows = gridSublocations.getRowCount();
        }
        
        int row = childCount / columns;
        int column = childCount % columns;
        
        // Set layout params with specific row and column
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(row, 1f),
                GridLayout.spec(column, 1f)
        );
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.topMargin = (int) getResources().getDimension(R.dimen.location_item_margin_top);
        newSublocation.setLayoutParams(params);
        
        // Set layout properties similar to existing location items
        newSublocation.setGravity(android.view.Gravity.CENTER);
        newSublocation.setPadding(20, 20, 20, 20);
        newSublocation.setText(sublocationName);
        newSublocation.setTextColor(getResources().getColor(R.color.black, getTheme()));
        newSublocation.setTextSize(14);
        
        // Set default icon
        newSublocation.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_xyz, 0, 0);
        newSublocation.setCompoundDrawablePadding(8);
        
        // Generate a unique ID for this sublocation
        final String sublocationId = "sublocation_" + System.currentTimeMillis();
        
        // Add click listener
        newSublocation.setOnClickListener(v -> openSaamanListActivity(sublocationId, sublocationName));
        
        // Add to grid
        gridSublocations.addView(newSublocation);
        
        Toast.makeText(this, "Added new sublocation: " + sublocationName, Toast.LENGTH_SHORT).show();
    }
    
    private void openSaamanListActivity(String sublocationId, String sublocationName) {
        Intent intent = new Intent(this, SaamanListActivity.class);
        intent.putExtra(SaamanListActivity.EXTRA_SUBLOCATION_ID, sublocationId);
        intent.putExtra(SaamanListActivity.EXTRA_SUBLOCATION_NAME, sublocationName);
        intent.putExtra(SaamanListActivity.EXTRA_LOCATION_ID, locationName);
        startActivity(intent);
    }
} 