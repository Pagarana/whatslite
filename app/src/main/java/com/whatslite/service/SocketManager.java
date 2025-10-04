package com.whatslite.service;

import android.util.Log;
import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketManager {
    private static final String TAG = "SocketManager";
    private Socket socket;

    public void connect(String url) {
        try {
            socket = IO.socket(url);
            socket.on(Socket.EVENT_CONNECT, args -> Log.d(TAG, "Connected"));
            socket.connect();
        } catch (Exception e) {
            Log.e(TAG, "Socket connect error", e);
        }
    }

    public void disconnect() {
        try {
            if (socket != null) socket.disconnect();
        } catch (Exception ignored) {}
    }
}
