package scripting.field.scheduler;

public class FieldScriptScheduleEntry {
    private Runnable runnable;
    private Long duration;

    public Runnable getRunnable() {
        return runnable;
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }
}
