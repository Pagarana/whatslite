package com.whatslite.database;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.whatslite.model.ChatRoom;
import java.util.List;

@Dao
public interface ChatRoomDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChatRoom(ChatRoom chatRoom);

    @Query("SELECT * FROM chat_rooms WHERE participant1 = :nickname OR participant2 = :nickname ORDER BY lastMessageTime DESC")
    LiveData<List<ChatRoom>> getChatRoomsForUser(String nickname);

    @Query("SELECT * FROM chat_rooms WHERE chatRoomId = :chatRoomId")
    ChatRoom getChatRoomById(String chatRoomId);

    @Query("UPDATE chat_rooms SET lastMessage = :lastMessage, lastMessageTime = :lastMessageTime WHERE chatRoomId = :chatRoomId")
    void updateLastMessage(String chatRoomId, String lastMessage, long lastMessageTime);

    @Query("SELECT * FROM chat_rooms WHERE (participant1 = :user1 AND participant2 = :user2) OR (participant1 = :user2 AND participant2 = :user1)")
    ChatRoom findChatRoomBetweenUsers(String user1, String user2);

    @Delete
    void deleteChatRoom(ChatRoom chatRoom);
}
