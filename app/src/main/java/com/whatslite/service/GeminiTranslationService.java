package com.whatslite.service;

import android.content.Context;

public final class GeminiTranslationService {
    private GeminiTranslationService(){}

    public interface Callback {
        void onSuccess(String translated);
        void onError(Throwable t);
    }

    public static boolean isConfigured() {
        // Stub: gerçek anahtar kontrolü burada yapılır
        return false;
    }

    public static void translateWithAI(Context ctx, String text, String from, String to, Callback cb) {
        // Stub: yapılandırılmadıysa hata verelim
        if (!isConfigured()) { cb.onError(new IllegalStateException("Gemini not configured")); return; }
        cb.onSuccess(text);
    }
}
