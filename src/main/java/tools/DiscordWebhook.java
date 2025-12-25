package tools;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DiscordWebhook {

    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "DiscordWebhook");
                t.setDaemon(true);
                return t;
            });

    private DiscordWebhook() {}

    public static void sendAsync(String webhookUrl, String content) {
        if (webhookUrl == null || webhookUrl.isEmpty()) return;
        if (content == null || content.isEmpty()) return;

        EXECUTOR.submit(() -> {
            try {
                post(webhookUrl, content);
            } catch (Exception e) {
                System.err.println("[DiscordWebhook] Failed: " + e.getMessage());
            }
        });
    }

    private static void post(String webhookUrl, String content) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json");

        String safe = content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ");

        String json = "{\"content\":\"" + safe + "\"}";
        try (OutputStream os = con.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        con.getResponseCode(); // trigger send
        con.disconnect();
    }
}
