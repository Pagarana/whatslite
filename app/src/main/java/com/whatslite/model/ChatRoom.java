package com.whatslite.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_rooms")
public class ChatRoom {
    @PrimaryKey @NonNull
    public String chatRoomId;

    public String participant1;
    public String participant2;
    public String lastMessage;
    public long lastMessageTime;
    public boolean isActive;

    public ChatRoom() {}

    @Ignore
    public ChatRoom(String participant1, String participant2) {
        this.participant1 = participant1;
        this.participant2 = participant2;
        this.chatRoomId = generateChatRoomId(participant1, participant2);
        this.lastMessage = "";
        this.lastMessageTime = System.currentTimeMillis();
        this.isActive = true;
    }

    private String generateChatRoomId(String a, String b) {
        return a.compareToIgnoreCase(b) <= 0 ? a + "__" + b : b + "__" + a;
    }

    public String getOtherParticipant(String me) {
        return me.equals(participant1) ? participant2 : participant1;
    }
}
