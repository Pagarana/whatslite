package com.whatslite.database;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.whatslite.model.User;
import java.util.List;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(User user);
    
    @Query("SELECT * FROM users WHERE nickname = :nickname")
    User getUserByNickname(String nickname);
    
    @Query("SELECT * FROM users WHERE isOnline = 1")
    LiveData<List<User>> getOnlineUsers();
    
    @Query("UPDATE users SET isOnline = :isOnline, lastSeen = :lastSeen WHERE nickname = :nickname")
    void updateUserStatus(String nickname, boolean isOnline, long lastSeen);
    
    @Query("SELECT COUNT(*) FROM users WHERE nickname = :nickname")
    int isNicknameExists(String nickname);
}
