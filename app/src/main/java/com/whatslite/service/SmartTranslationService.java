package com.whatslite.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public final class SmartTranslationService {
    private SmartTranslationService(){}

    private static final String PREFS = "whatslite_prefs";
    private static final String KEY_PROVIDER = "translation_provider";

    public enum TranslationProvider { DEEPL, GEMINI, AUTO }

    public interface Callback {
        void onSuccess(String translated);
        void onError(Throwable t);
    }

    // SettingsActivity kullanımına uygun setter
    public static void setPreferredProvider(Context ctx, TranslationProvider provider) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_PROVIDER, provider.name()).apply();
    }

    public static TranslationProvider getPreferredProvider(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            String v = sp.getString(KEY_PROVIDER, TranslationProvider.AUTO.name());
            return TranslationProvider.valueOf(v);
        } catch (Exception e) {
            return TranslationProvider.AUTO;
        }
    }

    public static void translate(Context context, String text, String from, String to, Callback cb) {
        TranslationProvider pref = getPreferredProvider(context);
        switch (pref) {
            case DEEPL:
                tryDeepLThenFailoverToGemini(context, text, from, to, cb, false);
                break;
            case GEMINI:
                tryGeminiThenFailoverToDeepL(context, text, from, to, cb, false);
                break;
            case AUTO:
            default:
                // Önce DeepL dene, olmazsa Gemini
                tryDeepLThenFailoverToGemini(context, text, from, to, cb, true);
                break;
        }
    }

    private static void tryDeepLThenFailoverToGemini(Context ctx, String text, String from, String to, Callback cb, boolean allowFailover) {
        try {
            DeepLTranslationService.translateAsync(ctx, text, from, to, new DeepLTranslationService.Callback() {
                @Override public void onSuccess(String translated) { cb.onSuccess(translated); }
                @Override public void onError(Throwable t) {
                    Log.w("SmartTranslation", "DeepL failed", t);
                    if (allowFailover) tryGemini(ctx, text, from, to, cb);
                    else cb.onError(t);
                }
            });
        } catch (Throwable t) {
            if (allowFailover) tryGemini(ctx, text, from, to, cb); else cb.onError(t);
        }
    }

    private static void tryGeminiThenFailoverToDeepL(Context ctx, String text, String from, String to, Callback cb, boolean allowFailover) {
        if (GeminiTranslationService.isConfigured()) {
            GeminiTranslationService.translateWithAI(ctx, text, from, to, new GeminiTranslationService.Callback() {
                @Override public void onSuccess(String translated) { cb.onSuccess(translated); }
                @Override public void onError(Throwable t) {
                    if (allowFailover) {
                        tryDeepLThenFailoverToGemini(ctx, text, from, to, cb, false);
                    } else cb.onError(t);
                }
            });
        } else {
            if (allowFailover) {
                tryDeepLThenFailoverToGemini(ctx, text, from, to, cb, false);
            } else {
                cb.onError(new IllegalStateException("Gemini not configured"));
            }
        }
    }

    private static void tryGemini(Context ctx, String text, String from, String to, Callback cb) {
        if (GeminiTranslationService.isConfigured()) {
            GeminiTranslationService.translateWithAI(ctx, text, from, to, new GeminiTranslationService.Callback() {
                @Override public void onSuccess(String translated) { cb.onSuccess(translated); }
                @Override public void onError(Throwable t) { cb.onError(t); }
            });
        } else {
            cb.onError(new IllegalStateException("No translator configured"));
        }
    }
}
