package com.whatslite.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.whatslite.model.ChatRoom;
import com.whatslite.model.Contact;
import com.whatslite.model.Message;
import com.whatslite.model.User;

@Database(
        entities = {
                User.class,
                Contact.class,
                ChatRoom.class,
                Message.class
        },
        version = 1,
        exportSchema = false
)
public abstract class ChatDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract ContactDao contactDao();
    public abstract ChatRoomDao chatRoomDao();
    public abstract MessageDao messageDao();

    private static volatile ChatDatabase INSTANCE;

    public static ChatDatabase get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (ChatDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    ctx.getApplicationContext(),
                                    ChatDatabase.class,
                                    "whatslite.db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /** Eski çağrılarla uyumluluk için alias */
    public static ChatDatabase getDatabase(Context ctx) {
        return get(ctx);
    }
}
