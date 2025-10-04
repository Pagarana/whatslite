package com.whatslite.database;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.whatslite.model.Message;
import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    long insertMessage(Message message); // Return the row ID
    
    @Query("SELECT * FROM messages WHERE chatRoomId = :chatRoomId ORDER BY timestamp ASC")
    LiveData<List<Message>> getMessagesForChatRoom(String chatRoomId);
    
    @Query("UPDATE messages SET translatedText = :translatedText, targetLanguage = :targetLanguage, isTranslated = 1 WHERE id = :messageId")
    void updateMessageTranslation(int messageId, String translatedText, String targetLanguage);
    
    @Query("SELECT * FROM messages WHERE chatRoomId = :chatRoomId ORDER BY timestamp DESC LIMIT 1")
    Message getLastMessageForChatRoom(String chatRoomId);
    
    @Delete
    void deleteMessage(Message message);
    
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    List<Message> getAllMessages();
    
    @Query("SELECT * FROM messages WHERE chatRoomId = :chatRoomId ORDER BY timestamp ASC")
    List<Message> getMessagesForChatRoomSync(String chatRoomId);
    
    @Query("SELECT * FROM messages WHERE chatRoomId = :chatRoomId AND id > :afterId ORDER BY timestamp ASC")
    List<Message> getMessagesAfterId(String chatRoomId, int afterId);
}
