package com.whatslite.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SmartTranslationService {
    private static final String TAG = "SmartTranslationService";
    
    // Translation providers - Hybrid approach
    public enum TranslationProvider {
        DEEPL,
        GEMINI_AI,
        HYBRID_SMART
    }
    
    public interface Callback {
        void onSuccess(String translated, TranslationProvider usedProvider);
        void onError(String message);
    }
    
    /**
     * DeepL ile çeviri - Sadece DeepL kullanır
     */
    public static void translateSmart(Context context, String text, String from, String to, Callback callback) {
        Log.d(TAG, "🎯 Using DeepL-only translation strategy");
        Log.d(TAG, "📝 Original text: " + text);
        
        // Metni ön işleme al - yazım hatalarını düzel
        String preprocessedText = TextPreprocessor.preprocessText(text, from);
        int qualityScore = TextPreprocessor.calculateTextQuality(preprocessedText);
        
        Log.d(TAG, "🔧 Preprocessed: " + preprocessedText);
        Log.d(TAG, "📊 Quality score: " + qualityScore + "/100");
        Log.d(TAG, "📝 Final translation: " + from + " -> " + to);
        
        // Akıllı provider seçimi
        TranslationProvider selectedProvider = selectBestProvider(preprocessedText, qualityScore);
        Log.d(TAG, "🤖 Selected provider: " + selectedProvider);
        
        // Ön işleme yapılmış metni çevir
        translateWithProvider(context, preprocessedText, from, to, selectedProvider, callback);
    }
    
    /**
     * Belirtilen provider ile çevir
     */
    public static void translateWithProvider(Context context, String text, String from, String to, 
                                           TranslationProvider provider, Callback callback) {
        
        Log.d(TAG, "Translating with " + provider + ": " + from + " -> " + to);
        
        switch (provider) {
            case DEEPL:
                Log.d(TAG, "🚀 Using DeepL API for translation");
                DeepLTranslationService.translateAsync(context, text, from, to, new DeepLTranslationService.Callback() {
                    @Override
                    public void onSuccess(String translated) {
                        Log.d(TAG, "✅ DeepL translation successful");
                        callback.onSuccess(translated, TranslationProvider.DEEPL);
                    }
                    
                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "❌ DeepL failed, trying Gemini as fallback: " + message);
                        // Fallback to Gemini if configured
                        if (GeminiTranslationService.isConfigured()) {
                            translateWithGemini(context, text, from, to, callback);
                        } else {
                            callback.onError("DeepL failed and Gemini not configured: " + message);
                        }
                    }
                });
                break;
                
            case GEMINI_AI:
                translateWithGemini(context, text, from, to, callback);
                break;
                
            case HYBRID_SMART:
                // İlk DeepL dene, başarısız olursa Gemini
                translateWithProvider(context, text, from, to, TranslationProvider.DEEPL, callback);
                break;
                
            default:
                Log.e(TAG, "Unknown translation provider: " + provider);
                callback.onError("DeepL-only mode: Invalid provider - " + provider);
        }
    }
    
    /**
     * Kullanıcının tercih ettiği provider'ı kaydet
     */
    public static void setPreferredProvider(Context context, TranslationProvider provider) {
        SharedPreferences prefs = context.getSharedPreferences("ChatTranslator", Context.MODE_PRIVATE);
        prefs.edit().putString("translation_provider", provider.name()).apply();
        Log.d(TAG, "Translation provider set to: " + provider);
    }
    
    /**
     * Mevcut provider'ı al - DeepL only
     */
    public static TranslationProvider getPreferredProvider(Context context) {
        // Her zaman DeepL dönüyor
        return TranslationProvider.DEEPL;
    }
    
    private static void translateWithGemini(Context context, String text, String from, String to, Callback callback) {
        Log.d(TAG, "🤖 Using Gemini AI for translation");
        GeminiTranslationService.translateWithAI(context, text, from, to, new GeminiTranslationService.Callback() {
            @Override
            public void onSuccess(String translated) {
                Log.d(TAG, "✅ Gemini AI translation successful");
                callback.onSuccess(translated, TranslationProvider.GEMINI_AI);
            }
            
            @Override
            public void onError(String message) {
                Log.e(TAG, "❌ Gemini AI translation failed: " + message);
                callback.onError("Gemini AI translation failed: " + message);
            }
        });
    }
    
    private static TranslationProvider selectBestProvider(String text, int qualityScore) {
        // Gemini yapılandırılmamışsa DeepL kullan
        if (!GeminiTranslationService.isConfigured()) {
            Log.d(TAG, "🤖 Gemini not configured, using DeepL");
            return TranslationProvider.DEEPL;
        }
        
        // Metin kalitesi düşükse (yazim hatali, slang), Gemini AI daha iyi
        if (qualityScore < 60) {
            Log.d(TAG, "🤖 Low quality text (" + qualityScore + "/100), using Gemini AI for better understanding");
            return TranslationProvider.GEMINI_AI;
        }
        
        // Yüksek kaliteli metinler için DeepL (hızlı ve kaliteli)
        Log.d(TAG, "🚀 High quality text (" + qualityScore + "/100), using DeepL for speed");
        return TranslationProvider.DEEPL;
    }
    
    /**
     * Translation provider'ın görünen adını al
     */
    public static String getProviderDisplayName(TranslationProvider provider) {
        switch (provider) {
            case DEEPL:
                return "DeepL";
            case GEMINI_AI:
                return "Gemini AI";
            case HYBRID_SMART:
                return "Akıllı Seçim";
            default:
                return "DeepL (Default)";
        }
    }
}