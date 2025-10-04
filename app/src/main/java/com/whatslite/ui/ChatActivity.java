package com.whatslite.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.whatslite.R;
import com.whatslite.adapter.MessagesAdapter;
import com.whatslite.database.ChatDatabase;
import com.whatslite.database.MessageDao;
import com.whatslite.model.Message;
import com.whatslite.service.FirebaseManager;

import java.util.List;

public class ChatActivity extends AppCompatActivity implements FirebaseManager.FirebaseListener {

    private RecyclerView recyclerMessages;
    private EditText edtMessage;
    private ImageButton btnSend;

    private MessageDao messageDao;
    private MessagesAdapter adapter;
    private FirebaseManager firebaseManager;

    private String roomId;
    private String myNickname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerMessages = findViewById(R.id.recyclerMessages);
        edtMessage = findViewById(R.id.edtMessage);
        btnSend = findViewById(R.id.btnSend);

        roomId = getIntent().getStringExtra("roomId");
        myNickname = getSharedPreferences("whatslite_prefs", MODE_PRIVATE)
                .getString("nickname", "guest");

        ChatDatabase db = ChatDatabase.getDatabase(getApplicationContext());
        messageDao = db.messageDao();

        adapter = new MessagesAdapter(myNickname);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerMessages.setAdapter(adapter);

        firebaseManager = new FirebaseManager(this, this);
        firebaseManager.startListeningRoom(roomId);

        btnSend.setOnClickListener(v -> {
            String text = edtMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                Message m = new Message();
                m.roomId = roomId;
                m.sender = myNickname;
                m.text = text;
                m.timestamp = System.currentTimeMillis();
                // Lokal kaydet
                messageDao.insert(m);
                // Firebase’e gönder
                firebaseManager.sendMessage(roomId, m);
                edtMessage.setText("");
                refreshMessages();
            }
        });

        refreshMessages();
    }

    private void refreshMessages() {
        List<Message> list = messageDao.getByRoomSync(roomId);
        adapter.submitList(list);
        recyclerMessages.scrollToPosition(Math.max(0, list.size() - 1));
    }

    @Override
    public void onNewMessageReceived(String chatRoomId) {
        if (roomId != null && roomId.equals(chatRoomId)) {
            refreshMessages();
        }
    }
}
