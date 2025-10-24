package com.whatslite.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tek merkez: auth, users, rooms, inbox.
 */
public class FirebaseManager {

    private static final String TAG = "FirebaseManager";

    // ===== Singleton =====
    private static FirebaseManager INSTANCE;
    public static FirebaseManager getInstance() {
        if (INSTANCE == null) INSTANCE = new FirebaseManager();
        return INSTANCE;
    }
    private FirebaseManager() {}

    // ===== Alanlar =====
    private Context appCtx;
    private FirebaseAuth auth;
    private FirebaseDatabase db;
    private DatabaseReference usersRef;
    private DatabaseReference roomsRef;
    private DatabaseReference inboxRef;

    private String myUid;
    private String myNickname;
    private String myLanguage;

    private final List<FirebaseListener> listeners = new ArrayList<>();
    private com.google.firebase.database.ValueEventListener usersValueListener;

    private ChildEventListener roomChildListener;
    private DatabaseReference currentRoomMessagesRef;

    private ChildEventListener inboxListener;

    // ===== Listener tipleri / DTO'lar =====
    public interface FirebaseListener {
        default void onUserListUpdated(@NonNull List<ChatUser> users) {}
        default void onJoined(@NonNull String nickname, @NonNull String language) {}
        default void onUsersListeningStarted() {}
        default void onUsersListeningStopped() {}
    }
    public interface SendCallback { void onResult(boolean ok, @Nullable String err); }
    public interface MessageStream { void onMessage(@NonNull MessageDTO m); }

    public interface InboxListener { void onItem(@NonNull InboxItem item); }

    public static class ChatUser {
        public String uid;
        public String nickname;
        public String language;
        public boolean isOnline;
        public long lastSeen;
    }
    public static class MessageDTO {
        public String id;              // push key
        public String text;
        public String senderId;
        public String senderNickname;
        public long timestamp;
        public String translatedText;
        public String targetLanguage;
    }
    public static class InboxItem {
        public String peer;     // karşı tarafın nickname’i
        public String roomId;
        public String lastText;
        public long   timestamp;
        public boolean hasUnread;
    }

    // ===== Init / Auth =====
    public synchronized void setContext(@NonNull Context ctx) {
        if (appCtx != null) return;
        appCtx = ctx.getApplicationContext();

        if (FirebaseApp.getApps(appCtx).isEmpty()) {
            FirebaseApp.initializeApp(appCtx);
        }

        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        usersRef = db.getReference("users");
        roomsRef = db.getReference("rooms");
        inboxRef = db.getReference("inbox");
    }

    public String getMyUid() { return myUid; }
    public String getMyLanguage() { return myLanguage; }
    public String getMyNickname() { return myNickname; }

    public void addListener(@NonNull FirebaseListener l) { if (!listeners.contains(l)) listeners.add(l); }
    public void removeListener(@NonNull FirebaseListener l) { listeners.remove(l); }

