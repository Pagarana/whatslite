package com.whatslite.utils;

import android.content.Context;
import android.util.Log;
import com.whatslite.service.FirebaseManager;

public class FirebaseDebugTest {
    
    private static final String TAG = "FirebaseDebugTest";
    
    public static void testFirebaseMessaging(Context context, String fromUser, String toUser, String message) {
        Log.d(TAG, "===============================================");
        Log.d(TAG, "🧪 FIREBASE DEBUG TEST STARTED");
        Log.d(TAG, "===============================================");
        Log.d(TAG, "📤 From: " + fromUser);
        Log.d(TAG, "📥 To: " + toUser);
        Log.d(TAG, "💬 Message: " + message);
        Log.d(TAG, "===============================================");
        
        FirebaseManager firebaseManager = FirebaseManager.getInstance();
        
        if (firebaseManager.isConnected()) {
            Log.d(TAG, "✅ FirebaseManager is connected");
            
            // Test mesajı gönder
            String testChatRoomId = "test_" + fromUser + "_" + toUser;
            firebaseManager.sendMessage(toUser, message, testChatRoomId);
            
            Log.d(TAG, "📡 Test message sent via Firebase");
        } else {
            Log.e(TAG, "❌ FirebaseManager is NOT connected");
            Log.e(TAG, "💡 Call firebaseManager.joinChat() first");
        }
        
        Log.d(TAG, "===============================================");
        Log.d(TAG, "🧪 FIREBASE DEBUG TEST COMPLETED");
        Log.d(TAG, "===============================================");
    }
}