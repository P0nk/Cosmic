package server.buffnpc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    private static final int LEAVES_PER_HOUR = 10000;
    private static final int MAX_LEAF_CAP = 40000; // Cap storage at 40k (4 hours queued)
    private static final int ONE_HOUR_MS = 3600000;

    // --- State ---
    // Represents leaves currently "in the bank" waiting to be used
    private static final AtomicInteger queuedLeaves = new AtomicInteger(0);
    // Timestamp when the CURRENT active boost expires
    private static long currentBoostEndTime = 0;
    // Track if timer is currently running to prevent double scheduling
    private static boolean isTimerRunning = false;

    // Load data from SQL on startup
    static {
        loadDonationData();
        // Resume logic: If server crashed while leaves were queued, try to restart boost
        if (queuedLeaves.get() >= LEAVES_PER_HOUR && currentBoostEndTime < System.currentTimeMillis()) {
            consumeLeavesAndStartRound();
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
        long activeTimeLeft = Math.max(0, currentBoostEndTime - System.currentTimeMillis());

        // Calculate how many full hours are waiting in the queue
        int hoursInQueue = queuedLeaves.get() / LEAVES_PER_HOUR;
        long queuedTime = hoursInQueue * ONE_HOUR_MS;

        return activeTimeLeft + queuedTime;
    }

    // Check if player can donate specific amount without overflowing 40k
    public static boolean canDonate(int amount) {
        return (queuedLeaves.get() + amount) <= MAX_LEAF_CAP;
    }

    // --- Main Logic ---

    public static void handleDonation(int amountDonated) {
        // 1. Add to the queue
        int newTotal = queuedLeaves.addAndGet(amountDonated);
        saveDonationData();

        // 2. If the boost isn't active, try to start it immediately
        // (If it IS active, we just leave the leaves in the queue for the timer to pick up later)
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
        // Deduct the cost immediately
        queuedLeaves.addAndGet(-LEAVES_PER_HOUR);
        saveDonationData();

        // Extend the time
        // If we are already running (extending), add to existing time?
        // Logic: Simplest is to set exact end time from NOW if starting fresh, or NOW + 1hr
        currentBoostEndTime = System.currentTimeMillis() + ONE_HOUR_MS;

        activateWorldEffects();

        // If this is the first start (timer not running), start the loop
        if (!isTimerRunning) {
            startTimerLoop();
        }
    }

    private static void startTimerLoop() {
        isTimerRunning = true;
        // Schedule check exactly when this hour ends
        TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                // The hour has passed.
                // Check if we have enough leaves for ANOTHER hour
                if (queuedLeaves.get() >= LEAVES_PER_HOUR) {
                    broadcastWorldBoostMessage(0, "[System] The Maple Leaf Event has been extended for another hour!");
                    consumeLeavesAndStartRound(); // Recursive-like behavior (loops back)
                } else {
                    // Stop everything
                    isTimerRunning = false;
                    resetWorldSpawnBoost();
                }
            }
        }, ONE_HOUR_MS);
    }

    private static void activateWorldEffects() {
        Server server = getServerInstance();
        for (World world : server.getWorlds()) {
            world.setMobrate(3);
            world.setMobperspawnpoint(3);
            // Only announce "Activated" if we assume it wasn't running,
            // but for smooth extensions, we usually rely on the timer message.
            // We can send a silent update or just ensure rates are set.
        }
        // Send global message only if this is a fresh start (approximated logic)
        // You can make this smarter, but calling it every hour is fine too.
        broadcastWorldBoostMessage(0, "[System] World spawn boost is ACTIVE! (Rates doubled).");
    }

    private static void resetWorldSpawnBoost() {
        currentBoostEndTime = 0;
        isTimerRunning = false;

        Server server = getServerInstance();
        for (World world : server.getWorlds()) {
            world.setMobrate(2);
            world.setMobperspawnpoint(2);
        }
        broadcastWorldBoostMessage(0, "[System] World spawn boost has ended. We need more leaves to restart!");
    }

    // --- Helper Methods ---

    // worldId 0 usually broadcasts to all worlds in simple sources, or loop it.
    private static void broadcastWorldBoostMessage(int worldId, String message) {
        Packet packet = PacketCreator.serverNotice(6, message);
        try {
            Server.getInstance().broadcastMessage(5,packet);
        } catch (Exception e) {
//            // Fallback if broadcastMessage doesn't exist on Server
//            for (World w : Server.getInstance().getWorlds()) {
//                w.broadcastMessage(packet);
//            }
        }
    }

    private static Server getServerInstance() {
        return Server.getInstance();
    }

    // --- SQL Methods ---

    private static void loadDonationData() {
        try (Connection con = DatabaseConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT total_donated FROM event_maple_leaf WHERE id = 1");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                queuedLeaves.set(rs.getInt("total_donated"));
            }
            ps.close();
            rs.close();
        } catch (Exception e) {
            System.err.println("[MapleLeafManager] Failed to load donation data: " + e);
        }
    }

    private static void saveDonationData() {
        // Runs on every donation/deduction.
        // If high traffic, consider moving this to a scheduled thread (every 1 min).
        try (Connection con = DatabaseConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE event_maple_leaf SET total_donated = ? WHERE id = 1");
            ps.setInt(1, queuedLeaves.get());
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            System.err.println("[MapleLeafManager] Failed to save donation data: " + e);
        }
    }
}