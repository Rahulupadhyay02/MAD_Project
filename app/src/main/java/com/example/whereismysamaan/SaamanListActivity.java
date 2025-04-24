package com.example.whereismysamaan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.whereismysamaan.adapter.SaamanAdapter;
import com.example.whereismysamaan.model.Saaman;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class SaamanListActivity extends AppCompatActivity implements SaamanAdapter.OnSaamanClickListener {
    private static final String TAG = "SaamanListActivity";

    // Intent extras
    public static final String EXTRA_SUBLOCATION_ID = "sublocation_id";
    public static final String EXTRA_SUBLOCATION_NAME = "sublocation_name";
    public static final String EXTRA_LOCATION_ID = "location_id";

    // UI components
    private TextView tvSublocationTitle;
    private ImageView ivBack;
    private RecyclerView recyclerSaaman;
    private FloatingActionButton fabAddSaaman;
    private View emptyView;

    // Adapter
    private SaamanAdapter adapter;

    // Data
    private String sublocationId;
    private String sublocationName;
    private String locationId;
    private List<Saaman> saamanList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saaman_list);

        // Set system UI behavior
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.saaman_list_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get data from intent
        sublocationId = getIntent().getStringExtra(EXTRA_SUBLOCATION_ID);
        sublocationName = getIntent().getStringExtra(EXTRA_SUBLOCATION_NAME);
        locationId = getIntent().getStringExtra(EXTRA_LOCATION_ID);

        if (sublocationName == null) {
            sublocationName = "Sublocation";
        }

        // Initialize views
        initViews();

        // Set up RecyclerView
        setupRecyclerView();

        // Set click listeners
        setupClickListeners();

        // Load data
        loadSaamanItems();
    }

    private void initViews() {
        tvSublocationTitle = findViewById(R.id.tv_sublocation_title);
        ivBack = findViewById(R.id.iv_back);
        recyclerSaaman = findViewById(R.id.recycler_saaman);
        fabAddSaaman = findViewById(R.id.fab_add_saaman);
        emptyView = findViewById(R.id.empty_view);

        // Set sublocation name as title
        tvSublocationTitle.setText(sublocationName);
    }

    private void setupRecyclerView() {
        adapter = new SaamanAdapter();
        adapter.setOnSaamanClickListener(this);

        recyclerSaaman.setLayoutManager(new LinearLayoutManager(this));
        recyclerSaaman.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Back button - finish activity
        ivBack.setOnClickListener(v -> finish());

        // FAB - show add saaman dialog
        fabAddSaaman.setOnClickListener(v -> showAddSaamanDialog());
    }

    private void loadSaamanItems() {
        // For now, we'll use an in-memory list
        // In a real app, this would load from a database or API
        updateEmptyViewVisibility();
    }

    private void updateEmptyViewVisibility() {
        if (adapter.getItemCount() == 0) {
            recyclerSaaman.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerSaaman.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void showAddSaamanDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_saaman, null);
        EditText etSaamanName = dialogView.findViewById(R.id.et_saaman_name);
        EditText etSaamanDescription = dialogView.findViewById(R.id.et_saaman_description);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnAdd = dialogView.findViewById(R.id.btn_add);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String saamanName = etSaamanName.getText().toString().trim();
            String saamanDescription = etSaamanDescription.getText().toString().trim();

            if (!saamanName.isEmpty()) {
                addNewSaaman(saamanName, saamanDescription);
                dialog.dismiss();
            } else {
                Toast.makeText(SaamanListActivity.this, "Please enter a saaman name", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void addNewSaaman(String name, String description) {
        // Create a new Saaman object
        Saaman saaman = new Saaman(name, description, sublocationId);
        
        // In a real app, you would save this to a database
        // For now, we just add it to our adapter
        adapter.addSaaman(saaman);
        
        // Update UI
        updateEmptyViewVisibility();
        
        Toast.makeText(this, "Added new saaman: " + name, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSaamanClick(Saaman saaman, int position) {
        // Handle item click - for now just show a toast
        Toast.makeText(this, saaman.getName() + " selected", Toast.LENGTH_SHORT).show();
        
        // In a real app, you might open a detail view or edit dialog
    }
} 