package server.gambling;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Runnable-based scheduler for daily 4D draws.
 */
public class FourDDrawScheduler implements Runnable {

    private static final LocalTime DRAW_TIME = LocalTime.of(0, 0); // 12AM GMT+8
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Random random = new Random();

    /**
     * Starts the recurring draw scheduler (call on server startup).
     */
    public static void init() {
        System.out.println("[4D Scheduler] Initializing...");
        scheduleNextDraw();
    }

    /**
     * Schedules the next draw task based on DRAW_TIME.
     */
    private static void scheduleNextDraw() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime drawTimeToday = LocalDate.now().atTime(DRAW_TIME);
        LocalDateTime nextRun = now.isBefore(drawTimeToday) ? drawTimeToday : drawTimeToday.plusDays(1);

        long delayMillis = Duration.between(now, nextRun).toMillis();

        System.out.println("[4D Scheduler] Next draw scheduled for " + nextRun + " (" + delayMillis + " ms from now)");

        scheduler.schedule(new FourDDrawScheduler(), delayMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Called when the scheduler executes. Performs draw and re-schedules itself.
     */
    @Override
    public void run() {
        try {
            LocalDate today = LocalDate.now();
            System.out.println("[4D Scheduler] Attempting draw for " + today + "...");

            if (!FourDResultManager.hasDrawToday(today)) {
                performDraw(today);
                System.out.println("[4D Scheduler] ✅ Draw completed for " + today);
            } else {
                System.out.println("[4D Scheduler] ⚠ Draw already exists for " + today);
            }
        } catch (Exception e) {
            System.err.println("[4D Scheduler] ❌ Draw execution error:");
            e.printStackTrace();
        } finally {
            scheduleNextDraw();
        }
    }

    /**
     * Performs a draw and evaluates winning bets.
     */
    private static void performDraw(LocalDate date) {
        System.out.println("[4D Scheduler] Generating numbers for draw: " + date);

        String first = generate4D();
        String second = generate4D();
        String third = generate4D();
        List<String> starters = generateUniqueNumbers(10);
        List<String> consolations = generateUniqueNumbers(10);

        System.out.println("[4D Scheduler] ➤ 1st Prize: " + first);
        System.out.println("[4D Scheduler] ➤ 2nd Prize: " + second);
        System.out.println("[4D Scheduler] ➤ 3rd Prize: " + third);
        System.out.println("[4D Scheduler] ➤ Starters: " + String.join(", ", starters));
        System.out.println("[4D Scheduler] ➤ Consolations: " + String.join(", ", consolations));

        FourDResultManager.storeDraw(date, first, second, third, starters, consolations);
        System.out.println("[4D Scheduler] Evaluating winning bets for " + date + "...");
        FourDResultManager.evaluateBets(date);
        System.out.println("[4D Scheduler] Evaluation complete.");
    }

    /**
     * Generates a 4-digit number (padded).
     */
    private static String generate4D() {
        return String.format("%04d", random.nextInt(10000));
    }

    /**
     * Generates a unique list of 4-digit numbers.
     */
    private static List<String> generateUniqueNumbers(int count) {
        Set<String> set = new LinkedHashSet<>();
        while (set.size() < count) {
            set.add(generate4D());
        }
        return new ArrayList<>(set);
    }

    /**
     * Returns draw date: today if before draw time, otherwise tomorrow.
     */
    public static LocalDate getNextDrawDate() {
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(LocalDate.now().atTime(DRAW_TIME)) ? LocalDate.now() : LocalDate.now().plusDays(1);
    }

    /**
     * Returns previous draw date.
     */
    public static LocalDate getLastDrawDate() {
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(LocalDate.now().atTime(DRAW_TIME)) ? LocalDate.now().minusDays(1) : LocalDate.now();
    }

    /**
     * Manually triggers draw (GM command).
     */
    public static void forceDrawToday() {
        LocalDate today = LocalDate.now();
        System.out.println("[4D Scheduler] Manual draw invoked by GM for " + today);

        if (!FourDResultManager.hasDrawToday(today)) {
            performDraw(today);
            System.out.println("[4D Scheduler] ✅ Manual draw completed.");
        } else {
            System.out.println("[4D Scheduler] ⚠ Draw already exists for today: " + today);
        }
    }

    /**
     * Stops all scheduled tasks.
     */
    public static void shutdown() {
        scheduler.shutdownNow();
        System.out.println("[4D Scheduler] Scheduler shutdown.");
    }
}