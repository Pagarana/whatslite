package com.whatslite.ui;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.whatslite.service.FirebaseManager;

public class ChatListActivity extends AppCompatActivity implements FirebaseManager.FirebaseListener {

    private FirebaseManager firebaseManager;

    private String myNickname;
    private String myLanguage;
    @Nullable
    private String roomId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // setContentView(R.layout.activity_chat_list); // layout bağını sonra ekleyebilirsin

        SharedPreferences prefs = getSharedPreferences("whatslite_prefs", MODE_PRIVATE);
        myNickname = prefs.getString("nickname", "guest");
        myLanguage = prefs.getString("language", "tr");

        firebaseManager = new FirebaseManager(this, this);

        // users/<nickname> garantile + users dinlemeye başla
        firebaseManager.ensureUsersNode(myNickname, myLanguage);
        firebaseManager.startListeningUsers();

        // Oda ile gelindiyse mesajları dinle
        roomId = getIntent() != null ? getIntent().getStringExtra("roomId") : null;
        if (roomId != null && !roomId.isEmpty()) {
            firebaseManager.startListeningRoom(roomId);
        }
    }

    @Override
    public void onNewMessageReceived(String chatRoomId) {
        // TODO: adapter/list yenile
    }

    @Override
    public void onUsersChanged() {
        // TODO: kullanıcı listesini yenile
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (firebaseManager != null) {
            firebaseManager.stopMessagesListener();
            firebaseManager.stopUsersListener();
        }
    }
}
