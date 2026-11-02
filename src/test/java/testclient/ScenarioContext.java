package testclient;

import java.util.function.Consumer;

/** Everything a scenario needs; filled from command-line flags by {@link TestClientMain}. */
public final class ScenarioContext {
    public final String host;
    public final int loginPort;
    public final String account;
    public final String password;
    public final int world;
    public final int channel; // 0-based on the wire (channel 1 == 0)
    private final Consumer<String> log;

    public ScenarioContext(String host, int loginPort, String account, String password, int world, int channel,
                           Consumer<String> log) {
        this.host = host;
        this.loginPort = loginPort;
        this.account = account;
        this.password = password;
        this.world = world;
        this.channel = channel;
        this.log = log;
    }

    public void log(String msg) {
        log.accept(msg);
    }

    public Consumer<String> logger() {
        return log;
    }

    /** Throw a scenario failure if the condition does not hold. */
    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
