package com.whatslite.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.whatslite.R;

/**
 * Bildirim servisi – tıklanınca ChatActivity'yi açar.
 * DİKKAT: Bu dosyada SADECE NotificationService public sınıfı olmalı.
 */
public class NotificationService {

    private static final String CHANNEL_ID = "msgs";
    private static final String CHANNEL_NAME = "Messages";

    private static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            ch.enableLights(true);
            ch.setLightColor(Color.GREEN);
            ch.enableVibration(true);
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    /**
     * @param context      app context
     * @param peerNickname karşı tarafın nick'i
     * @param previewText  önizleme metni
     */
    public static void showIncomingMessageNotification(Context context,
                                                       String peerNickname,
                                                       String previewText) {
        ensureChannel(context);

        // Tam nitelikli sınıf adıyla ChatActivity
        Intent intent = new Intent(context, com.whatslite.ui.ChatActivity.class);
        intent.putExtra("peerNickname", peerNickname);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                context,
                (peerNickname == null ? 0 : peerNickname.hashCode()),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat) // yoksa ic_translate kullan
                .setContentTitle(peerNickname == null ? context.getString(R.string.app_name) : peerNickname)
                .setContentText(previewText == null ? "" : previewText)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            int id = (peerNickname == null ? 1 : Math.abs(peerNickname.hashCode()));
            nm.notify(id, b.build());
        }
    }
}
