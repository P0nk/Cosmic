package tools;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;

public class RankingScheduler {

    public static void start() {
        // Using a pool of 2 to allow both tasks to potentially overlap if needed,
        // though 1 would work since they are offset by a minute.
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        long initialDelay = computeNextMidnightDelay();
        long period = 24 * 60 * 60 * 1000; // 24 hours
        long oneMinuteOffset = 60 * 1000;

        // 1. Schedule Scania to run at Midnight
        scheduler.scheduleAtFixedRate(
                new DailyRankingAnnouncer("Scania"),
                initialDelay,
                period,
                TimeUnit.MILLISECONDS
        );

        // 2. Schedule Bera to run 1 minute after Scania
        scheduler.scheduleAtFixedRate(
                new DailyRankingAnnouncer("Bera"),
                initialDelay + oneMinuteOffset,
                period,
                TimeUnit.MILLISECONDS
        );

        System.out.println("[RankingScheduler] Scania scheduled in " + (initialDelay / 1000 / 60) + " minutes.");
        System.out.println("[RankingScheduler] Bera scheduled 1 minute after Scania.");
    }

    private static long computeNextMidnightDelay() {
        Calendar now = Calendar.getInstance();
        Calendar midnight = Calendar.getInstance();
        midnight.add(Calendar.DAY_OF_YEAR, 1);
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        return midnight.getTimeInMillis() - now.getTimeInMillis();
    }
}