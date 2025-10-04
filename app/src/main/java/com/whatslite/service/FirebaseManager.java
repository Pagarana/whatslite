package com.whatslite.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.whatslite.model.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * FirebaseManager (singleton)
 * - ContactsActivity'nin beklediği API ile %100 uyumludur:
 *   getInstance(), setContext(), addListener(), removeListener()
 *   FirebaseListener: onUserListUpdated(...), onMessageReceived(...), onConnectionStatusChanged(...), onError(...)
 *
 * - Realtime DB ağaç varsayımları:
 *   /users/<nickname> { nickname, language, isOnline, lastSeen, userId }
 *   /messages/<autoId> { chatRoomId, sender, to, text, timestamp }
 */
public final class FirebaseManager {

    // ===== Listener arayüzü (ContactsActivity ile uyumlu) =====
    public interface FirebaseListener {
        /** Kullanıcı listesi değiştiğinde TAM liste gönderilir. */
        void onUserListUpdated(List<ChatUser> users);

        /** Yeni mesaj geldiğinde bildirir. */
        default void onMessageReceived(String from, String message, String chatRoomId, long timestamp, @Nullable String senderLanguage) {}

        /** Bağlantı durumu değişimi. */
        default void onConnectionStatusChanged(boolean connected) {}

        /** Hata kancası. */
        default void onError(String error) {}
    }

    // ===== Kamuya açık basit model (ContactsActivity FirebaseManager.ChatUser olarak kullanıyor) =====
    public static class ChatUser {
        public String userId;
        public String nickname;
        public String language;
        public boolean isOnline;
        public long lastSeen;

        public ChatUser() {}
        public ChatUser(String userId, String nickname, String language, boolean isOnline, long lastSeen) {
            this.userId = userId;
            this.nickname = nickname;
            this.language = language;
            this.isOnline = isOnline;
            this.lastSeen = lastSeen;
        }
    }

    private static final String TAG = "FirebaseManager";
    private static FirebaseManager INSTANCE;

    public static synchronized FirebaseManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FirebaseManager();
        }
        return INSTANCE;
    }

    // ==== Alanlar ====
    private Context appContext;
    private final List<FirebaseListener> listeners = new CopyOnWriteArrayList<>();
    private final DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

    // Dinleyici referansları (iptal edebilmek için)
    private ValueEventListener usersValueListener;
    private ChildEventListener messagesListener;
    private String listeningRoomId;

    // Cache
    private final List<ChatUser> cachedUsers = new ArrayList<>();

    private FirebaseManager() {}

    /** Application context ataması (ContactsActivity çağırıyor). */
    public void setContext(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void addListener(@NonNull FirebaseListener l) {
        listeners.add(l);
        // Yeni eklenen dinleyiciye mevcut cache'i hemen gönder
        if (!cachedUsers.isEmpty()) {
            l.onUserListUpdated(new ArrayList<>(cachedUsers));
        }
    }

    public void removeListener(@NonNull FirebaseListener l) {
        listeners.remove(l);
    }

    // ======= USERS =======

    /** users kökünü ve kendi kaydını garanti eder (isteğe bağlı). */
    public void ensureUsersNode(@NonNull String myNickname, @Nullable String language) {
        DatabaseReference usersRef = rootRef.child("users");
        usersRef.get().addOnCompleteListener(t -> {
            if (!t.isSuccessful() || t.getResult() == null || !t.getResult().exists()) {
                usersRef.setValue(new HashMap<>()); // boş obje
            }
            if (!myNickname.isEmpty()) {
                Map<String, Object> me = new HashMap<>();
                me.put("nickname", myNickname);
                me.put("language", language != null ? language : "tr");
                me.put("isOnline", true);
                me.put("lastSeen", System.currentTimeMillis());
                me.put("userId", myNickname);
                usersRef.child(myNickname).updateChildren(me);
            }
        });
    }

    /** /users için value listener: tam listeyi her değişimde yollar. */
    public synchronized void startUsersListener() {
        stopUsersListener();
        usersValueListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                cachedUsers.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        ChatUser u = child.getValue(ChatUser.class);
                        if (u == null) {
                            // Manuel parse
                            String key = child.getKey();
                            String nickname = asString(child.child("nickname").getValue(), key);
                            String lang = asString(child.child("language").getValue(), "tr");
                            boolean online = asBool(child.child("isOnline").getValue());
                            long last = asLong(child.child("lastSeen").getValue());
                            u = new ChatUser(key, nickname, lang, online, last);
                        }
                        cachedUsers.add(u);
                    }
                }
                for (FirebaseListener l : listeners) {
                    l.onUserListUpdated(new ArrayList<>(cachedUsers));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                emitError("users listen cancelled: " + error);
            }
        };
        rootRef.child("users").addValueEventListener(usersValueListener);
        emitConn(true);
    }

    public synchronized void stopUsersListener() {
        if (usersValueListener != null) {
            rootRef.child("users").removeEventListener(usersValueListener);
            usersValueListener = null;
        }
    }

    // ======= MESSAGES / ROOM =======

    /** Belirli oda için /messages altında chatRoomId == roomId dinler. */
    public synchronized void startListeningRoom(@NonNull String roomId) {
        stopListeningRoom();
        listeningRoomId = roomId;
        messagesListener = new ChildEventListener() {
            @Override public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Message m = snapshot.getValue(Message.class);
                if (m == null) {
                    // Yedek parse
                    String sender = asString(snapshot.child("sender").getValue(), null);
                    String text = asString(snapshot.child("text").getValue(), "");
                    long ts = asLong(snapshot.child("timestamp").getValue());
                    for (FirebaseListener l : listeners) {
                        l.onMessageReceived(sender, text, roomId, ts, null);
                    }
                } else {
                    for (FirebaseListener l : listeners) {
                        l.onMessageReceived(m.sender, m.text, roomId, m.timestamp, null);
                    }
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                emitError("messages listen cancelled: " + error);
            }
        };
        rootRef.child("messages").orderByChild("chatRoomId").equalTo(roomId)
                .addChildEventListener(messagesListener);
    }

    public synchronized void stopListeningRoom() {
        if (messagesListener != null) {
            if (listeningRoomId != null) {
                rootRef.child("messages").orderByChild("chatRoomId").equalTo(listeningRoomId)
                        .removeEventListener(messagesListener);
            }
            messagesListener = null;
            listeningRoomId = null;
        }
    }

    /** Mesaj gönder (isteğe bağlı, mevcut koda uyum için). */
    public void sendMessage(@NonNull String roomId, @NonNull Message message) {
        if (message.timestamp == 0) message.timestamp = System.currentTimeMillis();
        message.roomId = roomId;
        rootRef.child("messages").push().setValue(message)
                .addOnFailureListener(e -> emitError("sendMessage failed: " + e));
    }

    // ===== helper =====
    private void emitConn(boolean connected) {
        for (FirebaseListener l : listeners) l.onConnectionStatusChanged(connected);
    }
    private void emitError(String err) {
        Log.e(TAG, err);
        for (FirebaseListener l : listeners) l.onError(err);
    }
    private static String asString(Object v, @Nullable String def) {
        return v == null ? (def == null ? "" : def) : String.valueOf(v);
    }
    private static boolean asBool(Object v) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v == null) return false;
        return "true".equalsIgnoreCase(String.valueOf(v));
    }
    private static long asLong(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        if (v == null) return 0L;
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; }
    }
}
