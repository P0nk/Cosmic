package server.bossshop;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Runnable-based scheduler for resetting daily purchase limits for Boss Shop materials.
 */
public class BossShopScheduler implements Runnable {

    private static final LocalTime RESET_TIME = LocalTime.of(0, 0); // 12AM GMT+8 (midnight)
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Define the Map to hold the material purchase limits
    private static final Map<Integer, Integer> materialPurchaseLimits = new HashMap<>();

    // Define the constants
    private static final int MAX_PLAYERS = 1000;  // Max players in the server (adjust if needed)

    // Number of materials (you can change this as you add more materials)
    private static final int MAX_MATERIALS = 6;   // Max materials (now including 6 materials)

    // Define the array to track the daily purchase counts
    private static int[][] dailyPurchaseCounts = new int[MAX_PLAYERS][MAX_MATERIALS];

    static {
        // Configuring the material purchase limits
        materialPurchaseLimits.put(4001017, 10);  // Zakum Eye of Fire (10 purchases per day)
        materialPurchaseLimits.put(4021032, 10);  // Horntail Mana Crystal (10 purchases per day)
        materialPurchaseLimits.put(4001189, 10);  // Pink Poppin (Pink Bean) (10 purchases per day)
        materialPurchaseLimits.put(4001694, 10);  // Von Leon Ticket (10 purchases per day)
    }

    /**
     * Starts the recurring reset scheduler (call on server startup).
     */
    public static void init() {
        System.out.println("[BossShop Scheduler] Initializing...");
        scheduleNextReset();
    }

    /**
     * Schedules the next reset task based on RESET_TIME.
     */
    private static void scheduleNextReset() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime resetTimeToday = LocalDate.now().atTime(RESET_TIME);
        LocalDateTime nextRun = now.isBefore(resetTimeToday) ? resetTimeToday : resetTimeToday.plusDays(1);

        long delayMillis = Duration.between(now, nextRun).toMillis();

        // Debugging: print the next reset time
        System.out.println("[BossShop Scheduler] Next reset scheduled for " + nextRun + " (" + delayMillis + " ms from now)");

        scheduler.schedule(new BossShopScheduler(), delayMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the purchase limit for a given material.
     * @param materialId The material's ID (e.g., 4001017 for Zakum Eye of Fire).
     * @return The purchase limit for the material.
     */
    public static int getMaterialPurchaseLimit(int materialId) {
        int limit = materialPurchaseLimits.getOrDefault(materialId, 0);  // Default to 0 if not found

        // Debugging: print material purchase limit
        System.out.println("[BossShop Scheduler] Material ID " + materialId + " Purchase Limit: " + limit);

        return limit;
    }

    /**
     * Called when the scheduler executes. Resets the daily purchase limits for all players and re-schedules itself.
     */
    @Override
    public void run() {
        try {
            LocalDate today = LocalDate.now();
            System.out.println("[BossShop Scheduler] Resetting daily purchase limits for " + today + "...");

            // Debugging: print when the reset process starts
            System.out.println("[BossShop Scheduler] Starting daily reset process...");

            // Reset the daily purchase counts for all players (reset logic)
            resetDailyPurchaseCounts();

            // Debugging: print after resetting purchase counts
            System.out.println("[BossShop Scheduler] ✅ Daily purchase counts have been reset for " + today);

        } catch (Exception e) {
            System.err.println("[BossShop Scheduler] ❌ Reset execution error:");
            e.printStackTrace();
        } finally {
            scheduleNextReset();
        }
    }

    /**
     * Resets the daily purchase counts for all players.
     * This method will reset the purchase limits for each material sold by the NPC.
     */
    public static void resetDailyPurchaseCounts() {
        // Debugging: print before resetting daily purchase counts
        System.out.println("[BossShop Scheduler] Resetting all daily purchase counts...");

        // Reset the purchase counts for all players (replace with actual logic)
        for (int i = 0; i < MAX_PLAYERS; i++) {
            for (int j = 0; j < MAX_MATERIALS; j++) {
                dailyPurchaseCounts[i][j] = 0; // Reset all purchase counts for each material
            }
        }

        // Debugging: print after resetting the purchase counts
        System.out.println("[BossShop Scheduler] All daily purchase counts have been reset.");
    }

    /**
     * Returns the next reset date: today if before reset time, otherwise tomorrow.
     */
    public static LocalDate getNextResetDate() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate nextResetDate = now.isBefore(LocalDate.now().atTime(RESET_TIME)) ? LocalDate.now() : LocalDate.now().plusDays(1);

        // Debugging: print the next reset date
        System.out.println("[BossShop Scheduler] Next reset date: " + nextResetDate);

        return nextResetDate;
    }
}
