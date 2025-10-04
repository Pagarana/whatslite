package com.whatslite.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "contacts")
public class Contact {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String originalNickname;  // Firebase'deki gerçek nickname
    public String displayName;       // Kullanıcının verdiği özel isim
    public String language;          // Kişinin dili
    public String profileImagePath;  // Profil resmi yolu (opsiyonel)
    public boolean isBlocked;        // Engellenmiş mi?
    public long addedDate;          // Eklenme tarihi
    public long lastSeenDate;       // Son görülme tarihi
    
    // Room için boş constructor gerekli
    public Contact() {}
    
    @Ignore
    public Contact(String originalNickname, String displayName, String language) {
        this.originalNickname = originalNickname;
        this.displayName = displayName;
        this.language = language;
        this.isBlocked = false;
        this.addedDate = System.currentTimeMillis();
        this.lastSeenDate = System.currentTimeMillis();
    }
    
    public String getDisplayNameOrOriginal() {
        return (displayName != null && !displayName.trim().isEmpty()) ? displayName : originalNickname;
    }
}