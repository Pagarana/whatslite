package com.whatslite.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class Message {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String chatRoomId;
    public String senderNickname;
    public String originalText;
    public String translatedText;
    public String originalLanguage;
    public String targetLanguage;
    public long timestamp;
    public boolean isFromMe;
    public boolean isTranslated;
    
    public Message() {}
    
    @Ignore
    public Message(String chatRoomId, String senderNickname, String originalText, 
                   String originalLanguage, boolean isFromMe) {
        this.chatRoomId = chatRoomId;
        this.senderNickname = senderNickname;
        this.originalText = originalText;
        this.originalLanguage = originalLanguage;
        this.isFromMe = isFromMe;
        this.timestamp = System.currentTimeMillis();
        this.isTranslated = false;
    }
}
