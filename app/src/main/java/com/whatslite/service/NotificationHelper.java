package com.whatslite.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.whatslite.R;
import com.whatslite.ui.ChatActivity;

public final class NotificationHelper {
    private static final String CHANNEL_ID = "whatslite_general";
    private static final String CHANNEL_NAME = "WhatsLite Notifications";

    private NotificationHelper(){}

    private static void ensureChannel(Context ctx){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Chat messages and alerts");
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
    }

    public static void showNewMessage(Context ctx, String fromNick, String text, String roomId){
        ensureChannel(ctx);

        Intent tap = new Intent(ctx, ChatActivity.class);
        tap.putExtra("peerNickname", fromNick);
        tap.putExtra("roomId", roomId);
        tap.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                ctx, (fromNick + "_" + roomId).hashCode(), tap,
                Build.VERSION.SDK_INT >= 31
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_translate)
                .setContentTitle(fromNick)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(("msg_"+fromNick).hashCode(), n);
    }
}
