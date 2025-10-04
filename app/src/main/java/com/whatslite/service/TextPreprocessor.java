package com.whatslite.service;

import android.os.Build;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TextPreprocessor {
    private static final String TAG = "TextPreprocessor";
    
    // Türkçe karakter düzeltmeleri
    private static final Map<String, String> TURKISH_FIXES = new HashMap<>();
    static {
        // Yaygın yazım hataları
        TURKISH_FIXES.put("oylemi", "öyle mi");
        TURKISH_FIXES.put("nasilsin", "nasılsın");
        TURKISH_FIXES.put("nasilsın", "nasılsın");
        TURKISH_FIXES.put("nasılsın", "nasılsın");
        TURKISH_FIXES.put("naber", "ne haber");
        TURKISH_FIXES.put("slm", "selam");
        TURKISH_FIXES.put("selm", "selam");
        TURKISH_FIXES.put("mrhb", "merhaba");
        TURKISH_FIXES.put("mrb", "merhaba");
        TURKISH_FIXES.put("tmm", "tamam");
        TURKISH_FIXES.put("tamamdır", "tamam");
        TURKISH_FIXES.put("olr", "olur");
        TURKISH_FIXES.put("iyidir", "iyi");
        TURKISH_FIXES.put("iyiyim", "iyiyim");
        TURKISH_FIXES.put("teşekkürler", "teşekkür ederim");
        TURKISH_FIXES.put("tşk", "teşekkür ederim");
        TURKISH_FIXES.put("tşkrlr", "teşekkür ederim");
        TURKISH_FIXES.put("görüşürüz", "görüşürüz");
        TURKISH_FIXES.put("görüşürz", "görüşürüz");
        TURKISH_FIXES.put("gln", "gülen");
        TURKISH_FIXES.put("knk", "kanka");
        
        // İ/i, Ü/ü, Ğ/g, Ş/s, Ç/c, Ö/o düzeltmeleri
        TURKISH_FIXES.put("gercekten", "gerçekten");
        TURKISH_FIXES.put("cogunlukla", "çoğunlukla");
        TURKISH_FIXES.put("bugun", "bugün");
        TURKISH_FIXES.put("dun", "dün");
        TURKISH_FIXES.put("yarin", "yarın");
        TURKISH_FIXES.put("aksamusic", "akşam üstü");
        TURKISH_FIXES.put("aksam", "akşam");
        TURKISH_FIXES.put("sabahleyin", "sabahleyin");
        TURKISH_FIXES.put("oglen", "öğlen");
        TURKISH_FIXES.put("ogleden", "öğleden");
    }
    
    // İngilizce casual fixes
    private static final Map<String, String> ENGLISH_FIXES = new HashMap<>();
    static {
        ENGLISH_FIXES.put("u", "you");
        ENGLISH_FIXES.put("ur", "your");
        ENGLISH_FIXES.put("r", "are");
        ENGLISH_FIXES.put("n", "and");
        ENGLISH_FIXES.put("thx", "thanks");
        ENGLISH_FIXES.put("ty", "thank you");
        ENGLISH_FIXES.put("np", "no problem");
        ENGLISH_FIXES.put("omg", "oh my god");
        ENGLISH_FIXES.put("lol", "laugh out loud");
        ENGLISH_FIXES.put("brb", "be right back");
        ENGLISH_FIXES.put("gtg", "got to go");
        ENGLISH_FIXES.put("ttyl", "talk to you later");
        ENGLISH_FIXES.put("hru", "how are you");
        ENGLISH_FIXES.put("wbu", "what about you");
        ENGLISH_FIXES.put("tbh", "to be honest");
        ENGLISH_FIXES.put("imo", "in my opinion");
        ENGLISH_FIXES.put("btw", "by the way");
        ENGLISH_FIXES.put("fyi", "for your information");
    }
    
    /**
     * Metni çeviri için hazırlar - yazım hatalarını düzeltir
     */
    public static String preprocessText(String text, String language) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        Log.d(TAG, "🔧 Preprocessing text (" + language + "): " + text);
        
        String processed = text.trim().toLowerCase();
        
        // Dile göre düzeltmeler
        if ("tr".equals(language)) {
            processed = fixTurkish(processed);
        } else if ("en".equals(language)) {
            processed = fixEnglish(processed);
        }
        
        // Genel düzeltmeler
        processed = generalFixes(processed);
        
        Log.d(TAG, "✅ Preprocessed result: " + processed);
        
        return processed;
    }
    
    private static String fixTurkish(String text) {
        String fixed = text;
        
        // Yaygın Türkçe hatalarını düzelt
        for (Map.Entry<String, String> entry : TURKISH_FIXES.entrySet()) {
            fixed = fixed.replaceAll("\\b" + Pattern.quote(entry.getKey()) + "\\b", entry.getValue());
        }
        
        // Türkçe karakter eksikliklerini akıllı düzelt
        fixed = smartTurkishCharacters(fixed);
        
        return fixed;
    }
    
    private static String fixEnglish(String text) {
        String fixed = text;
        
        // İngilizce casual language düzelt
        for (Map.Entry<String, String> entry : ENGLISH_FIXES.entrySet()) {
            fixed = fixed.replaceAll("\\b" + Pattern.quote(entry.getKey()) + "\\b", entry.getValue());
        }
        
        return fixed;
    }
    
    private static String smartTurkishCharacters(String text) {
        // Akıllı Türkçe karakter düzeltmeleri
        String fixed = text;
        
        // Context-aware düzeltmeler
        fixed = fixed.replaceAll("\\bgıl\\b", "gil"); // "gel" için
        fixed = fixed.replaceAll("\\bgeliyorum\\b", "geliyorum");
        fixed = fixed.replaceAll("\\bgidiyorum\\b", "gidiyorum");
        fixed = fixed.replaceAll("\\bişi\\b", "iş");
        fixed = fixed.replaceAll("\\böyle\\b", "öyle");
        fixed = fixed.replaceAll("\\bçok\\b", "çok");
        
        return fixed;
    }
    
    private static String generalFixes(String text) {
        String fixed = text;
        
        // Çoklu boşlukları düzelt
        fixed = fixed.replaceAll("\\s+", " ");
        
        // Noktalama işaretlerini düzelt
        fixed = fixed.replaceAll("([.!?])([a-zA-Z])", "$1 $2");
        
        // Başlangıç harfini büyük yap
        if (fixed.length() > 0) {
            fixed = fixed.substring(0, 1).toUpperCase() + fixed.substring(1);
        }
        
        return fixed.trim();
    }
    
    /**
     * Metinon quality score hesaplar (0-100)
     */
    public static int calculateTextQuality(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        
        int score = 100;
        
        // Çok kısa metinler için düşük skor
        if (text.length() < 3) score -= 30;
        
        // Sadece harf olmayan karakterler
        if (!text.matches(".*[a-zA-ZçğıöşüÇĞIÖŞÜ].*")) score -= 50;
        
        // Çok fazla yazım hatası (heuristic)
        long spaceCount = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            spaceCount = text.chars().filter(ch -> ch == ' ').count();
        }
        long wordCount = spaceCount + 1;
        if (text.length() / wordCount > 15) score -= 20; // Çok uzun kelimeler
        
        return Math.max(0, Math.min(100, score));
    }
}