package server.events;

import net.server.Server;
import net.server.world.World;
import server.TimerManager;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.ScheduledFuture;

/**
 * DailyRestartScheduler
 *
 * Schedules a daily server restart at a fixed wall-clock time in Singapore Time
 * (UTC+8).
 * Countdown announcements are displayed via the scrolling server message bar:
 * T-60 min, T-30 min, T-15 min, T-5 min, T-1 min, then shutdown.
 *
 * Configuration:
 * RESTART_HOUR – hour of restart in 24h SGT (default: 10 = 10:00 AM)
 * RESTART_MINUTE – minute of restart (default: 0)
 */
public class DailyRestartScheduler {

    // ---- Configuration ----
    private static final int RESTART_HOUR = 10;
    private static final int RESTART_MINUTE = 0;
    private static final ZoneId SGT = ZoneId.of("Asia/Singapore");

    // ---- Singleton ----
    private static final DailyRestartScheduler instance = new DailyRestartScheduler();

    public static DailyRestartScheduler getInstance() {
        return instance;
    }

    private DailyRestartScheduler() {
    }

    // ---- State ----
    private ScheduledFuture<?> task60;
    private ScheduledFuture<?> task30;
    private ScheduledFuture<?> task15;
    private ScheduledFuture<?> task5;
    private ScheduledFuture<?> task1;
    private ScheduledFuture<?> taskShutdown;

    // ---- Public API ----

    public void start() {
        scheduleAll();
        System.out.println("[DailyRestartScheduler] Started. Next restart at " +
                RESTART_HOUR + ":" + String.format("%02d", RESTART_MINUTE) + " SGT.");
    }

    // ---- Internal ----

    /**
     * Computes milliseconds from now until the next occurrence of
     * RESTART_HOUR:RESTART_MINUTE SGT.
     * If the target time has already passed today, schedules for tomorrow.
     */
    private long millisUntilNextRestart() {
        ZonedDateTime now = ZonedDateTime.now(SGT);
        ZonedDateTime target = now.toLocalDate().atTime(RESTART_HOUR, RESTART_MINUTE)
                .atZone(SGT);
        if (!target.isAfter(now)) {
            target = target.plusDays(1);
        }
        return target.toInstant().toEpochMilli() - now.toInstant().toEpochMilli();
    }

    /**
     * Schedules all countdown tasks relative to the next restart time.
     * Tasks that are in the past (e.g. T-60 but reboot is in 20 min) are simply
     * skipped.
     */
    private void scheduleAll() {
        cancelAll();
        TimerManager tm = TimerManager.getInstance();
        long msToRestart = millisUntilNextRestart();

        // Helper: schedule a countdown message if there's still time left for it
        long ms60 = msToRestart - 60 * 60_000L;
        long ms30 = msToRestart - 30 * 60_000L;
        long ms15 = msToRestart - 15 * 60_000L;
        long ms5 = msToRestart - 5 * 60_000L;
        long ms1 = msToRestart - 60_000L;

        if (ms60 > 0)
            task60 = tm.schedule(() -> announce(
                    "⚠️ [Server Notice] The server will restart in 60 minutes. Please prepare to log out safely!"),
                    ms60);

        if (ms30 > 0)
            task30 = tm.schedule(() -> announce(
                    "⚠️ [Server Notice] Server restarting in 30 minutes! Finish up what you're doing."),
                    ms30);

        if (ms15 > 0)
            task15 = tm.schedule(() -> announce(
                    "⚠️ [Server Notice] Server restarting in 15 minutes! Please start logging out soon."),
                    ms15);

        if (ms5 > 0)
            task5 = tm.schedule(() -> announce(
                    "🚨 [Server Notice] Server restarting in 5 minutes! LOG OUT NOW to avoid losing progress!"),
                    ms5);

        if (ms1 > 0)
            task1 = tm.schedule(() -> announce(
                    "🚨 [Server Notice] Server restarting in 1 MINUTE! DISCONNECT NOW!"),
                    ms1);

        // The actual shutdown + reschedule for next day
        taskShutdown = tm.schedule(() -> {
            System.out.println("[DailyRestartScheduler] Triggering daily restart now.");
            Server.getInstance().shutdown(false).run();
            // The shutdown takes care of stopping the process; the OS/launch script
            // should restart the server. The scheduler will re-initialize on next boot.
        }, msToRestart);

        System.out.printf("[DailyRestartScheduler] Restart in %.1f hours.%n",
                msToRestart / 3_600_000.0);
    }

    /**
     * Broadcasts a message on the scrolling server message bar across all worlds.
     */
    private void announce(String message) {
        System.out.println("[DailyRestartScheduler] Announcing: " + message);
        for (World w : Server.getInstance().getWorlds()) {
            w.setServerMessage(message);
        }
    }

    private void cancelAll() {
        cancelQuiet(task60);
        cancelQuiet(task30);
        cancelQuiet(task15);
        cancelQuiet(task5);
        cancelQuiet(task1);
        cancelQuiet(taskShutdown);
    }

    private void cancelQuiet(ScheduledFuture<?> f) {
        if (f != null && !f.isDone()) {
            f.cancel(false);
        }
    }
}
