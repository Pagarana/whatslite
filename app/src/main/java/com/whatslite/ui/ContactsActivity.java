package com.whatslite.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.whatslite.R;
import com.whatslite.database.ChatDatabase;
import com.whatslite.database.ContactDao;
import com.whatslite.model.Contact;
import com.whatslite.service.FirebaseManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Kişiler ekranı (SON HAL):
 *  - Liste YEREL Room tablosundan gelir (LiveData ile anında güncellenir).
 *  - Firebase /users yalnızca durum (online/dil) zenginleştirmesi için okunur.
 *  - Böylece kişi ekleyince ANINDA görünür; karşı taraf daha eklememiş olsa bile.
 */
public class ContactsActivity extends AppCompatActivity implements FirebaseManager.FirebaseListener {

    private RecyclerView rv;
    private View emptyState;
    private FloatingActionButton fabAdd;

    private UsersAdapter adapter;

    private ContactDao contactDao;
    private FirebaseManager fm;

    // Yerel veriler (LiveData ile güncellenir)
    private List<Contact> localContacts = new ArrayList<>();

    // Firebase durum zenginleştirmesi için: nick -> user
    private final Map<String, FirebaseManager.ChatUser> usersByNick = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        rv         = findViewById(R.id.recyclerContacts);
        emptyState = findViewById(R.id.emptyStateLayout);
        fabAdd     = findViewById(R.id.fabAddContact);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        rv.setLayoutManager(lm);
        rv.addItemDecoration(new DividerItemDecoration(this, lm.getOrientation()));

        adapter = new UsersAdapter(nick -> openChat(nick));
        rv.setAdapter(adapter);

        // DB
        contactDao = ChatDatabase.get(this).contactDao();

        // Firebase
        fm = FirebaseManager.getInstance();
        fm.setContext(this);
        fm.addListener(this);
        fm.startUsersListener(); // sadece durum için (online/lang)

        // Yerel Contact listesini reaktif izle -> anında listeyi yenile
        LiveData<List<Contact>> live = contactDao.getAllContacts();
        live.observe(this, contacts -> {
            localContacts = (contacts == null) ? new ArrayList<>() : contacts;
            rebuildAndSubmit(); // Firebase durum bilgisiyle zenginleştir
        });

