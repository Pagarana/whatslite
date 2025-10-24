package com.whatslite.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.whatslite.database.ChatDatabase;
import com.whatslite.database.ContactDao;
import com.whatslite.model.Contact;

import java.util.Locale;
import java.util.Map;

/**
 * - onNewToken: token'ı /users/<myNick>/tokens/<token>=true altına yazar
 * - onMessageReceived: bildirimi gösterir, gerekiyorsa kişiyi yerelde otomatik ekler
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM token: " + token);
        registerTokenWithServer(getApplicationContext(), token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage msg) {
        super.onMessageReceived(msg);

        Map<String, String> d = msg.getData();
        String fromNick = d.get("fromNick");
        String text     = d.get("text");
        String roomId   = d.get("roomId");

        if (fromNick == null) fromNick = "unknown";
        if (text == null) text = "";

        // 1) Bildirimi göster
        NotificationHelper.showNewMessage(getApplicationContext(), fromNick, text, roomId);

        // 2) Yerelde kişi yoksa otomatik ekle (listeye düşmesi için)
        autoAddContactIfMissing(getApplicationContext(), fromNick);
    }

    /** Uygulama içinden çağırılabilsin diye statik yardımcı */
    public static void registerCurrentToken(Context ctx, String myNick) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Log.d(TAG, "registerCurrentToken: " + token);
                    SharedPreferences p = ctx.getSharedPreferences("ChatTranslator", Context.MODE_PRIVATE);
                    p.edit().putString("fcm_token", token).apply();
                    if (myNick != null && !myNick.trim().isEmpty()) {
                        writeTokenToDb(normalize(myNick), token);
                    }
                });
    }

    private static void registerTokenWithServer(Context ctx, String token){
        SharedPreferences p = ctx.getSharedPreferences("ChatTranslator", Context.MODE_PRIVATE);
        p.edit().putString("fcm_token", token).apply();
        String myNick = p.getString("nickname", null);
        if (myNick != null && !myNick.trim().isEmpty()){
            writeTokenToDb(normalize(myNick), token);
        }
    }

    private static void writeTokenToDb(String myNick, String token){
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(myNick)
                .child("tokens")
                .child(token)
                .setValue(true);
    }

    private static String normalize(String s){
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("@")) t = t.substring(1);
        return t.toLowerCase(Locale.ROOT);
    }

    private void autoAddContactIfMissing(Context ctx, String fromNick){
        ContactDao dao = ChatDatabase.getDatabase(ctx).contactDao();
        String norm = normalize(fromNick);
        new Thread(() -> {
            int exists = dao.isContactExistsCI(norm);
            if (exists == 0){
                Contact c = new Contact(norm, "", "");
                dao.insertContact(c);
            }
        }).start();
    }
}
