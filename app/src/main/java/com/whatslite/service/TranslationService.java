package com.whatslite.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class TranslationService {

    public interface Callback {
        void onSuccess(String translated);
        void onError(String message);
    }

    public static void translateAsync(Context ctx, String text, String from, String to, Callback cb) {
        new Thread(() -> {
            try {
                String translated = translate(text, from, to);
                new Handler(Looper.getMainLooper()).post(() -> cb.onSuccess(translated));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> cb.onError(e.getMessage()));
            }
        }).start();
    }

    private static String translate(String text, String from, String to) throws Exception {
        String urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&dt=t&sl="
                + URLEncoder.encode(from, "UTF-8")
                + "&tl=" + URLEncoder.encode(to, "UTF-8")
                + "&q=" + URLEncoder.encode(text, "UTF-8");

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(15000);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);

        int code = conn.getResponseCode();
        if (code != 200) throw new RuntimeException("HTTP " + code);

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        for (String line; (line = br.readLine()) != null; ) sb.append(line);
        br.close();
        conn.disconnect();

        JSONArray outer = new JSONArray(sb.toString());
        JSONArray arr = outer.getJSONArray(0);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < arr.length(); i++) {
            out.append(arr.getJSONArray(i).getString(0));
        }
        return out.toString();
    }
}
