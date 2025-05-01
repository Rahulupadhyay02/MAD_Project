package com.example.whereismysamaan.model;

import java.util.UUID;

public class Location {
    private String id;
    private String name;
    private int iconResId;
    
    // Default constructor for Firebase
    public Location() {
        this.id = UUID.randomUUID().toString();
    }
    
    public Location(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getIconResId() {
        return iconResId;
    }
    
    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }
} 