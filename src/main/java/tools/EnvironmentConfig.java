package tools;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Provides access to configuration values from .env files and system environment variables.
 * Priority order:
 * 1. .env file values
 * 2. System environment variables
 * 3. Provided default value
 */
public class EnvironmentConfig {
    private static final Dotenv dotenv;

    static {
        dotenv = Dotenv.configure()
                       .ignoreIfMissing()
                       .load();
    }

    /**
     * Get an environment variable value with fallback to a default.
     *
     * @param key the environment variable key
     * @param defaultValue the default value to use if the variable is not found
     * @return the environment variable value, or the default if not found
     */
    public static String get(String key, String defaultValue) {
        if (key == null || key.isEmpty()) {
            return defaultValue;
        }

        // First try .env file
        if (dotenv != null) {
            String value = dotenv.get(key);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }

        // Then try system environment
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        // Finally use the provided default
        return defaultValue;
    }
}
