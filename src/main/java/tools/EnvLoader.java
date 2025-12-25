package tools;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public final class EnvLoader {
    private static final Map<String, String> ENV = new HashMap<>();
    private static volatile boolean loaded = false;

    private EnvLoader() {}

    public static void load() {
        if (loaded) return;
        loaded = true;

        File envFile = new File(".env");
        if (!envFile.exists()) {
            System.out.println("[ENV] No .env file found (skipping)");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int idx = line.indexOf('=');
                if (idx <= 0) continue;

                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                ENV.put(key, value);
            }
            System.out.println("[ENV] Loaded " + ENV.size() + " entries from .env");
        } catch (Exception e) {
            System.err.println("[ENV] Failed to load .env");
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        String sys = System.getenv(key);
        if (sys != null && !sys.trim().isEmpty()) return sys.trim();

        String v = ENV.get(key);
        return (v == null || v.trim().isEmpty()) ? null : v.trim();
    }
}
