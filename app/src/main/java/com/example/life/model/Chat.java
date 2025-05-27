package com.example.life.model;

public class Chat {
    private String id;
    private String otherUserId;
    private String lastMessage;
    private long lastMessageTimestamp;

    public Chat() {
        // Пустой конструктор для Firebase
    }

    public Chat(String id, String otherUserId, String lastMessage, long lastMessageTimestamp) {
        this.id = id;
        this.otherUserId = otherUserId;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }
} 