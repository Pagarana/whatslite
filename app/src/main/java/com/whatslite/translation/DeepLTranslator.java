package com.whatslite.translation;

import android.util.Log;

import androidx.annotation.Nullable;

import com.whatslite.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Basit DeepL istemcisi (form-encoded). */
public class DeepLTranslator {

    private static final MediaType FORM = MediaType.get("application/x-www-form-urlencoded; charset=utf-8");
    private final OkHttpClient http = new OkHttpClient();

    /**
     * @param text Çevrilecek metin
     * @param targetLang Örn: "TR", "EN", "DE"
     * @param sourceLang (ops.) Örn: "EN" – boş bırakılırsa otomatik algılar
     * @return Çevrilmiş metin; hata halinde null
     */
    public @Nullable String translate(String text, String targetLang, @Nullable String sourceLang) {
        try {
            String key  = BuildConfig.DEEPL_API_KEY != null ? BuildConfig.DEEPL_API_KEY : "";
            String base = BuildConfig.DEEPL_BASE_URL != null ? BuildConfig.DEEPL_BASE_URL : "https://api-free.deepl.com";
            if (key.isEmpty() || text == null || text.trim().isEmpty()) return null;

            StringBuilder sb = new StringBuilder();
            sb.append("auth_key=").append(url(key));
            sb.append("&text=").append(url(text));
            sb.append("&target_lang=").append(url(targetLang.toUpperCase()));
            if (sourceLang != null && !sourceLang.trim().isEmpty()) {
                sb.append("&source_lang=").append(url(sourceLang.toUpperCase()));
            }

            Request req = new Request.Builder()
                    .url(base + "/v2/translate")
                    .post(RequestBody.create(sb.toString(), FORM))
                    .build();

            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    Log.e("DeepL", "HTTP " + resp.code() + ": " + (resp.body()!=null?resp.body().string():""));
                    return null;
                }
                String body = resp.body() != null ? resp.body().string() : "";
                JSONObject json = new JSONObject(body);
                JSONArray arr = json.getJSONArray("translations");
                if (arr.length() == 0) return null;
                return arr.getJSONObject(0).getString("text");
            }
        } catch (Exception e) {
            Log.e("DeepL", "translate error", e);
            return null;
        }
    }

    private static String url(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return ""; }
    }
}
