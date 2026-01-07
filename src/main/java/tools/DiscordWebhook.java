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
                // [FIX] Changed 'true' to 'false' here.
                // This tells 'post' to wrap the plain text in JSON {"content": "..."}
                post(webhookUrl, content, false);
            } catch (Exception e) {
                System.err.println("[DiscordWebhook] Failed: " + e.getMessage());
            }
        });
    }

    /**
     * Sends a rich Embed message (Requires raw JSON).
     */
    public static void sendEmbedAsync(String webhookUrl, String payloadJson) {
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        EXECUTOR.submit(() -> {
            try {
                // Embeds are already JSON, so we pass 'true'
                post(webhookUrl, payloadJson, true);
            } catch (Exception e) {
                System.err.println("[DiscordWebhook] Failed to send embed: " + e.getMessage());
            }
        });
    }

    private static void post(String webhookUrl, String data, boolean isJsonRaw) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setConnectTimeout(5000);
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json");

        // If it's raw JSON (Embeds), send as is.
        // If it's plain text (Old Method), wrap it in "content".
        String payload = isJsonRaw ? data : "{\"content\":\"" + escape(data) + "\"}";

        try (OutputStream os = con.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        con.getResponseCode();
        con.disconnect();
    }

    public static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}