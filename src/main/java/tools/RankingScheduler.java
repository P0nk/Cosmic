package tools;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;

public class RankingScheduler {

    public static void start() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        long initialDelay = computeNextMidnightDelay();

//         long initialDelay = computeNextMidnightDelay();
//        long initialDelay = 1000 * 10; // Run 10 seconds after server start
        long period = 24 * 60 * 60 * 1000; // 24 hours

        // The Announcer grabs the URL from EnvLoader internally now
        scheduler.scheduleAtFixedRate(
                new DailyRankingAnnouncer(),
                initialDelay,
                period,
                TimeUnit.MILLISECONDS
        );

        System.out.println("[RankingScheduler] Leaderboards scheduled in " + (initialDelay / 1000 / 60) + " minutes.");
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