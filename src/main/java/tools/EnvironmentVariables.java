package tools;

import java.util.Map;

/**
 * Wrapper class for accessing environment variables.
 *
 * Using this class instead of calling built-in functions like `System.getenv()`
 * make it possible to mock environment variables in tests.
 *
 * Note that environment variables should only be accessed in limited places,
 * like configuration loading. In most places, you should use config instead
 * of environment variables.
 */
public class EnvironmentVariables {
    private static EnvironmentVariables instance;

    private EnvironmentVariables() {}

    public static synchronized EnvironmentVariables instance() {
        if (instance == null) {
            instance = new EnvironmentVariables();
        }

        return instance;
    }

    public static void setInstance(EnvironmentVariables instance) {
        EnvironmentVariables.instance = instance;
    }

    public Map<String, String> getAll() {
        return System.getenv();
    }
}
