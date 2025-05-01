package com.example.whereismysamaan.model;

public class User {
    private String id;
    private String name;
    private String username;
    private String email;
    private String phone;
    private String about;
    private String profileImageUrl;
    
    // Default constructor for Firebase
    public User() {
    }
    
    public User(String id, String email) {
        this.id = id;
        this.email = email;
        this.name = extractNameFromEmail(email);
        this.username = extractUsernameFromEmail(email);
    }
    
    private String extractNameFromEmail(String email) {
        if (email == null || email.isEmpty()) return "";
        return email.substring(0, email.indexOf('@'));
    }
    
    private String extractUsernameFromEmail(String email) {
        if (email == null || email.isEmpty()) return "";
        return email.substring(0, email.indexOf('@')).toLowerCase();
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
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getAbout() {
        return about;
    }
    
    public void setAbout(String about) {
        this.about = about;
    }
    
    public String getProfileImageUrl() {
        return profileImageUrl;
    }
    
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
} 