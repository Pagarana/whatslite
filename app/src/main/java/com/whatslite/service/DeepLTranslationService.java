package com.whatslite.service;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class DeepLTranslationService {

    private static final String TAG = "DeepLService";

    private static final Map<String, String> LANG_CODE_MAPPING = new HashMap<String, String>() {{
        put("tr","TR");
        put("en","EN");
        put("de","DE");
        put("fr","FR");
        put("es","ES");
        put("ar","AR");
        put("ru","RU");
    }};

    public String translate(String apiKey, String text, String from, String to) throws Exception {
        String target = LANG_CODE_MAPPING.getOrDefault(to, to).toUpperCase();
        URL url = new URL("https://api-free.deepl.com/v2/translate");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Authorization", "DeepL-Auth-Key " + apiKey);
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setDoOutput(true);

        String body = "text=" + urlEncode(text) + "&target_lang=" + urlEncode(target);
        try (OutputStreamWriter w = new OutputStreamWriter(con.getOutputStream())) {
            w.write(body);
        }

        StringBuilder resp = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) resp.append(line);
        }

        JSONObject json = new JSONObject(resp.toString());
        JSONArray arr = json.getJSONArray("translations");
        String out = arr.getJSONObject(0).getString("text");
        Log.d(TAG, "DeepL ok");
        return out;
    }

    private static String urlEncode(String s) throws Exception {
        return java.net.URLEncoder.encode(s, "UTF-8");
    }

    public static boolean isLanguageSupported(String code) {
        return LANG_CODE_MAPPING.containsKey(code) || code != null;
    }
}