    public void joinChat(@NonNull String nickname, @NonNull String language) {
        ensureContext();
        final String normalized = normalize(nickname);
        this.myNickname = normalized;
        this.myLanguage = language;

        if (auth.getCurrentUser() == null) {
            auth.signInAnonymously()
                .addOnSuccessListener(r -> {
                    myUid = r.getUser() != null ? r.getUser().getUid() : null;
                    upsertUserNode(normalized, language, true);
                    for (FirebaseListener l : listeners) l.onJoined(normalized, language);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Anonymous sign-in FAIL: " + e));
        } else {
            myUid = auth.getCurrentUser().getUid();
            upsertUserNode(normalized, language, true);
            for (FirebaseListener l : listeners) l.onJoined(normalized, language);
        }
    }

    // ===== Users =====
    public void startUsersListener() {
        if (usersValueListener != null) return;
        for (FirebaseListener l : listeners) l.onUsersListeningStarted();

        usersValueListener = usersRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatUser> list = new ArrayList<>();
                for (DataSnapshot c : snapshot.getChildren()) {
                    ChatUser u = new ChatUser();
                    u.nickname = c.getKey();
                    Object val = c.getValue();
                    if (val instanceof Map) {
                        Map<?, ?> m = (Map<?, ?>) val;
                        Object oUid = m.get("uid");
                        Object oLang = m.get("language");
                        Object oOnline = m.get("isOnline");
                        Object oLast = m.get("lastSeen");
                        u.uid = oUid == null ? null : String.valueOf(oUid);
                        u.language = oLang == null ? null : String.valueOf(oLang);
                        u.isOnline = oOnline instanceof Boolean ? (Boolean) oOnline : false;
                        u.lastSeen = (oLast instanceof Number) ? ((Number) oLast).longValue() : 0L;
                    }
                    list.add(u);
                }
                for (FirebaseListener l : listeners) l.onUserListUpdated(list);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "users listener cancelled: " + error);
            }
        });
    }

    public void stopUsersListener() {
        if (usersValueListener != null) {
            usersRef.removeEventListener(usersValueListener);
            usersValueListener = null;
        }
        for (FirebaseListener l : listeners) l.onUsersListeningStopped();
    }

    // ===== Rooms =====
    public void startListeningRoom(@NonNull String roomId, @NonNull MessageStream stream) {
        stopListeningRoom();

        currentRoomMessagesRef = roomsRef.child(roomId).child("messages");
        roomChildListener = currentRoomMessagesRef
                .orderByChild("timestamp")
                .addChildEventListener(new ChildEventListener() {
                    @Override public void onChildAdded(@NonNull DataSnapshot snap, @Nullable String prev) {
                        MessageDTO dto = snapshotToMessageDTO(snap);
                        if (dto != null) stream.onMessage(dto);
                    }
                    @Override public void onChildChanged(@NonNull DataSnapshot snap, @Nullable String prev) {
                        MessageDTO dto = snapshotToMessageDTO(snap);
                        if (dto != null) stream.onMessage(dto); // upsert edecek
                    }
                    @Override public void onChildRemoved(@NonNull DataSnapshot snap) {}
                    @Override public void onChildMoved(@NonNull DataSnapshot snap, @Nullable String prev) {}
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "room listener cancelled: " + error);
                    }
                });
    }

    public void stopListeningRoom() {
        if (roomChildListener != null && currentRoomMessagesRef != null) {
            currentRoomMessagesRef.removeEventListener(roomChildListener);
        }
        roomChildListener = null;
        currentRoomMessagesRef = null;
    }

    public void sendMessage(@NonNull String roomId,
                            @NonNull String text,
                            @NonNull SendCallback cb) {
        if (myUid == null) { cb.onResult(false, "Auth not ready"); return; }
        DatabaseReference msgRef = roomsRef.child(roomId).child("messages").push();

        Map<String, Object> m = new HashMap<>();
        m.put("text", text);
        m.put("senderId", myUid);
        m.put("senderNickname", myNickname);
        m.put("timestamp", ServerValue.TIMESTAMP);
        m.put("translatedText", null);
        m.put("targetLanguage", null);

        msgRef.setValue(m)
                .addOnSuccessListener(v -> cb.onResult(true, null))
                .addOnFailureListener(e -> cb.onResult(false, e.getMessage()));
    }

    /** Gönder + peer'in inbox'ını güncelle (karşı taraf eklememiş olsa da görünür). */
    public void sendMessage(@NonNull String roomId,
                            @NonNull String text,
                            @NonNull String peerNickname,
                            @NonNull SendCallback cb) {
        sendMessage(roomId, text, (ok, err) -> {
            if (!ok) { cb.onResult(false, err); return; }
            try {
                String peer = normalize(peerNickname);
                String me   = myNickname != null ? myNickname : "";
                Map<String, Object> inbox = new HashMap<>();
                inbox.put("from", me);
                inbox.put("roomId", roomId);
                inbox.put("lastText", text);
                inbox.put("timestamp", ServerValue.TIMESTAMP);
                inbox.put("hasUnread", true);
                inboxRef.child(peer).child(me).updateChildren(inbox)
                        .addOnSuccessListener(v -> cb.onResult(true, null))
                        .addOnFailureListener(e -> cb.onResult(false, e.getMessage()));
            } catch (Exception ex) {
                Log.e(TAG, "inbox update failed", ex);
                cb.onResult(true, null);
            }
        });
    }

    /** Sohbet açıldığında benim inbox öğemi okundu yap. */
    public void markInboxRead(@NonNull String peerNickname) {
        try {
            String me   = myNickname != null ? myNickname : "";
            String peer = normalize(peerNickname);
            Map<String, Object> patch = new HashMap<>();
            patch.put("hasUnread", false);
            patch.put("lastReadTs", ServerValue.TIMESTAMP);
            inboxRef.child(me).child(peer).updateChildren(patch);
        } catch (Exception e) {
            Log.e(TAG, "markInboxRead failed", e);
        }
    }

    /** Uygulama açıkken benim inbox’ımı dinle (listeyi güncelle ve bildirim göster). */
    public void startInboxListenerForMe(@NonNull InboxListener l) {
        stopInboxListener();
        if (myNickname == null) return;
        inboxListener = inboxRef.child(myNickname)
                .orderByChild("timestamp")
                .addChildEventListener(new ChildEventListener() {
                    private void handle(@NonNull DataSnapshot s) {
                        try {
                            String peer = s.getKey();
                            Object val = s.getValue();
                            if (!(val instanceof Map)) return;
                            Map<?, ?> m = (Map<?, ?>) val;

                            InboxItem it = new InboxItem();
                            it.peer      = peer;
                            it.roomId    = str(m.get("roomId"));
                            it.lastText  = str(m.get("lastText"));
                            it.timestamp = num(m.get("timestamp"));
                            Object hu = m.get("hasUnread");
                            it.hasUnread = (hu instanceof Boolean) ? (Boolean) hu : false;

                            l.onItem(it);
                        } catch (Exception e) {
                            Log.e(TAG, "inbox parse error", e);
                        }
                    }
                    @Override public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) { handle(snapshot); }
                    @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) { handle(snapshot); }
                    @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
                    @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                    @Override public void onCancelled(@NonNull DatabaseError error) { Log.e(TAG, "inbox cancelled: " + error); }
                });
    }

    public void stopInboxListener() {
        if (inboxListener != null && myNickname != null) {
            inboxRef.child(myNickname).removeEventListener(inboxListener);
        }
        inboxListener = null;
    }

    /** Debug yardımcıları için PUBLIC. */
    public void ensureUsersNode(@NonNull String nickname, @NonNull String language) {
        String n = normalize(nickname);
        Map<String, Object> map = new HashMap<>();
        map.put("uid", myUid == null ? "" : myUid);
        map.put("language", language);
        map.put("isOnline", true);
        map.put("lastSeen", ServerValue.TIMESTAMP);
        usersRef.child(n).updateChildren(map);
    }

    // ===== Yardımcılar =====
    public static String roomIdFor(@NonNull String a, @NonNull String b) {
        String na = normalize(a);
        String nb = normalize(b);
        return (na.compareTo(nb) <= 0) ? na + "__" + nb : nb + "__" + na;
    }
    public static String normalize(@NonNull String s) {
        String t = s.trim();
        if (t.startsWith("@")) t = t.substring(1);
        return t.toLowerCase(Locale.ROOT);
    }
    private void ensureContext() {
        if (appCtx == null) throw new IllegalStateException("Call setContext(context) first.");
    }
    private void upsertUserNode(@NonNull String nickname, @NonNull String language, boolean online) {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", myUid == null ? "" : myUid);
        map.put("language", language);
        map.put("isOnline", online);
        map.put("lastSeen", ServerValue.TIMESTAMP);

        DatabaseReference me = usersRef.child(nickname);
        me.updateChildren(map);

        Map<String, Object> off = new HashMap<>();
        off.put("isOnline", false);
        off.put("lastSeen", ServerValue.TIMESTAMP);
        me.onDisconnect().updateChildren(off);
    }
    @Nullable
    private static com.whatslite.service.FirebaseManager.MessageDTO snapshotToMessageDTO(@NonNull DataSnapshot s) {
        Object val = s.getValue();
        if (!(val instanceof Map)) return null;
        Map<?, ?> m = (Map<?, ?>) val;

        MessageDTO dto = new MessageDTO();
        dto.id = s.getKey();
        dto.text = str(m.get("text"));
        dto.senderId = str(m.get("senderId"));
        dto.senderNickname = str(m.get("senderNickname"));
        dto.timestamp = num(m.get("timestamp"));
        dto.translatedText = (m.get("translatedText") == null) ? null : String.valueOf(m.get("translatedText"));
        dto.targetLanguage = (m.get("targetLanguage") == null) ? null : String.valueOf(m.get("targetLanguage"));
        return dto;
    }
    private static String str(Object o) { return o == null ? null : String.valueOf(o); }
    private static long num(Object o) { return (o instanceof Number) ? ((Number) o).longValue() : 0L; }
}
