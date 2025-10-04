package com.whatslite.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.os.Build;
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
    
    private String[] languages = {
        "🇹🇷 Türkçe", "🇺🇸 English", "🇪🇸 Español", "🇫🇷 Français", "🇩🇪 Deutsch", "🇮🇹 Italiano", 
        "🇵🇹 Português", "🇷🇺 Русский", "🇸🇦 العربية", "🇨🇳 中文", "🇯🇵 日本語", "🇰🇷 한국어",
        "🇳🇱 Nederlands", "🇸🇪 Svenska", "🇳🇴 Norsk", "🇩🇰 Dansk", "🇫🇮 Suomi", "🇬🇷 Ελληνικά",
        "🇮🇳 हिन्दी", "🇹🇭 ไทย", "🇻🇳 Tiếng Việt", "🇵🇱 Polski", "🇨🇿 Čeština", "🇭🇺 Magyar",
        "🇷🇴 Română", "🇧🇬 Български", "🇭🇷 Hrvatski", "🇸🇰 Slovenčina", "🇸🇮 Slovenščina",
        "🇺🇦 Українська", "🇮🇷 فارسی", "🇮🇱 עברית", "🇮🇳 اردو", "🇧🇩 বাংলা", "🇲🇾 Bahasa Malaysia",
        "🇮🇩 Bahasa Indonesia", "🇵🇭 Filipino", "🇪🇪 Eesti", "🇱🇻 Latviešu", "🇱🇹 Lietuvių"
    };
    
    private String[] languageCodes = {
        "tr", "en", "es", "fr", "de", "it", "pt", "ru", "ar", "zh", "ja", "ko",
        "nl", "sv", "no", "da", "fi", "el", "hi", "th", "vi", "pl", "cs", "hu",
        "ro", "bg", "hr", "sk", "sl", "uk", "fa", "he", "ur", "bn", "ms",
        "id", "tl", "et", "lv", "lt"
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences("ChatTranslator", MODE_PRIVATE);
        String savedNickname = prefs.getString("nickname", null);
        
        if (savedNickname != null) {
            // User already has a nickname, go to chat list
            Intent intent = new Intent(this, ChatListActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        
        setContentView(R.layout.activity_main);
        
        userDao = ChatDatabase.getDatabase(this).userDao();
        firebaseManager = FirebaseManager.getInstance();
        
        initViews();
        setupLanguageSpinner();
        setupJoinButton();
        
        // Bildirim izni iste (Android 13+)
        requestNotificationPermission();
        
        // Debug Firebase connection
        DebugUtils.checkFirebaseConnection();
        
        // Comprehensive Firebase test
        com.whatslite.utils.FirebaseTestUtils.runCompleteFirebaseTest(this);
        
        // Ensure users node exists in Firebase
        com.whatslite.utils.FirebaseTestUtils.ensureUsersNodeExists();
        
        // Firebase will connect automatically when needed
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
        spinnerLanguage.setSelection(0); // Default to Turkish
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
        
        // Normalize nickname for consistent storage
        String normalizedNickname = normalizeNickname(nickname);
        
        if (normalizedNickname.length() < 3) {
            showError("Nickname must be at least 3 characters");
            return;
        }
        
        showLoading(true);
        
        // Check if normalized nickname is available (in background thread)
        new Thread(() -> {
            boolean isAvailable = userDao.isNicknameExists(normalizedNickname) == 0;
            
            runOnUiThread(() -> {
                showLoading(false);
                
                if (isAvailable) {
                    // Save user data
                    String selectedLanguage = languageCodes[spinnerLanguage.getSelectedItemPosition()];
                    
                    // Save to database (use normalized nickname)
                    new Thread(() -> {
                        User user = new User(normalizedNickname, selectedLanguage);
                        user.isOnline = true;
                        userDao.insertUser(user);
                    }).start();
                    
                    // Save to SharedPreferences (use normalized nickname)
                    prefs.edit()
                        .putString("nickname", normalizedNickname)
                        .putString("language", selectedLanguage)
                        .apply();
                    
                    // Debug user join
                    DebugUtils.logUserJoin(normalizedNickname, selectedLanguage);
                    
                    // Join Firebase chat (will be normalized again in FirebaseManager)
                    firebaseManager.joinChat(normalizedNickname, selectedLanguage);
                    
                    // Go to chat list
                    Intent intent = new Intent(MainActivity.this, ChatListActivity.class);
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
                
                ActivityCompat.requestPermissions(this, 
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 
                    REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Bildirim izni verildi
                android.util.Log.d("MainActivity", "🔔 Notification permission granted");
            } else {
                // Bildirim izni reddedildi
                android.util.Log.d("MainActivity", "😵 Notification permission denied");
            }
        }
    }
    
    /**
     * Normalize nickname for consistent storage and comparison
     * - Remove @ prefix if present
     * - Convert to lowercase for case-insensitive matching
     */
    private String normalizeNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return "";
        }
        
        String normalized = nickname.trim();
        
        // Remove @ prefix if present
        if (normalized.startsWith("@")) {
            normalized = normalized.substring(1);
        }
        
        // Convert to lowercase for case-insensitive matching
        normalized = normalized.toLowerCase();
        
        return normalized;
    }
}
