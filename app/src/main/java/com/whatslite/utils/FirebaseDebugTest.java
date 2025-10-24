package com.whatslite.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.whatslite.service.FirebaseManager;

public final class FirebaseDebugTest {

    private static final String TAG = "FirebaseDebugTest";

    private FirebaseDebugTest() {}

    public static void sendSample(@NonNull Context ctx,
                                  @NonNull String myNickname,
                                  @NonNull String peerNickname) {
        FirebaseManager fm = FirebaseManager.getInstance();
        fm.setContext(ctx);

        fm.joinChat(myNickname, "tr"); // ensureUsersNode GEREK YOK

        String roomId = FirebaseManager.roomIdFor(myNickname, peerNickname);
        fm.sendMessage(roomId, "Merhaba, test!", (ok, err) -> {
            if (ok) Log.d(TAG, "Mesaj gönderildi.");
            else    Log.e(TAG, "Gönderim hatası: " + err);
        });
    }
}
