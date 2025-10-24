package com.whatslite.service;

import android.util.Log;
import androidx.annotation.Nullable;
import com.whatslite.BuildConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.*;

public class DeepLClient {
    private static final MediaType FORM = MediaType.get("application/x-www-form-urlencoded; charset=utf-8");
    private final OkHttpClient http = new OkHttpClient();

    /** ör: targetLang "TR", sourceLang null bırak → otomatik algılar */
    public @Nullable String translate(String text, String targetLang, @Nullable String sourceLang) {
        try {
            String key  = BuildConfig.DEEPL_API_KEY == null ? "" : BuildConfig.DEEPL_API_KEY;
            String host = BuildConfig.DEEPL_API_HOST == null ? "api-free.deepl.com" : BuildConfig.DEEPL_API_HOST;
            if (key.isEmpty() || text == null || text.trim().isEmpty()) return null;

            StringBuilder sb = new StringBuilder();
            sb.append("auth_key=").append(url(textEncode(key)));
            sb.append("&text=").append(url(textEncode(text)));
            sb.append("&target_lang=").append(url(targetLang.toUpperCase()));
            if (sourceLang != null && !sourceLang.isEmpty()) {
                sb.append("&source_lang=").append(url(sourceLang.toUpperCase()));
            }
            Request req = new Request.Builder()
                    .url("https://" + host + "/v2/translate")
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
                JSONObject t0 = arr.getJSONObject(0);
                return t0.getString("text");
            }
        } catch (Exception e) {
            Log.e("DeepL", "translate error", e);
            return null;
        }
    }

    private static String url(String s) { try { return java.net.URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return ""; } }
    private static String textEncode(String s) { return s; }
}
