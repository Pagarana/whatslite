package com.whatslite.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey
    @NonNull
    public String nickname;
    
    public String selectedLanguage;
    public boolean isOnline;
    public long lastSeen;
    
    @Ignore
    public User(String nickname, String selectedLanguage) {
        this.nickname = nickname;
        this.selectedLanguage = selectedLanguage;
        this.isOnline = false;
        this.lastSeen = System.currentTimeMillis();
    }
    
    public User() {} // Room requires empty constructor
}
