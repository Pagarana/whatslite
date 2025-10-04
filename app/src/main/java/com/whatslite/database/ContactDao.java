package com.whatslite.database;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.whatslite.model.Contact;
import java.util.List;

@Dao
public interface ContactDao {
    
    @Query("SELECT * FROM contacts ORDER BY displayName ASC")
    LiveData<List<Contact>> getAllContacts();
    
    @Query("SELECT * FROM contacts WHERE isBlocked = 0 ORDER BY displayName ASC")
    LiveData<List<Contact>> getNonBlockedContacts();
    
    @Query("SELECT * FROM contacts WHERE isBlocked = 1 ORDER BY displayName ASC")
    LiveData<List<Contact>> getBlockedContacts();
    
    @Query("SELECT * FROM contacts WHERE originalNickname = :nickname LIMIT 1")
    Contact getContactByNickname(String nickname);
    
    @Query("SELECT * FROM contacts WHERE originalNickname = :nickname LIMIT 1")
    LiveData<Contact> getContactByNicknameLive(String nickname);
    
    @Query("SELECT displayName FROM contacts WHERE originalNickname = :nickname LIMIT 1")
    String getDisplayNameByNickname(String nickname);
    
    @Query("SELECT isBlocked FROM contacts WHERE originalNickname = :nickname LIMIT 1")
    Boolean isContactBlocked(String nickname);
    
    @Query("UPDATE contacts SET displayName = :displayName WHERE originalNickname = :nickname")
    void updateDisplayName(String nickname, String displayName);
    
    @Query("UPDATE contacts SET isBlocked = :isBlocked WHERE originalNickname = :nickname")
    void updateBlockStatus(String nickname, boolean isBlocked);
    
    @Query("UPDATE contacts SET lastSeenDate = :lastSeen WHERE originalNickname = :nickname")
    void updateLastSeen(String nickname, long lastSeen);
    
    @Query("UPDATE contacts SET profileImagePath = :imagePath WHERE originalNickname = :nickname")
    void updateProfileImage(String nickname, String imagePath);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertContact(Contact contact);
    
    @Update
    void updateContact(Contact contact);
    
    @Delete
    void deleteContact(Contact contact);
    
    @Query("DELETE FROM contacts WHERE originalNickname = :nickname")
    void deleteContactByNickname(String nickname);
    
    @Query("SELECT COUNT(*) FROM contacts")
    int getContactCount();
    
    @Query("SELECT COUNT(*) FROM contacts WHERE originalNickname = :nickname")
    int isContactExists(String nickname);
}