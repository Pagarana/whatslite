package com.whatslite.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.whatslite.model.User;
import com.whatslite.model.Message;
import com.whatslite.model.ChatRoom;
import com.whatslite.model.Contact;

@Database(
    entities = {User.class, Message.class, ChatRoom.class, Contact.class},
    version = 3,
    exportSchema = false
)
public abstract class ChatDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract MessageDao messageDao();
    public abstract ChatRoomDao chatRoomDao();
    public abstract ContactDao contactDao();
    
    private static volatile ChatDatabase INSTANCE;
    
    public static ChatDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (ChatDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            ChatDatabase.class, "chat_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
