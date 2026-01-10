package server.voting;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class VoteScheduler {
    private static final Logger log = Logger.getLogger(VoteScheduler.class.getName());
    // Increased thread pool size to 2 just in case tasks overlap (unlikely, but safer)
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public static void start() {
        ZoneId sgtZone = ZoneId.of("Asia/Singapore");
        ZonedDateTime now = ZonedDateTime.now(sgtZone);

        scheduleTask(now, 7, 55, 0, "Main Pull");
        scheduleTask(now, 7, 59, 30, "Last-Minute Catch");
    }

    private static void scheduleTask(ZonedDateTime now, int hour, int min, int sec, String taskName) {
        ZonedDateTime nextRun = now.withHour(hour).withMinute(min).withSecond(sec);

        // If the time has passed for today, schedule for tomorrow
        if (now.compareTo(nextRun) > 0) {
            nextRun = nextRun.plusDays(1);
        }

        long initialDelay = Duration.between(now, nextRun).getSeconds();

        // Schedule to run every 24 hours
        scheduler.scheduleAtFixedRate(new GTopVoteTask(), initialDelay, 24 * 60 * 60, TimeUnit.SECONDS);

        log.info("[VoteScheduler] " + taskName + " scheduled in " + (initialDelay / 60) + " minutes (" + hour + ":" + min + ":" + sec + " SGT).");
    }
}