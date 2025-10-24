package com.whatslite.service;

import android.content.Context;

public final class DeepLTranslationService {
    private DeepLTranslationService(){}

    public interface Callback {
        void onSuccess(String translated);
        void onError(Throwable t);
    }

    public static void translateAsync(Context ctx, String text, String from, String to, Callback cb) {
        // Stub: gerçek entegrasyon yerine aynısını döndürür
        try { cb.onSuccess(text); } catch (Exception e) { cb.onError(e); }
    }
}
