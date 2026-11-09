package com.soverixnet.soveravpn;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class ApiClient {
    interface Callback {
        void complete(JSONObject response, String error);
    }

    private final ExecutorService queue = Executors.newSingleThreadExecutor();
    private final String baseUrl;

    ApiClient(String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
    }

    boolean isConfigured() {
        return baseUrl.startsWith("https://") && baseUrl.length() > 9;
    }

    void request(String path, String method, JSONObject body, String token, Callback callback) {
        if (!isConfigured()) {
            callback.complete(null, "Cloudflare API is not configured yet.");
            return;
        }
        queue.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestMethod(method);
                connection.setRequestProperty("Accept", "application/json");
                if (token != null) connection.setRequestProperty("Authorization", "Bearer " + token);
                if (body != null) {
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json");
                    byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                    try (OutputStream out = connection.getOutputStream()) { out.write(bytes); }
                }
                int code = connection.getResponseCode();
                InputStream in = code < 400 ? connection.getInputStream() : connection.getErrorStream();
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] chunk = new byte[4096];
                int count;
                if (in != null) try (InputStream stream = in) {
                    while ((count = stream.read(chunk)) >= 0) {
                        if (bytes.size() > 65536) throw new IllegalStateException("Unexpected response size");
                        bytes.write(chunk, 0, count);
                    }
                }
                JSONObject response = new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                if (code >= 400) callback.complete(null, response.optString("error", "Request failed"));
                else callback.complete(response, null);
            } catch (Exception error) {
                callback.complete(null, error.getClass().getSimpleName() + ": " + error.getMessage());
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }
}
