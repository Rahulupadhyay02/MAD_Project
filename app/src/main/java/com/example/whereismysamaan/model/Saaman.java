package com.example.whereismysamaan.model;

import java.util.UUID;

public class Saaman {
    private String id;
    private String name;
    private String description;
    private String locationId;
    private String sublocationId;
    private String imageUrl;
    
    // Default constructor for Firebase or similar
    public Saaman() {
        this.id = UUID.randomUUID().toString();
    }
    
    public Saaman(String name, String description, String sublocationId) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.sublocationId = sublocationId;
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getLocationId() {
        return locationId;
    }
    
    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }
    
    public String getSublocationId() {
        return sublocationId;
    }
    
    public void setSublocationId(String sublocationId) {
        this.sublocationId = sublocationId;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
} 