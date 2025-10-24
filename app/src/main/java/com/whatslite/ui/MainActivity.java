package com.whatslite.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.whatslite.R;
import com.whatslite.database.ChatDatabase;
import com.whatslite.database.UserDao;
import com.whatslite.model.User;
import com.whatslite.service.FirebaseManager;
import com.whatslite.utils.DebugUtils;

public class MainActivity extends AppCompatActivity {

    private EditText etNickname;
    private Spinner spinnerLanguage;
    private Button btnJoinChat;
    private ProgressBar progressBar;
    private TextView tvError;

    private UserDao userDao;
    private SharedPreferences prefs;
    private FirebaseManager firebaseManager;

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    private final String[] languages = {
            "🇹🇷 Türkçe","🇺🇸 English","🇪🇸 Español","🇫🇷 Français","🇩🇪 Deutsch","🇮🇹 Italiano",
            "🇵🇹 Português","🇷🇺 Русский","🇸🇦 العربية","🇨🇳 中文","🇯🇵 日本語","🇰🇷 한국어",
            "🇳🇱 Nederlands","🇸🇪 Svenska","🇳🇴 Norsk","🇩🇰 Dansk","🇫🇮 Suomi","🇬🇷 Ελληνικά",
            "🇮🇳 हिन्दी","🇹🇭 ไทย","🇻🇳 Tiếng Việt","🇵🇱 Polski","🇨🇿 Čeština","🇭🇺 Magyar",
            "🇷🇴 Română","🇧🇬 Български","🇭🇷 Hrvatski","🇸🇌 Slovenčina","🇸🇮 Slovenščina",
            "🇺🇦 Українська","🇮🇷 فارسی","🇮🇱 עברית","🇵🇰 اردو","🇧🇩 বাংলা","🇲🇾 Bahasa Malaysia",
            "🇮🇩 Bahasa Indonesia","🇵🇭 Filipino","🇪🇪 Eesti","🇱🇻 Latviešu","🇱🇹 Lietuvių"
    };
    private final String[] languageCodes = {
            "tr","en","es","fr","de","it","pt","ru","ar","zh","ja","ko",
            "nl","sv","no","da","fi","el","hi","th","vi","pl","cs","hu",
            "ro","bg","hr","sk","sl","uk","fa","he","ur","bn","ms","id","tl","et","lv","lt"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("ChatTranslator", MODE_PRIVATE);
        String savedNickname = prefs.getString("nickname", null);

        // Zaten giriş yaptıysa doğrudan Kişiler ekranına
        if (savedNickname != null) {
            startActivity(new Intent(this, ContactsActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        userDao = ChatDatabase.getDatabase(this).userDao();
        firebaseManager = FirebaseManager.getInstance();
        firebaseManager.setContext(this);

        initViews();
        setupLanguageSpinner();
        setupJoinButton();
        requestNotificationPermission();

        DebugUtils.checkFirebaseConnection();
    }

    private void initViews() {
        etNickname = findViewById(R.id.etNickname);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        btnJoinChat = findViewById(R.id.btnJoinChat);
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);
    }

    private void setupLanguageSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);
        spinnerLanguage.setSelection(0);
    }

    private void setupJoinButton() {
        btnJoinChat.setOnClickListener(v -> joinChat());
    }

    private void joinChat() {
        String nickname = etNickname.getText().toString().trim();
        if (TextUtils.isEmpty(nickname)) {
            showError("Please enter a nickname");
            return;
        }
        String normalizedNickname = normalizeNickname(nickname);
        if (normalizedNickname.length() < 3) {
            showError("Nickname must be at least 3 characters");
            return;
        }

        showLoading(true);

        new Thread(() -> {
            boolean isAvailable = userDao.isNicknameExists(normalizedNickname) == 0;
            runOnUiThread(() -> {
                showLoading(false);
                if (isAvailable) {
                    String selectedLanguage = languageCodes[spinnerLanguage.getSelectedItemPosition()];

                    new Thread(() -> {
                        User user = new User(normalizedNickname, selectedLanguage);
                        user.isOnline = true;
                        userDao.insertUser(user);
                    }).start();

                    prefs.edit()
                            .putString("nickname", normalizedNickname)
                            .putString("language", selectedLanguage)
                            .apply();

                    DebugUtils.logUserJoin(normalizedNickname, selectedLanguage);
                    firebaseManager.joinChat(normalizedNickname, selectedLanguage);

                    // >>> Token'ı yazdır (ÖNEMLİ) <<<
                    com.whatslite.service.MyFirebaseMessagingService
                            .registerCurrentToken(getApplicationContext(), normalizedNickname);

                    // Kişiler ekranına geç
                    Intent intent = new Intent(MainActivity.this, ContactsActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    showError("This nickname is already taken. Please choose another one.");
                }
            });
        }).start();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnJoinChat.setEnabled(!show);
        etNickname.setEnabled(!show);
        spinnerLanguage.setEnabled(!show);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /** '@' varsa at, trim + lowercase */
    private String normalizeNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) return "";
        String n = nickname.trim();
        if (n.startsWith("@")) n = n.substring(1);
        return n.toLowerCase();
    }
}
