package testclient;

import java.util.Map;

/**
 * Command-line entry point.
 * <pre>
 *   java -cp target/Cosmic.jar;target/test-classes testclient.TestClientMain [scenario] [--host H] [--port P]
 *        [--user U] [--pass P] [--world W] [--channel C(1-based)]
 * </pre>
 * Exit code 0 and a final {@code SCENARIO <name> PASS ...} line on success; 1 and {@code FAIL} otherwise.
 */
public final class TestClientMain {
    private TestClientMain() {
    }

    public static void main(String[] args) {
        String name = "login-enter-map";
        String host = "127.0.0.1";
        int port = 8484;
        String user = "admin";
        String pass = "admin";
        String wz = "wz";
        int world = 0;
        int channel = 1;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--host" -> host = args[++i];
                case "--port" -> port = Integer.parseInt(args[++i]);
                case "--user" -> user = args[++i];
                case "--pass" -> pass = args[++i];
                case "--wz" -> wz = args[++i];
                case "--world" -> world = Integer.parseInt(args[++i]);
                case "--channel" -> channel = Integer.parseInt(args[++i]);
                case "--list" -> {
                    Scenarios.all().keySet().forEach(System.out::println);
                    return;
                }
                default -> name = args[i];
            }
        }

        Scenario scenario = Scenarios.all().get(name);
        if (scenario == null) {
            System.out.println("Unknown scenario '" + name + "'. Available: " + Scenarios.all().keySet());
            System.exit(2);
            return;
        }

        ScenarioContext ctx = new ScenarioContext(host, port, user, pass, world, channel - 1,
                msg -> System.out.println("[testclient] " + msg));
        ctx.wzPath = wz;
        try {
            String summary = scenario.run(ctx);
            System.out.println("SCENARIO " + name + " PASS " + summary);
        } catch (Throwable t) {
            System.out.println("SCENARIO " + name + " FAIL " + t);
            t.printStackTrace(System.out);
            System.exit(1);
        }
    }
}
