package server.buffnpc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import tools.DatabaseConnection;
import net.packet.Packet;
import net.server.Server;
import net.server.world.World;
import tools.PacketCreator;
import server.TimerManager;

public class MapleLeafWorldBuffManager {

    // --- Configuration ---
    private static final int MAPLE_LEAF_ITEM_ID = 4001126;
    private static final int LEAVES_PER_HOUR = 5000;

    // NOTE: Comment previously said "40k"; value is 20k. Keeping value (dependency-safe), fixing comment.
    private static final int MAX_LEAF_CAP = 20000; // Cap storage at 20k (4 hours queued)

    // Use exact hour in ms (previous value had +4ms drift)
    private static final long ONE_HOUR_MS = 3600000L;

    // --- State ---
    // Represents leaves currently "in the bank" waiting to be used
    private static final AtomicInteger queuedLeaves = new AtomicInteger(0);

    // Timestamp when the CURRENT active boost expires
    private static volatile long currentBoostEndTime = 0L;

    // Track if timer is currently running to prevent double scheduling
    private static final AtomicBoolean isTimerRunning = new AtomicBoolean(false);

    // --- Key Alerts (rate-limited) ---
    private static volatile long lastDbAlertAt = 0L;
    private static volatile long lastBroadcastAlertAt = 0L;
    private static volatile long lastConfigAlertAt = 0L;

    private static final long ALERT_COOLDOWN_MS = 60_000L; // 1 min cooldown to avoid spam

    private static void alertDb(String msg, Throwable t) {
        long now = System.currentTimeMillis();
        if (now - lastDbAlertAt >= ALERT_COOLDOWN_MS) {
            lastDbAlertAt = now;
            System.err.println("[MapleLeafManager][DB] " + msg + (t != null ? " err=" + t.getMessage() : ""));
        }
    }

    private static void alertBroadcast(String msg, Throwable t) {
        long now = System.currentTimeMillis();
        if (now - lastBroadcastAlertAt >= ALERT_COOLDOWN_MS) {
            lastBroadcastAlertAt = now;
            System.err.println("[MapleLeafManager][BCAST] " + msg + (t != null ? " err=" + t.getMessage() : ""));
        }
    }

    private static void alertConfig(String msg) {
        long now = System.currentTimeMillis();
        if (now - lastConfigAlertAt >= ALERT_COOLDOWN_MS) {
            lastConfigAlertAt = now;
            System.err.println("[MapleLeafManager][CONFIG] " + msg);
        }
    }

    // Load data from SQL on startup
    static {
        // Key alert if cap is misconfigured
        if (MAX_LEAF_CAP < LEAVES_PER_HOUR) {
            alertConfig("MAX_LEAF_CAP (" + MAX_LEAF_CAP + ") is less than LEAVES_PER_HOUR (" + LEAVES_PER_HOUR + "). Boost may never start.");
        }
        loadDonationData();

        // Resume logic: If server restarted with queued leaves, start if not active
        if (queuedLeaves.get() >= LEAVES_PER_HOUR && currentBoostEndTime < System.currentTimeMillis()) {
            checkAndConsume();
        }
    }

    // --- Getters for NPC ---

    public static int getQueuedLeaves() {
        return queuedLeaves.get();
    }

    public static int getMaxCap() {
        return MAX_LEAF_CAP;
    }

    // Calculates Total Time = (Time left on current buff) + (Hours stored in bank)
    public static long getTotalTimeRemaining() {
        long now = System.currentTimeMillis();
        long activeTimeLeft = Math.max(0L, currentBoostEndTime - now);

        // Calculate how many full hours are waiting in the queue
        int hoursInQueue = queuedLeaves.get() / LEAVES_PER_HOUR;
        long queuedTime = hoursInQueue * ONE_HOUR_MS;

        return activeTimeLeft + queuedTime;
    }

    // Check if player can donate specific amount without overflowing cap
    public static boolean canDonate(int amount) {
        if (amount <= 0) return false;
        return (queuedLeaves.get() + amount) <= MAX_LEAF_CAP;
    }

    // --- Main Logic ---

    public static void handleDonation(int amountDonated) {
        if (amountDonated <= 0) {
            // Key alert: bad input (should never happen from NPC)
            alertConfig("handleDonation called with non-positive amount: " + amountDonated);
            return;
        }

        // Ensure we never exceed cap (NPC should call canDonate, but this is safety)
        int before, after;
        do {
            before = queuedLeaves.get();
            int proposed = before + amountDonated;
            if (proposed > MAX_LEAF_CAP) {
                // Key alert: cap overflow attempt
                alertConfig("Donation would exceed cap. before=" + before + " donate=" + amountDonated + " cap=" + MAX_LEAF_CAP);
                return;
            }
            after = proposed;
        } while (!queuedLeaves.compareAndSet(before, after));

        saveDonationData();

        // If boost isn't active, try to start immediately
        if (currentBoostEndTime < System.currentTimeMillis()) {
            checkAndConsume();
        }
    }

