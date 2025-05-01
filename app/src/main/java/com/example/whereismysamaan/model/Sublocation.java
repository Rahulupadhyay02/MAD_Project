package com.example.whereismysamaan.model;

import java.util.UUID;

public class Sublocation {
    private String id;
    private String name;
    private String locationId;
    
    // Default constructor for Firebase
    public Sublocation() {
        this.id = UUID.randomUUID().toString();
    }
    
    public Sublocation(String name, String locationId) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.locationId = locationId;
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
    
    public String getLocationId() {
        return locationId;
    }
    
    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }
} 