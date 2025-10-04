package com.whatslite.utils;

import android.util.Log;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

public class DebugUtils {
    private static final String TAG = "DEBUG_FIREBASE";
    
    public static void checkFirebaseConnection() {
        Log.d(TAG, "=== FIREBASE CONNECTION TEST ===");
        
        // Test Firebase connection
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                Log.d(TAG, "Firebase Connected: " + connected);
                
                if (connected) {
                    Log.d(TAG, "✅ Firebase connection is WORKING");
                } else {
                    Log.e(TAG, "❌ Firebase connection is FAILED");
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "❌ Firebase connection listener cancelled: " + error.getMessage());
            }
        });
        
        // Test database read/write
        testDatabaseReadWrite();
    }
    
    private static void testDatabaseReadWrite() {
        DatabaseReference testRef = FirebaseDatabase.getInstance().getReference("debug_test");
        
        // Write test data
        testRef.setValue("test_" + System.currentTimeMillis())
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Firebase WRITE successful");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Firebase WRITE failed: " + e.getMessage());
            });
        
        // Read test data
        testRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                Log.d(TAG, "✅ Firebase READ successful: " + value);
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "❌ Firebase READ failed: " + databaseError.getMessage());
            }
        });
    }
    
    public static void logUserJoin(String nickname, String language) {
        Log.d(TAG, "=== USER JOIN ATTEMPT ===");
        Log.d(TAG, "Nickname: " + nickname);
        Log.d(TAG, "Language: " + language);
        Log.d(TAG, "Timestamp: " + System.currentTimeMillis());
    }
    
    public static void logUserList(int userCount) {
        Log.d(TAG, "=== USER LIST UPDATE ===");
        Log.d(TAG, "Total users online: " + userCount);
    }
    
    public static void logError(String operation, String error) {
        Log.e(TAG, "=== ERROR IN " + operation + " ===");
        Log.e(TAG, "Error: " + error);
    }
}
