package com.example.whereismysamaan.model;

import java.util.Date;
import java.util.UUID;

public class Message {
    private String id;
    private String senderId;
    private String senderName;
    private String receiverId;
    private String title;
    private String content;
    private String saamanId;
    private String saamanName;
    private String saamanImageUrl;
    private String locationName;
    private String sublocationName;
    private long timestamp;
    private boolean isRead;
    
    // Default constructor for Firebase
    public Message() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = new Date().getTime();
        this.isRead = false;
    }
    
    // Constructor for sharing Samaan
    public Message(String senderId, String senderName, String receiverId, 
                   String saamanId, String saamanName, String saamanImageUrl,
                   String locationName, String sublocationName) {
        this.id = UUID.randomUUID().toString();
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.saamanId = saamanId;
        this.saamanName = saamanName;
        this.saamanImageUrl = saamanImageUrl;
        this.locationName = locationName;
        this.sublocationName = sublocationName;
        this.title = "Shared Item: " + saamanName;
        this.content = "Location: " + locationName + " > " + sublocationName;
        this.timestamp = new Date().getTime();
        this.isRead = false;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getSaamanId() {
        return saamanId;
    }
    
    public void setSaamanId(String saamanId) {
        this.saamanId = saamanId;
    }
    
    public String getSaamanName() {
        return saamanName;
    }
    
    public void setSaamanName(String saamanName) {
        this.saamanName = saamanName;
    }
    
    public String getSaamanImageUrl() {
        return saamanImageUrl;
    }
    
    public void setSaamanImageUrl(String saamanImageUrl) {
        this.saamanImageUrl = saamanImageUrl;
    }
    
    public String getLocationName() {
        return locationName;
    }
    
    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
    
    public String getSublocationName() {
        return sublocationName;
    }
    
    public void setSublocationName(String sublocationName) {
        this.sublocationName = sublocationName;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isRead() {
        return isRead;
    }
    
    public void setRead(boolean read) {
        isRead = read;
    }
} 