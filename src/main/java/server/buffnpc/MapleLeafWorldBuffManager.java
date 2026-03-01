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

    // Shifted from 5000/hr to 84/min (~5040 an hour) for safer server restarts
    private static final int LEAVES_PER_MINUTE = 84;
    private static final int MAX_LEAF_CAP = 20000;
    private static final long ONE_MINUTE_MS = 60000L;

    // --- State ---
    private static final AtomicInteger queuedLeaves = new AtomicInteger(0);
    private static volatile long currentBoostEndTime = 0L;
    private static final AtomicBoolean isTimerRunning = new AtomicBoolean(false);

    // --- Key Alerts (rate-limited) ---
    private static volatile long lastDbAlertAt = 0L;
    private static volatile long lastBroadcastAlertAt = 0L;
    private static volatile long lastConfigAlertAt = 0L;
    private static final long ALERT_COOLDOWN_MS = 60_000L;

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

    static {
        loadDonationData();

        // Resume logic: Start immediately if we have at least 1 minute of leaves saved
        if (queuedLeaves.get() >= LEAVES_PER_MINUTE && currentBoostEndTime < System.currentTimeMillis()) {
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

    public static long getTotalTimeRemaining() {
        long now = System.currentTimeMillis();
        long activeTimeLeft = Math.max(0L, currentBoostEndTime - now);

        // Calculate how many full minutes are waiting in the queue
        int minutesInQueue = queuedLeaves.get() / LEAVES_PER_MINUTE;
        long queuedTime = minutesInQueue * ONE_MINUTE_MS;

        return activeTimeLeft + queuedTime;
    }

    public static boolean canDonate(int amount) {
        if (amount <= 0) return false;
        return (queuedLeaves.get() + amount) <= MAX_LEAF_CAP;
    }

    // --- Main Logic ---
    public static void handleDonation(int amountDonated) {
        if (amountDonated <= 0) {
            alertConfig("handleDonation called with non-positive amount: " + amountDonated);
            return;
        }

        int before, after;
        do {
            before = queuedLeaves.get();
            int proposed = before + amountDonated;
            if (proposed > MAX_LEAF_CAP) {
                alertConfig("Donation would exceed cap. before=" + before + " donate=" + amountDonated + " cap=" + MAX_LEAF_CAP);
                return;
            }
            after = proposed;
        } while (!queuedLeaves.compareAndSet(before, after));

        saveDonationData();

        if (currentBoostEndTime < System.currentTimeMillis()) {
            checkAndConsume();
        }
    }

    private static void checkAndConsume() {
        if (queuedLeaves.get() >= LEAVES_PER_MINUTE) {
            if (isTimerRunning.compareAndSet(false, true)) {
                // Pass 'true' to indicate this is the start of a fresh run (triggers broadcast)
                consumeLeavesAndStartRound(true);
            }
        } else {
            resetWorldSpawnBoost();
        }
    }

    private static void consumeLeavesAndStartRound(boolean isFirstStart) {
        while (true) {
            int cur = queuedLeaves.get();
            if (cur < LEAVES_PER_MINUTE) {
                resetWorldSpawnBoost();
                return;
            }
            if (queuedLeaves.compareAndSet(cur, cur - LEAVES_PER_MINUTE)) {
                break;
            }
        }

        saveDonationData(); // Saves to DB every minute, minimizing restart losses!

        long now = System.currentTimeMillis();
        long base = Math.max(now, currentBoostEndTime);
        currentBoostEndTime = base + ONE_MINUTE_MS;

        Server server = getServerInstance();
        if (server != null) {
            for (World world : server.getWorlds()) {
                world.setMobrate(3);
                world.setMobperspawnpoint(3);
            }
        }

        // Only broadcast if we are officially kicking off the event
        if (isFirstStart) {
            broadcastWorldBoostMessage(0, "[System] World spawn boost is ACTIVE! (Rates doubled).");
        }

        startTimerLoop(); // Schedule the next minute's check
    }

    private static void startTimerLoop() {
        long now = System.currentTimeMillis();
        long delay = Math.max(0L, currentBoostEndTime - now);

        TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (queuedLeaves.get() >= LEAVES_PER_MINUTE) {
                    consumeLeavesAndStartRound(false); // false = don't broadcast spam
                } else {
                    resetWorldSpawnBoost();
                }
            }
        }, delay);
    }

    private static void resetWorldSpawnBoost() {
        currentBoostEndTime = 0L;
        isTimerRunning.set(false);

        Server server = getServerInstance();
        if (server != null) {
            for (World world : server.getWorlds()) {
                world.setMobrate(2);
                world.setMobperspawnpoint(2);
            }
        }
        broadcastWorldBoostMessage(0, "[System] World spawn boost has ended. We need more leaves to restart!");
    }

    private static void broadcastWorldBoostMessage(int worldId, String message) {
        Packet packet = PacketCreator.serverNotice(6, message);
        try {
            Server.getInstance().broadcastMessage(5, packet);
        } catch (Exception e) {
            alertBroadcast("Failed to broadcast message", e);
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
                if (v < 0) v = 0;
                if (v > MAX_LEAF_CAP) v = MAX_LEAF_CAP;
                queuedLeaves.set(v);
            } else {
                alertDb("No row found in event_maple_leaf where id=1. Donations will not persist.", null);
            }

        } catch (Exception e) {
            alertDb("Failed to load donation data.", e);
        }
    }

    private static void saveDonationData() {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE event_maple_leaf SET total_donated = ? WHERE id = 1")) {

            ps.setInt(1, queuedLeaves.get());
            ps.executeUpdate();

        } catch (Exception e) {
            alertDb("Failed to save donation data.", e);
        }
    }
}