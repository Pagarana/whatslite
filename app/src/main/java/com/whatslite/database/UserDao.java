package com.whatslite.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.whatslite.model.User;

import java.util.List;

@Dao
public interface UserDao {

    // --- Ekleme / Güncelleme ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(User user);

    @Update
    void updateUser(User user);

    // --- Tekil sorgular ---
    @Query("SELECT * FROM users WHERE TRIM(nickname) = TRIM(:nickname) LIMIT 1")
    User getUserByNickname(String nickname);

    @Query("SELECT COUNT(*) FROM users WHERE TRIM(nickname) = TRIM(:nickname)")
    int isNicknameExists(String nickname);  // 0 = yok, 1+ = var

    // --- Online kullanıcılar ---
    @Query("SELECT * FROM users WHERE isOnline = 1")
    LiveData<List<User>> getOnlineUsers();

    // --- Durum güncellemesi ---
    @Query("UPDATE users SET isOnline = :isOnline, lastSeen = :lastSeen WHERE TRIM(nickname) = TRIM(:nickname)")
    void updateUserStatus(String nickname, boolean isOnline, long lastSeen);

    // --- Tüm kullanıcılar (isteğe bağlı sync liste) ---
    @Query("SELECT * FROM users ORDER BY nickname COLLATE NOCASE ASC")
    List<User> getAllUsersSync();
}
