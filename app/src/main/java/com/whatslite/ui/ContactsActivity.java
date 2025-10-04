package com.whatslite.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.whatslite.R;
import com.whatslite.adapter.ContactsAdapter;
import com.whatslite.database.ChatDatabase;
import com.whatslite.database.ChatRoomDao;
import com.whatslite.database.ContactDao;
import com.whatslite.database.UserDao;
import com.whatslite.model.ChatRoom;
import com.whatslite.model.Contact;
import com.whatslite.service.FirebaseManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ContactsActivity extends AppCompatActivity implements FirebaseManager.FirebaseListener {

    private static final String TAG = "ContactsActivity";

    private RecyclerView recyclerContacts;
    private LinearLayout emptyStateLayout;
    private FloatingActionButton fabAddContact;

    private ContactDao contactDao;
    private UserDao userDao;
    private ChatRoomDao chatRoomDao;

    private ContactsAdapter contactsAdapter;
    private SharedPreferences prefs;
    private String myNickname;

    private FirebaseManager firebaseManager;
    private List<FirebaseManager.ChatUser> firebaseUsers = new ArrayList<>();
    private List<Contact> lastRenderedContacts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        // ---- Prefs & nickname ----
        prefs = getSharedPreferences("whatslite_prefs", MODE_PRIVATE);
        myNickname = prefs.getString("nickname", "");
        if (TextUtils.isEmpty(myNickname)) {
            myNickname = "user_" + System.currentTimeMillis();
            prefs.edit().putString("nickname", myNickname).apply();
        }

        // ---- UI ----
        initViews();
        initDatabase();
        setupRecyclerView();
        observeContacts();

        // ---- Firebase ----
        firebaseManager = FirebaseManager.getInstance();
        firebaseManager.setContext(this);
        firebaseManager.addListener(this);
        firebaseManager.ensureUsersNode(myNickname, "tr");
        firebaseManager.startUsersListener();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerContacts = findViewById(R.id.recyclerContacts);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        fabAddContact = findViewById(R.id.fabAddContact);

        fabAddContact.setOnClickListener(v -> showAddContactDialog());
    }

    private void initDatabase() {
        ChatDatabase db = ChatDatabase.getDatabase(this);
        contactDao = db.contactDao();
        userDao = db.userDao();           // Şimdilik kullanılmıyor; ilerde kullanılabilir.
        chatRoomDao = db.chatRoomDao();
    }

    private void setupRecyclerView() {
        recyclerContacts.setLayoutManager(new LinearLayoutManager(this));
        contactsAdapter = new ContactsAdapter(this::onContactLongClick, this::onContactClick);
        recyclerContacts.setAdapter(contactsAdapter);
    }

    private void observeContacts() {
        // 1) Önce ChatRoom'lardan Contact üret
        chatRoomDao.getChatRoomsForUser(myNickname).observe(this, chatRooms -> {
            if (chatRooms != null && !chatRooms.isEmpty()) {
                createContactsFromChatRooms(chatRooms);
            } else {
                Log.d(TAG, "No chat rooms found");
            }
        });

        // 2) Sonra Contact DB'sini dinle
        contactDao.getAllContacts().observe(this, contacts -> {
            if (contacts != null && !contacts.isEmpty()) {
                updateContactsWithOnlineStatus(contacts);
                emptyStateLayout.setVisibility(LinearLayout.GONE);
                recyclerContacts.setVisibility(RecyclerView.VISIBLE);
                Log.d(TAG, "Loaded " + contacts.size() + " contacts from database");
            } else {
                emptyStateLayout.setVisibility(LinearLayout.VISIBLE);
                recyclerContacts.setVisibility(RecyclerView.GONE);
                Log.d(TAG, "No contacts in database");
            }
        });
    }

    private void createContactsFromChatRooms(List<ChatRoom> chatRooms) {
        new Thread(() -> {
            List<Contact> contacts = new ArrayList<>();

            for (ChatRoom chatRoom : chatRooms) {
                String otherUserNickname = chatRoom.getOtherParticipant(myNickname);

                Contact existingContact = contactDao.getContactByNickname(otherUserNickname);
                if (existingContact == null) {
                    Contact newContact = new Contact(otherUserNickname, null, "tr");
                    newContact.lastSeenDate = chatRoom.lastMessageTime;
                    contactDao.insertContact(newContact);
                    contacts.add(newContact);
                    Log.d(TAG, "Auto-created contact for: " + otherUserNickname);
                } else {
                    existingContact.lastSeenDate =
                            Math.max(existingContact.lastSeenDate, chatRoom.lastMessageTime);
                    contacts.add(existingContact);
                }
            }

            Log.d(TAG, "Total contacts processed: " + contacts.size());

            runOnUiThread(() -> updateContactsWithOnlineStatus(contacts));
        }).start();
    }

    private void updateContactsWithOnlineStatus(List<Contact> contacts) {
        // Firebase online durumlarını işle
        for (Contact contact : contacts) {
            boolean isOnline = false;
            for (FirebaseManager.ChatUser fbUser : firebaseUsers) {
                String normalizedContactNick = normalizeNickname(contact.originalNickname);
                String normalizedFbUserNick = normalizeNickname(fbUser.nickname);

                if (normalizedFbUserNick.equals(normalizedContactNick)) {
                    isOnline = fbUser.isOnline;
                    contact.lastSeenDate = fbUser.lastSeen;
                    Log.d(TAG, "Matched Firebase user: " + fbUser.nickname +
                            " ↔ contact: " + contact.originalNickname);
                    break;
                }
            }
            // Basit işaret: "online" / "offline" (sıralama için)
            contact.profileImagePath = isOnline ? "online" : "offline";
        }

        // Sıralama: önce online, sonra ada göre
        Collections.sort(contacts, new Comparator<Contact>() {
            @Override
            public int compare(Contact c1, Contact c2) {
                boolean c1Online = "online".equals(c1.profileImagePath);
                boolean c2Online = "online".equals(c2.profileImagePath);

                if (c1Online && !c2Online) return -1;
                if (!c1Online && c2Online) return 1;

                String name1 = c1.getDisplayNameOrOriginal();
                String name2 = c2.getDisplayNameOrOriginal();
                return name1.compareToIgnoreCase(name2);
            }
        });

        contactsAdapter.updateContacts(contacts);
        lastRenderedContacts = new ArrayList<>(contacts);
    }

    private void onContactLongClick(Contact contact) {
        showRenameDialog(contact);
    }

    private void onContactClick(Contact contact) {
        new Thread(() -> {
            ChatRoom existingRoom =
                    chatRoomDao.findChatRoomBetweenUsers(myNickname, contact.originalNickname);

            if (existingRoom == null) {
                ChatRoom newRoom = new ChatRoom(myNickname, contact.originalNickname);
                chatRoomDao.insertChatRoom(newRoom);
                existingRoom = newRoom;
            }

            final String chatRoomId = existingRoom.chatRoomId;

            runOnUiThread(() -> {
                Intent intent = new Intent(ContactsActivity.this, ChatActivity.class);
                intent.putExtra("chatRoomId", chatRoomId);
                intent.putExtra("otherUserNickname", contact.originalNickname);
                startActivity(intent);
                Log.d(TAG, "Opening chat with: " + contact.originalNickname);
            });
        }).start();
    }

    private void showRenameDialog(Contact contact) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Contact");

        final EditText input = new EditText(this);
        input.setText(contact.displayName != null ? contact.displayName : contact.originalNickname);
        input.selectAll();
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!TextUtils.isEmpty(newName)) {
                new Thread(() -> {
                    contactDao.updateDisplayName(contact.originalNickname, newName);
                    runOnUiThread(() ->
                            Log.d(TAG, "Renamed " + contact.originalNickname + " → " + newName));
                }).start();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showAddContactDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Contact");
        builder.setMessage("Enter the nickname of the person you want to add:");

        final EditText input = new EditText(this);
        input.setHint("Nickname");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String nickname = input.getText().toString().trim();
            if (!TextUtils.isEmpty(nickname) && !nickname.equals(myNickname)) {
                new Thread(() -> {
                    Contact existing = contactDao.getContactByNickname(nickname);
                    if (existing == null) {
                        Contact newContact = new Contact(nickname, nickname, "tr");
                        newContact.lastSeenDate = System.currentTimeMillis();
                        contactDao.insertContact(newContact);
                        runOnUiThread(() ->
                                Log.d(TAG, "Added new contact: " + nickname));
                    } else {
                        runOnUiThread(() ->
                                Log.d(TAG, "Contact already exists: " + nickname));
                    }
                }).start();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // ---- Menu ----
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---- Lifecycle ----
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (firebaseManager != null) {
            firebaseManager.removeListener(this);
            firebaseManager.stopUsersListener();
            firebaseManager.stopListeningRoom();
        }
    }

    // ===== FirebaseManager.FirebaseListener =====
    @Override
    public void onUserListUpdated(List<FirebaseManager.ChatUser> users) {
        this.firebaseUsers = users;
        if (lastRenderedContacts != null && !lastRenderedContacts.isEmpty()) {
            // Ekrandaki listeyi online durumuyla tazele
            updateContactsWithOnlineStatus(new ArrayList<>(lastRenderedContacts));
        }
    }

    @Override
    public void onMessageReceived(String from, String message, String chatRoomId,
                                  long timestamp, String senderLanguage) {
        // Mesajdan kişiyi otomatik ekle (yoksa)
        new Thread(() -> {
            int exists = contactDao.isContactExists(from);
            if (exists == 0 && !from.equals(myNickname)) {
                Contact newContact = new Contact(from, from, senderLanguage);
                contactDao.insertContact(newContact);
                Log.d(TAG, "Auto-added new contact: " + from);
            }
        }).start();
    }

    @Override
    public void onConnectionStatusChanged(boolean connected) {
        Log.d(TAG, "Firebase connection: " + connected);
    }

    @Override
    public void onError(String error) {
        Log.e(TAG, "Firebase error: " + error);
    }

    // ---- Utils ----
    /** Normalize nickname: baştaki '@' kaldır + lowercase. */
    private String normalizeNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) return "";
        String n = nickname.trim();
        if (n.startsWith("@")) n = n.substring(1);
        return n.toLowerCase();
    }
}
