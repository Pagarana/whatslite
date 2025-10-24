package com.whatslite.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.whatslite.BuildConfig;
import com.whatslite.translation.DeepLTranslator;

public final class DeepLTranslationService {
    private DeepLTranslationService(){}

    public interface Callback {
        void onSuccess(String translated);
        void onError(Throwable t);
    }

    /** DeepL yapılandırması var mı? (debug için faydalı) */
    public static boolean isConfigured() {
        return BuildConfig.DEEPL_API_KEY != null && !BuildConfig.DEEPL_API_KEY.trim().isEmpty();
    }

    /**
     * DeepL ile asenkron çeviri.
     * @param text Çevrilecek metin (boşsa hata döner)
     * @param from Kaynak dil kodu (örn: "EN"), boş/null ise otomatik algılar
     * @param to   Hedef dil kodu (örn: "TR"); boş/null ise "EN" kullanılır
     */
    public static void translateAsync(Context ctx, String text, String from, String to, Callback cb) {
        if (cb == null) return;

        if (TextUtils.isEmpty(text)) {
            postError(cb, new IllegalArgumentException("Text is empty"));
            return;
        }
        if (!isConfigured()) {
            postError(cb, new IllegalStateException("DeepL API key is missing (see ~/.gradle/gradle.properties)"));
            return;
        }

        new Thread(() -> {
            try {
                DeepLTranslator client = new DeepLTranslator();

                // Kaynak/Hedef dilleri normalize et
                final String src = TextUtils.isEmpty(from) ? null : from.trim().toUpperCase();
                final String tgt = TextUtils.isEmpty(to)   ? "EN" : to.trim().toUpperCase();

                String out = client.translate(text, tgt, src);
                if (out == null) throw new RuntimeException("DeepL returned null response");

                postSuccess(cb, out);
            } catch (Throwable t) {
                Log.e("DeepLTranslationSvc", "translateAsync failed", t);
                postError(cb, t);
            }
        }).start();
    }

    // ---- helper’lar ----
    private static void postSuccess(Callback cb, String result) {
        new Handler(Looper.getMainLooper()).post(() -> cb.onSuccess(result));
    }

    private static void postError(Callback cb, Throwable t) {
        new Handler(Looper.getMainLooper()).post(() -> cb.onError(t));
    }
}