        // Kişi ekle
        fabAdd.setOnClickListener(v -> showAddContactDialog());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fm != null) {
            fm.removeListener(this);
            fm.stopUsersListener();
        }
    }

    // ===== Firebase Listener =====

    @Override public void onUsersListeningStarted() {}
    @Override public void onUsersListeningStopped() {}

    @Override
    public void onUserListUpdated(@NonNull List<FirebaseManager.ChatUser> users) {
        // Gelen tüm kullanıcıları haritaya koy (normalize edilmiş nick ile)
        usersByNick.clear();
        for (FirebaseManager.ChatUser u : users) {
            if (u == null || u.nickname == null) continue;
            usersByNick.put(normalize(u.nickname), u);
        }
        // Yerel liste değişmemiş olsa bile durum bilgisi değişmiş olabilir -> yeniden kur
        runOnUiThread(this::rebuildAndSubmit);
    }

    // ===== Yardımcılar =====

    private void rebuildAndSubmit() {
        // localContacts'ı tek tek satıra çevir; varsa usersByNick ile zenginleştir
        List<Row> rows = new ArrayList<>();
        String myNick = normalize(fm.getMyNickname());

        for (Contact c : localContacts) {
            if (c == null || c.originalNickname == null) continue;
            String nick = normalize(c.originalNickname);

            // KENDİMİ listeleme (istenmiyor)
            if (!TextUtils.isEmpty(myNick) && myNick.equals(nick)) continue;

            FirebaseManager.ChatUser u = usersByNick.get(nick);

            Row r = new Row();
            r.originalNickname = c.originalNickname;
            r.displayName = c.getDisplayNameOrOriginal();
            // Dil: Firebase varsa onu, yoksa contact.language
            String lang = (u != null && !TextUtils.isEmpty(u.language)) ? u.language
                         : (TextUtils.isEmpty(c.language) ? "—" : c.language);
            r.subtitle = "@" + c.originalNickname + " • " + lang;
            r.online = (u != null && u.isOnline);

            rows.add(r);
        }

        adapter.submit(rows);
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean empty = adapter.getItemCount() == 0;
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void openChat(String peerNickname) {
        if (TextUtils.isEmpty(peerNickname)) return;
        Intent i = new Intent(this, ChatActivity.class);
        i.putExtra("peerNickname", peerNickname);
        startActivity(i);
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("@")) t = t.substring(1);
        return t.toLowerCase(Locale.ROOT);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    // ===== Kişi Ekle Dialogu =====
    private void showAddContactDialog() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        final EditText etNick = new EditText(this);
        etNick.setHint("Nickname");
        root.addView(etNick, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final EditText etDisplay = new EditText(this);
        etDisplay.setHint("Display name (optional)");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        root.addView(etDisplay, lp);

        new AlertDialog.Builder(this)
                .setTitle("Add contact")
                .setView(root)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", (d, w) -> {
                    String raw = etNick.getText().toString().trim();
                    if (raw.isEmpty()) {
                        Toast.makeText(this, "Please enter a nickname", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String nickNorm = normalize(raw);

                    // Kendini ekleme
                    String myNickNorm = normalize(fm.getMyNickname());
                    if (!TextUtils.isEmpty(myNickNorm) && myNickNorm.equals(nickNorm)) {
                        Toast.makeText(this, "You cannot add yourself", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String disp = etDisplay.getText().toString().trim();

                    // DB işlemleri arka thread’de
                    new Thread(() -> {
                        // Case-insensitive varlık kontrolü
                        int exists = contactDao.isContactExistsCI(nickNorm);
                        if (exists > 0) {
                            runOnUiThread(() ->
                                    Toast.makeText(this, "This contact already exists", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        // Kaydet (Contact ctor: (originalNickname, displayName, language))
                        Contact c = new Contact(nickNorm, disp, "");
                        contactDao.insertContact(c);

                        // LiveData zaten gözlemliyor -> otomatik liste yenilenecek
                        runOnUiThread(() ->
                                Toast.makeText(this, "Contact added", Toast.LENGTH_SHORT).show());
                    }).start();
                })
                .show();
    }

    // ===== Görsel satır modeli =====
    static class Row {
        String originalNickname;
        String displayName;
        String subtitle; // @nick • lang
        boolean online;
    }

    // ===== Adapter =====
    static class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.VH> {

        interface Click { void onClick(@NonNull String originalNickname); }

        private final List<Row> data = new ArrayList<>();
        private final Click click;

        UsersAdapter(Click c) { this.click = c; }

        void submit(List<Row> rows) {
            data.clear();
            if (rows != null) data.addAll(rows);
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_contact, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Row r = data.get(pos);
            h.tvName.setText(r.displayName == null ? r.originalNickname : r.displayName);
            h.tvNick.setText(r.subtitle == null ? ("@" + r.originalNickname) : r.subtitle);
            h.tvStatus.setText(r.online ? "Online" : "Offline");

            View.OnClickListener open = v -> {
                if (click != null) click.onClick(r.originalNickname);
            };
            h.itemView.setOnClickListener(open);
            h.btnChat.setOnClickListener(open);
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvNick, tvStatus;
            ImageView btnChat;
            VH(@NonNull View v) {
                super(v);
                tvName   = v.findViewById(R.id.tvContactName);
                tvNick   = v.findViewById(R.id.tvContactNickname);
                tvStatus = v.findViewById(R.id.tvOnlineStatus);
                btnChat  = v.findViewById(R.id.btnChat);
            }
        }
    }
}
