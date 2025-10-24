package com.whatslite.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.whatslite.database.ChatDatabase;
import com.whatslite.database.ContactDao;
import com.whatslite.model.Contact;

import java.util.ArrayList;
import java.util.List;

/**
 * Basit sohbet listesi ekranı.
 *
 * NOT: Bu sürümde FirebaseManager.InboxItem KULLANILMIYOR.
 * Sadece yerel Room "contacts" tablosundaki kişiler listelenir.
 * Bir kişiye tıklayınca ChatActivity açılır.
 */
public class ChatListActivity extends AppCompatActivity {

    private RecyclerView rv;
    private View emptyView;
    private Adapter adapter;
    private ContactDao contactDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ---- Programatik sade UI (layout dosyasına ihtiyaç yok) ----
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Chats");
        title.setTextSize(20);
        title.setPadding(0, 0, 0, dp(8));
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        rv = new RecyclerView(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.addItemDecoration(new DividerItemDecoration(this, RecyclerView.VERTICAL));
        root.addView(rv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ));

        emptyView = new TextView(this);
        ((TextView) emptyView).setText("No chats yet");
        ((TextView) emptyView).setGravity(Gravity.CENTER);
        ((TextView) emptyView).setTextSize(16);
        emptyView.setVisibility(View.GONE);
        root.addView(emptyView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        Button btnContacts = new Button(this);
        btnContacts.setText("Open Contacts");
        btnContacts.setOnClickListener(v ->
                startActivity(new Intent(ChatListActivity.this, ContactsActivity.class)));
        root.addView(btnContacts, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        setContentView(root);
        // -------------------------------------------------------------

        adapter = new Adapter(item -> {
            Intent it = new Intent(ChatListActivity.this, ChatActivity.class);
            it.putExtra("peerNickname", item.originalNickname);
            startActivity(it);
        });
        rv.setAdapter(adapter);

        contactDao = ChatDatabase.getDatabase(this).contactDao();

        // Kişileri yükle
        loadContactsAsChats();
    }

    private void loadContactsAsChats() {
        new Thread(() -> {
            List<Contact> contacts = contactDao.getAllContactsSync();
            List<Item> items = new ArrayList<>();
            if (contacts != null) {
                for (Contact c : contacts) {
                    if (c == null || c.originalNickname == null) continue;
                    Item it = new Item();
                    it.originalNickname = c.originalNickname;
                    it.displayName = (c.displayName != null && !c.displayName.trim().isEmpty())
                            ? c.displayName : c.originalNickname;
                    items.add(it);
                }
            }
            runOnUiThread(() -> {
                adapter.submit(items);
                boolean empty = items.isEmpty();
                emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
                rv.setVisibility(empty ? View.GONE : View.VISIBLE);
            });
        }).start();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    // ----------------- Basit model -----------------
    static class Item {
        String originalNickname;
        String displayName;
    }

    // ----------------- Adapter -----------------
    static class Adapter extends RecyclerView.Adapter<Adapter.VH> {

        interface Click {
            void onClick(@NonNull Item item);
        }

        private final List<Item> data = new ArrayList<>();
        private final Click click;

        Adapter(Click c) { this.click = c; }

        void submit(List<Item> list) {
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.VERTICAL);
            int pad = (int) (12 * parent.getResources().getDisplayMetrics().density);
            row.setPadding(pad, pad, pad, pad);

            TextView tvTop = new TextView(parent.getContext());
            tvTop.setTextSize(16);
            tvTop.setTag("top");
            row.addView(tvTop, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            TextView tvBottom = new TextView(parent.getContext());
            tvBottom.setTextSize(12);
            tvBottom.setAlpha(0.7f);
            tvBottom.setTag("bottom");
            row.addView(tvBottom, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            return new VH(row, tvTop, tvBottom);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Item it = data.get(pos);
            h.top.setText(it.displayName);
            h.bottom.setText("@" + it.originalNickname);
            h.itemView.setOnClickListener(v -> {
                if (click != null) click.onClick(it);
            });
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView top, bottom;
            VH(@NonNull View itemView, TextView top, TextView bottom) {
                super(itemView);
                this.top = top;
                this.bottom = bottom;
            }
        }
    }
}
