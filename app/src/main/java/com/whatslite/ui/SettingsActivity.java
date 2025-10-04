package com.whatslite.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.whatslite.R;
import com.whatslite.service.SmartTranslationService;

public class SettingsActivity extends AppCompatActivity {
    
    private SharedPreferences prefs;
    private Spinner spinnerTranslationProvider;
    private TextView tvCurrentProvider;
    
    @Override 
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        prefs = getSharedPreferences("ChatTranslator", MODE_PRIVATE);
        
        initViews();
        // setupTranslationProviderSpinner();
        // updateCurrentProviderDisplay();
        
        // DeepL-only mode - sadece DeepL kullanılıyor
        SmartTranslationService.setPreferredProvider(this, SmartTranslationService.TranslationProvider.DEEPL);
        
        // DeepL bilgi mesajı
        Toast.makeText(this, "🎯 Çeviri: DeepL API (Yüksek Kalite)", Toast.LENGTH_LONG).show();
    }
    
    private void initViews() {
        // UI elements will be added later - for now just basic functionality
        // spinnerTranslationProvider = findViewById(R.id.spinnerTranslationProvider);
        // tvCurrentProvider = findViewById(R.id.tvCurrentProvider);
    }
    
    private void setupTranslationProviderSpinner() {
        // DeepL-only mode: sadece bilgi görüntülemesi
        String[] providers = {"DeepL (Sadece)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, providers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTranslationProvider.setAdapter(adapter);
        
        // Her zaman DeepL seçili
        spinnerTranslationProvider.setSelection(0);
        spinnerTranslationProvider.setEnabled(false); // Değiştirilemez
        
        // DeepL-only bilgi mesajı
        Toast.makeText(this, "🎯 Sadece DeepL kullanılıyor (Yüksek Kalite)", Toast.LENGTH_LONG).show();
    }
    
    private void updateCurrentProviderDisplay() {
        // DeepL-only mode: her zaman DeepL
        if (tvCurrentProvider != null) {
            tvCurrentProvider.setText("Aktif Çeviri Servisi: DeepL (Yüksek Kalite)");
        }
    }
}
