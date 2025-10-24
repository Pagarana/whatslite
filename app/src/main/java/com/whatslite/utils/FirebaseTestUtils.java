package com.whatslite.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.whatslite.service.FirebaseManager;

public final class FirebaseTestUtils {

    private static final String TAG = "FirebaseTestUtils";

    private FirebaseTestUtils() {}

    public static void sendOnce(@NonNull Context ctx,
                                @NonNull String me,
                                @NonNull String peer,
                                @NonNull String text) {
        FirebaseManager fm = FirebaseManager.getInstance();
        fm.setContext(ctx);

        fm.joinChat(me, "tr"); // ensureUsersNode GEREK YOK

        String roomId = FirebaseManager.roomIdFor(me, peer);
        fm.sendMessage(roomId, text, (ok, err) -> {
            if (ok) Log.d(TAG, "Mesaj gönderildi: " + text);
            else    Log.e(TAG, "Gönderim hatası: " + err);
        });
    }
}
