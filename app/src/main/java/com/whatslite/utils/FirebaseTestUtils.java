package com.whatslite.utils;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseTestUtils {

    private static final String TAG = "FirebaseTestUtils";

    public static void connectionSmokeTest() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(".info/connected");
        ref.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Firebase reachable");
            } else {
                Log.e(TAG, "Firebase not reachable", task.getException());
            }
        });
    }

    public static void writeReadSmokeTest() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("whatslite/test");
        ref.setValue("ok");
        ref.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Write/Read ok: " + task.getResult().getValue());
            } else {
                Log.e(TAG, "Write/Read failed", task.getException());
            }
        });
    }
}
