package server.expeditions;

public class BossLogData {
    private int attempts;
    private int failures;
    private int completions;

    BossLogData() { }

    public void updateAttempts(boolean success) {
        attempts += 1;

        if (success) {
            completions += 1;
        } else {
            failures += 1;
        }
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public int getFailures() {
        return failures;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public int getCompletions() { return completions; }

    public void setCompletions(int completions) { this.completions = completions; }
}
