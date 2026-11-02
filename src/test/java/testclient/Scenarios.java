package testclient;

import java.util.LinkedHashMap;
import java.util.Map;

/** Registry of scenarios runnable by name: {@code TestClientMain <name>}. Add a line here for each new scenario. */
public final class Scenarios {
    private static final Map<String, Scenario> ALL = new LinkedHashMap<>();

    static {
        ALL.put("login-enter-map", new LoginEnterMapScenario());
    }

    private Scenarios() {
    }

    public static Map<String, Scenario> all() {
        return ALL;
    }
}
