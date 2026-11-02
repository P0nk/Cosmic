package testclient;

/** A named end-to-end script against a running server. Register new ones in {@link Scenarios}. */
public interface Scenario {
    /** Run to completion; throw (AssertionError / IOException) on failure. Return a one-line result summary. */
    String run(ScenarioContext ctx) throws Exception;
}
