package com.whatslite.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.whatslite.adapter.MessagesAdapter;
import com.whatslite.model.Message;
import com.whatslite.service.FirebaseManager;

import java.util.Date;

public class ChatActivity extends AppCompatActivity implements MessagesAdapter.MessageActionListener {

    private RecyclerView rv;
    private EditText et;
    private ImageButton btn;

    private MessagesAdapter adapter;
    private FirebaseManager fm;

    private String myNick;
    private String peerNick;
    private String roomId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Basit programatik UI
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        rv = new RecyclerView(this);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rv.setLayoutManager(lm);
        root.addView(rv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        int p = dp(12);
        bar.setPadding(p, dp(8), p, dp(8));

        et = new EditText(this);
        et.setHint("Mesaj yazın…");
        et.setMaxLines(4);
        bar.addView(et, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        btn = new ImageButton(this);
        btn.setImageResource(android.R.drawable.ic_menu_send);
        btn.setBackground(null);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.gravity = Gravity.CENTER_VERTICAL;
        bar.addView(btn, blp);

        root.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);

        // Prefs: kendim
        SharedPreferences prefs = getSharedPreferences("ChatTranslator", MODE_PRIVATE);
        myNick = prefs.getString("nickname", "");

        // Karşı taraf
        peerNick = getIntent().getStringExtra("peerNickname");
        if (TextUtils.isEmpty(peerNick)) {
            String peerUid = getIntent().getStringExtra("peerUid");
            peerNick = (peerUid != null && !peerUid.trim().isEmpty()) ? peerUid : "unknown";
        }

        // Oda kimliği
        roomId = FirebaseManager.roomIdFor(myNick, peerNick);

        // Firebase
        fm = FirebaseManager.getInstance();
        fm.setContext(this);

        // Adapter (myUid hazır değilse geçici)
        adapter = new MessagesAdapter(fm.getMyUid() == null ? "temp" : fm.getMyUid(), this);
        rv.setAdapter(adapter);

        // Odayı dinle: DUPLICATE ENGELİ => pushKey ile upsert
        fm.startListeningRoom(roomId, dto -> runOnUiThread(() -> {
            Message m = new Message();
            m.chatRoomId = roomId;
            m.text = dto.text == null ? "" : dto.text;
            m.senderId = dto.senderId == null ? "" : dto.senderId;
            m.timestamp = dto.timestamp == 0L ? new Date().getTime() : dto.timestamp;
            m.translatedText = dto.translatedText;
            m.targetLanguage = dto.targetLanguage;
            String myUid = fm.getMyUid();
            m.isFromMe = (myUid != null && myUid.equals(dto.senderId));
            adapter.addOrUpdateWithKey(dto.id == null ? (m.senderId+"|"+m.timestamp+"|"+m.text.hashCode()) : dto.id, m);
            rv.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
        }));

        // Inbox'ımı okundu yap (karşı taraf adına gelen satırdaki kırmızı nokta vs)
        fm.markInboxRead(peerNick);

        // Gönder butonu – SADECE Firebase’e yaz, adapter’e ELLE ekleme (çiftlenme biter)
        btn.setOnClickListener(v -> {
            String t = et.getText().toString().trim();
            if (t.isEmpty()) return;
            fm.sendMessage(roomId, t, peerNick, (ok, err) -> runOnUiThread(() -> {
                if (ok) et.setText("");
                else Toast.makeText(this, "Gönderilemedi: " + err, Toast.LENGTH_SHORT).show();
            }));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fm.markInboxRead(peerNick);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fm != null) fm.stopListeningRoom();
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    @Override public void onMessageLongClick(Message m, View anchor) { /* menü vs. */ }
}
