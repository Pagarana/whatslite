package com.whatslite.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.whatslite.model.Contact;

import java.util.List;

@Dao
public interface ContactDao {

    // --- Listelemeler (displayName boşsa originalNickname ile, case-insensitive sıralama) ---
    @Query("SELECT * FROM contacts " +
           "ORDER BY COALESCE(NULLIF(TRIM(displayName), ''), originalNickname) COLLATE NOCASE ASC")
    LiveData<List<Contact>> getAllContacts();

    @Query("SELECT * FROM contacts " +
           "ORDER BY COALESCE(NULLIF(TRIM(displayName), ''), originalNickname) COLLATE NOCASE ASC")
    List<Contact> getAllContactsSync();

    @Query("SELECT * FROM contacts WHERE isBlocked = 0 " +
           "ORDER BY COALESCE(NULLIF(TRIM(displayName), ''), originalNickname) COLLATE NOCASE ASC")
    LiveData<List<Contact>> getNonBlockedContacts();

    @Query("SELECT * FROM contacts WHERE isBlocked = 1 " +
           "ORDER BY COALESCE(NULLIF(TRIM(displayName), ''), originalNickname) COLLATE NOCASE ASC")
    LiveData<List<Contact>> getBlockedContacts();

    // --- Tekil erişimler ---
    @Query("SELECT * FROM contacts WHERE originalNickname = :nickname LIMIT 1")
    Contact getContactByNickname(String nickname);

    @Query("SELECT * FROM contacts WHERE originalNickname = :nickname LIMIT 1")
    LiveData<Contact> getContactByNicknameLive(String nickname);

    @Query("SELECT COALESCE(NULLIF(TRIM(displayName), ''), originalNickname) " +
           "FROM contacts WHERE originalNickname = :nickname LIMIT 1")
    String getDisplayNameByNickname(String nickname);

    @Query("SELECT isBlocked FROM contacts WHERE originalNickname = :nickname LIMIT 1")
    int isContactBlocked(String nickname); // 1=blocked, 0=not blocked

    // --- Alan güncellemeleri ---
    // Case-insensitive bloklama güncellemesi (ContactsActivity uzun basış menüsü için)
    @Query("UPDATE contacts SET isBlocked = :isBlocked " +
           "WHERE LOWER(TRIM(originalNickname)) = LOWER(TRIM(:nickname))")
    void updateBlockStatus(String nickname, boolean isBlocked);

    // --- Ekleme / Güncelleme / Silme ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertContact(Contact contact); // upsert benzeri

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertContacts(List<Contact> contacts);

    @Update
    void updateContact(Contact contact);

    @Delete
    void deleteContact(Contact contact);

    @Query("DELETE FROM contacts WHERE LOWER(TRIM(originalNickname)) = LOWER(TRIM(:nickname))")
    void deleteContactByNickname(String nickname);

    // --- Sayaçlar / varlık kontrolü ---
    @Query("SELECT COUNT(*) FROM contacts")
    int getContactCount();

    // Case-insensitive varlık kontrolü (trim + lower)
    @Query("SELECT COUNT(*) FROM contacts " +
           "WHERE LOWER(TRIM(originalNickname)) = LOWER(TRIM(:nickname))")
    int isContactExistsCI(String nickname);

    // ContactsActivity ile uyum için alias (CI kontrol)
    @Query("SELECT COUNT(*) FROM contacts " +
           "WHERE LOWER(TRIM(originalNickname)) = LOWER(TRIM(:nickname))")
    int isContactExists(String nickname);

    // Filtrelemek için kısa liste (lower+trim)
    @Query("SELECT LOWER(TRIM(originalNickname)) FROM contacts")
    List<String> getAllNicknames();
}
