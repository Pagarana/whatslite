package com.whatslite.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class FirebaseBackgroundService extends Service {
    private static final String TAG = "FirebaseBgService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Bound service değil
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Started");
        // Burada arka plan sync, lightweight işler yapılabilir.
        return START_STICKY;
    }
}
