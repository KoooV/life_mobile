package com.example.life.model;

public class Message {
    private String id;
    private String text;
    private String senderId;
    private long timestamp;

    public Message() {
        // Пустой конструктор для Firebase
    }

    public Message(String id, String text, String senderId, long timestamp) {
        this.id = id;
        this.text = text;
        this.senderId = senderId;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
} 