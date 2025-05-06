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
    private boolean isImageBase64;
    
    // Default constructor for Firebase
    public Message() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = new Date().getTime();
        this.isRead = false;
        this.isImageBase64 = false;
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
        this.isImageBase64 = false;
    }
    
    /**
     * Copy constructor to create a deep copy of a message
     * @param original The original message to copy
     */
    public Message(Message original) {
        if (original != null) {
            this.id = original.id;
            this.senderId = original.senderId;
            this.senderName = original.senderName;
            this.receiverId = original.receiverId;
            this.title = original.title;
            this.content = original.content;
            this.saamanId = original.saamanId;
            this.saamanName = original.saamanName;
            this.saamanImageUrl = original.saamanImageUrl;
            this.locationName = original.locationName;
            this.sublocationName = original.sublocationName;
            this.timestamp = original.timestamp;
            this.isRead = original.isRead;
            this.isImageBase64 = original.isImageBase64;
        }
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
    
    public boolean isImageBase64() {
        return isImageBase64;
    }
    
    public void setIsImageBase64(boolean isImageBase64) {
        this.isImageBase64 = isImageBase64;
    }
} 