package com.whatslite;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.whatslite.BuildConfig; // DEBUG log için BuildConfig erişimi

public class WhatsLiteApplication extends Application {

    public static final String CHANNEL_ID = "whatslite_default_channel";

    @Override
    public void onCreate() {
        super.onCreate();

        // 1) Bildirim kanalı (Android 8.0+)
        createDefaultNotificationChannel();

        // 2) Firebase anonim oturum (telefon + emülatör aynı projeye bağlansın)
        ensureAnonymousSignIn();

        // 3) DEBUG: DeepL anahtarı user-level gradle.properties'ten okundu mu?
        //    (Sadece debug derlemelerde log düşer)
        if (BuildConfig.DEBUG) {
            Log.d(
                "APP",
                "DeepL host=" + BuildConfig.DEEPL_API_HOST +
                " | key_set=" + (BuildConfig.DEEPL_API_KEY != null && !BuildConfig.DEEPL_API_KEY.isEmpty())
            );
        }
    }

    private void createDefaultNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "WhatsLite Bildirimleri";
            String desc = "Mesaj ve genel bildirim kanalı";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(desc);

            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
                Log.d("APP", "NotificationChannel created: " + CHANNEL_ID);
            } else {
                Log.w("APP", "NotificationManager is null; channel not created");
            }
        }
    }

    private void ensureAnonymousSignIn() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            auth.signInAnonymously().addOnCompleteListener(t -> {
                if (t.isSuccessful()) {
                    FirebaseUser u = auth.getCurrentUser();
                    Log.d("APP", "Anon sign-in OK. uid=" + (u != null ? u.getUid() : "null"));
                } else {
                    Log.e("APP", "Anon sign-in FAILED: ", t.getException());
                }
            });
        } else {
            Log.d("APP", "Already signed-in. uid=" + auth.getCurrentUser().getUid());
        }
    }
}
