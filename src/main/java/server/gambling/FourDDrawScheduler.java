package server.gambling;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public class FourDDrawScheduler implements Runnable {

    private static final LocalTime DRAW_TIME = LocalTime.of(0, 0); // 12AM
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Random random = new Random();

    public static void init() {
        System.out.println("[4D Scheduler] Initializing...");
        scheduleNextDraw();
    }

    private static void scheduleNextDraw() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime drawTimeToday = LocalDate.now().atTime(DRAW_TIME);
        LocalDateTime nextRun = now.isBefore(drawTimeToday) ? drawTimeToday : drawTimeToday.plusDays(1);
        long delayMillis = Duration.between(now, nextRun).toMillis();
        System.out.println("[4D Scheduler] Next draw: " + nextRun);
        scheduler.schedule(new FourDDrawScheduler(), delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run() {
        try {
            LocalDate today = LocalDate.now();
            if (!FourDResultManager.hasDrawToday(today)) {
                performDraw(today);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scheduleNextDraw();
        }
    }

    private static void performDraw(LocalDate date) {
        System.out.println("[4D Scheduler] Generating draw for " + date);
        String first = generate4D();
        String second = generate4D();
        String third = generate4D();
        List<String> starters = generateUniqueNumbers(10);
        List<String> consolations = generateUniqueNumbers(10);

        FourDResultManager.storeDraw(date, first, second, third, starters, consolations);
        FourDResultManager.evaluateBets(date);
    }

    private static String generate4D() {
        return String.format("%04d", random.nextInt(10000));
    }

    private static List<String> generateUniqueNumbers(int count) {
        Set<String> set = new LinkedHashSet<>();
        while (set.size() < count) set.add(generate4D());
        return new ArrayList<>(set);
    }

    public static LocalDate getNextDrawDate() {
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(LocalDate.now().atTime(DRAW_TIME)) ? LocalDate.now() : LocalDate.now().plusDays(1);
    }

    public static void forceDrawToday() {
        LocalDate today = LocalDate.now();
        if (!FourDResultManager.hasDrawToday(today)) performDraw(today);
    }
}