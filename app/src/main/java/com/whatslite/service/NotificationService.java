package com.whatslite.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.whatslite.R;
import com.whatslite.ui.ChatActivity;
import android.util.Log;

public class NotificationService {
    
    private static final String CHANNEL_ID = "whatslite_messages";
    private static final String CHANNEL_NAME = "WhatsLite Messages";
    private static final int NOTIFICATION_ID = 1001;
    
    private Context context;
    private NotificationManagerCompat notificationManager;
    private SharedPreferences prefs;
    
    // 📱 Açık chat ekranı takibi
    private static String currentOpenChatRoomId = null;
    
    // 📱 Uygulama açık mı takibi
    private static boolean isAppInForeground = false;
    
    public NotificationService(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        this.prefs = context.getSharedPreferences("ChatTranslator", Context.MODE_PRIVATE);
        createNotificationChannel();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for new messages");
            channel.enableLights(true);
            channel.enableVibration(true);
            
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
            
            Log.d("NotificationService", "✅ Notification channel created");
        }
    }
    
    public void showMessageNotification(String senderName, String message, String chatRoomId) {
        // Check if notifications are enabled
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        if (!notificationsEnabled) {
            Log.d("NotificationService", "🔇 Notifications disabled, skipping");
            return;
        }
        
        Log.d("NotificationService", "🔔 Showing notification for message from: " + senderName);
        
        // ÖNEMLİ: Firebase'den gelen chatRoomId yanlış olabilir, doğrusu generate et
        String myNickname = prefs.getString("nickname", "");
        String correctChatRoomId = generateChatRoomId(myNickname, senderName);
        
        Log.d("NotificationService", "🔧 Firebase chatRoomId: " + chatRoomId + " -> Correct: " + correctChatRoomId);
        
        // ✅ KONTROL 1: Uygulama açık mı?
        if (isAppInForeground) {
            Log.d("NotificationService", "📱 App is in FOREGROUND (recent chats open) - SKIPPING notification");
            return; // Uygulama açıksa bildirim gösterme!
        }
        
        // ✅ KONTROL 2: Bu chat ekranı açık mı?
        if (correctChatRoomId.equals(currentOpenChatRoomId)) {
            Log.d("NotificationService", "📱 Chat is currently OPEN - SKIPPING notification");
            return; // Bildirim gösterme!
        }
        
        Log.d("NotificationService", "✅ App is in BACKGROUND and chat is closed - showing notification");
        
        // Create intent to open chat - DOĞRU chatRoomId ile
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("chatRoomId", correctChatRoomId); // Doğru ID kullan
        intent.putExtra("otherUserNickname", senderName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // WhatsApp tarzı bildirim oluştur
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(senderName) // WhatsApp gibi sadece gönderen ismi
            .setContentText(message) // Mesajın kendisi
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message)) // Uzun mesajlar için
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setShowWhen(true) // Zaman gösterimi
            .setContentIntent(pendingIntent)
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI) // Ses
            .setVibrate(new long[]{0, 250, 250, 250}) // Titreşim patternı
            .setLights(0xFF00FF00, 300, 1000); // Yeşil LED
        
        // Show notification
        try {
            // Her sohbet için unique notification ID kullan
            int notificationId = chatRoomId.hashCode();
            notificationManager.notify(notificationId, builder.build());
            Log.d("NotificationService", "✅ Notification shown successfully for chat: " + chatRoomId);
        } catch (SecurityException e) {
            Log.e("NotificationService", "😨 Permission denied for notifications", e);
        }
    }
    
    public void clearNotifications() {
        notificationManager.cancelAll();
        Log.d("NotificationService", "🧹 All notifications cleared");
    }
    
    public boolean areNotificationsEnabled() {
        return prefs.getBoolean("notifications_enabled", true);
    }
    
    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply();
        Log.d("NotificationService", "🔔 Notifications " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * ChatActivity açıldığında çağırılır
     */
    public static void setCurrentOpenChat(String chatRoomId) {
        currentOpenChatRoomId = chatRoomId;
        Log.d("NotificationService", "📱 Chat opened: " + chatRoomId);
    }
    
    /**
     * ChatActivity kapandığında çağırılır
     */
    public static void clearCurrentOpenChat() {
        Log.d("NotificationService", "📱 Chat closed: " + currentOpenChatRoomId);
        currentOpenChatRoomId = null;
    }
    
    /**
     * Uygulama açıldığında çağırılır (ChatListActivity, ChatActivity vs.)
     */
    public static void setAppInForeground() {
        isAppInForeground = true;
        Log.d("NotificationService", "📱 App is now in FOREGROUND");
    }
    
    /**
     * Uygulama tamamen kapandığında çağırılır
     */
    public static void setAppInBackground() {
        isAppInForeground = false;
        Log.d("NotificationService", "📱 App is now in BACKGROUND");
    }
    
    /**
     * Generate chatRoomId using same logic as ChatRoom model
     */
    private String generateChatRoomId(String user1, String user2) {
        // Alfabetik sıraya koy ki aynı oda ID'si oluşsun
        if (user1.compareTo(user2) < 0) {
            return user1 + "_" + user2;
        } else {
            return user2 + "_" + user1;
        }
    }
}
