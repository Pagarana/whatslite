package com.whatslite.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room tablosu: messages
 * DAO sorgularıyla uyumlu alanlar:
 *  - id             : @PrimaryKey (int, autoGenerate)  -> afterId / update vs. için
 *  - chatRoomId     : odadaki filtrelemeler için
 *  - text           : mesaj içeriği
 *  - senderId       : gönderen uid
 *  - timestamp      : sıralama
 *  - translatedText : çeviri sonucu (nullable)
 *  - targetLanguage : çeviri hedef dili (nullable)
 *  - isFromMe       : benim mesajım mı?
 *
 * Firebase push key'i (opsiyonel) için remoteId ekledik; Room'a dahil değil.
 * Test/Uyumluluk için: senderNickname, sender, originalText (Room dışında)
 */
@Entity(tableName = "messages")
public class Message {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String chatRoomId = "";

    @NonNull
    public String text = "";

    @NonNull
    public String senderId = "";

    public long timestamp = 0L;

    @Nullable
    public String translatedText;

    @Nullable
    public String targetLanguage;

    public boolean isFromMe = false;

    /** --- Room dışı alanlar (test/geri uyumluluk) --- */
    @Ignore @Nullable public String remoteId;        // Firebase push key
    @Ignore @Nullable public String senderNickname;  // eski test yardımcıları için
    @Ignore @Nullable public String sender;          // eski test yardımcıları için
    @Ignore @Nullable public String originalText;    // eski test yardımcıları için

    public Message() { /* Room için boş ctor */ }

    @Ignore
    public Message(@NonNull String chatRoomId,
                   @NonNull String text,
                   @NonNull String senderId,
                   long timestamp,
                   boolean isFromMe) {
        this.chatRoomId = chatRoomId;
        this.text = text;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.isFromMe = isFromMe;
    }
}