    // Checks if we have enough to start/extend a round
    private static void checkAndConsume() {
        if (queuedLeaves.get() >= LEAVES_PER_HOUR) {
            consumeLeavesAndStartRound();
        } else {
            // Not enough leaves to continue/start
            resetWorldSpawnBoost();
        }
    }

    private static void consumeLeavesAndStartRound() {
        // Atomically deduct one hour worth. Prevent negative.
        while (true) {
            int cur = queuedLeaves.get();
            if (cur < LEAVES_PER_HOUR) {
                // Not enough, stop
                resetWorldSpawnBoost();
                return;
            }
            if (queuedLeaves.compareAndSet(cur, cur - LEAVES_PER_HOUR)) {
                break;
            }
        }

        saveDonationData();

        // Extend time from whichever is later: now or existing end time
        long now = System.currentTimeMillis();
        long base = Math.max(now, currentBoostEndTime);
        currentBoostEndTime = base + ONE_HOUR_MS;

        activateWorldEffects();

        // Start timer loop only once
        if (isTimerRunning.compareAndSet(false, true)) {
            startTimerLoop();
        }
    }

    private static void startTimerLoop() {
        // Schedule check exactly when the CURRENT end time occurs (more accurate than fixed ONE_HOUR_MS)
        long now = System.currentTimeMillis();
        long delay = Math.max(0L, currentBoostEndTime - now);

        TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                // When timer fires, we decide whether to extend again
                if (queuedLeaves.get() >= LEAVES_PER_HOUR) {
                    broadcastWorldBoostMessage(0, "[System] The Maple Leaf Event has been extended for another hour!");
                    consumeLeavesAndStartRound(); // This will reschedule as needed
                } else {
                    isTimerRunning.set(false);
                    resetWorldSpawnBoost();
                }
            }
        }, delay);
    }

    private static void activateWorldEffects() {
        Server server = getServerInstance();
        if (server == null) {
            alertConfig("Server instance is null during activateWorldEffects().");
            return;
        }

        for (World world : server.getWorlds()) {
            world.setMobrate(3);
            world.setMobperspawnpoint(3);
        }
        // Keep behaviour: broadcast "active" (even if called on extensions)
        broadcastWorldBoostMessage(0, "[System] World spawn boost is ACTIVE! (Rates doubled).");
    }

    private static void resetWorldSpawnBoost() {
        currentBoostEndTime = 0L;
        isTimerRunning.set(false);

        Server server = getServerInstance();
        if (server == null) {
            alertConfig("Server instance is null during resetWorldSpawnBoost().");
            return;
        }

        for (World world : server.getWorlds()) {
            world.setMobrate(2);
            world.setMobperspawnpoint(2);
        }
        broadcastWorldBoostMessage(0, "[System] World spawn boost has ended. We need more leaves to restart!");
    }

    // --- Helper Methods ---

    // worldId param preserved for dependency safety (current implementation broadcasts globally)
    private static void broadcastWorldBoostMessage(int worldId, String message) {
        Packet packet = PacketCreator.serverNotice(6, message);
        try {
            // Keeping existing behaviour (global broadcast); worldId currently unused.
            Server.getInstance().broadcastMessage(5, packet);
        } catch (Exception e) {
            // Key alert: if broadcasts fail, you'd want to know
            alertBroadcast("Failed to broadcast message: " + message, e);
        }
    }

    private static Server getServerInstance() {
        return Server.getInstance();
    }

    // --- SQL Methods ---

    private static void loadDonationData() {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT total_donated FROM event_maple_leaf WHERE id = 1");
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int v = rs.getInt("total_donated");
                if (v < 0) {
                    alertDb("Loaded negative total_donated=" + v + " (forcing to 0).", null);
                    v = 0;
                }
                if (v > MAX_LEAF_CAP) {
                    alertDb("Loaded total_donated=" + v + " exceeds cap=" + MAX_LEAF_CAP + " (clamping).", null);
                    v = MAX_LEAF_CAP;
                }
                queuedLeaves.set(v);
            } else {
                // Key alert: missing row id=1 (system won't persist correctly)
                alertDb("No row found in event_maple_leaf where id=1. Donations will not persist.", null);
            }

        } catch (Exception e) {
            alertDb("Failed to load donation data.", e);
        }
    }

    private static void saveDonationData() {
        // Runs on every donation/deduction. If this becomes hot, we can batch later.
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE event_maple_leaf SET total_donated = ? WHERE id = 1")) {

            ps.setInt(1, queuedLeaves.get());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                // Key alert: row missing -> updates silently do nothing
                alertDb("UPDATE affected 0 rows for event_maple_leaf id=1. Persistence is broken.", null);
            }

        } catch (Exception e) {
            alertDb("Failed to save donation data.", e);
        }
    }
}
