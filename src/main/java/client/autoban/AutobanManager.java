/*
 * Revised AutobanManager for Auto-Jail and Strike System
 * FIXED: Removed missing methods (Divine Shield, BanIP, GetMobCount)
 */
package client.autoban;

import client.BuffStat;
import client.Character;
import client.Job;
import config.YamlConfig;
import net.server.Server;
import server.maps.MapObjectType; // [FIX 1] Added Import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author kevintjuh93
 * @author Modified for Jail/Strike System
 */
public class AutobanManager {
    private static final Logger log = LoggerFactory.getLogger(AutobanManager.class);

    private final Character chr;
    private final Map<AutobanFactory, Integer> points = new HashMap<>();
    private final Map<AutobanFactory, Long> lastTime = new HashMap<>();

    // Counters
    private int consecutiveMisses = 0;
    private int fastAttackCount = 0;

    // Spam Arrays
    private final long[] spam = new long[20];
    private final int[] timestamp = new int[20];
    private final byte[] timestampcounter = new byte[20];

    public AutobanManager(Character chr) {
        this.chr = chr;
    }

    /**
     * [CORE] The Jail System
     * Checks strike count -> Jails or Bans -> Logs to DB
     */
    public void jailPlayer(String reason, int durationMinutes) {
        if (chr.isGM()) {
            chr.dropMessage(5, "[Anti-Hack] You would have been jailed for: " + reason);
            return;
        }

        // 1. Check Strikes (3 Strikes = Ban)
        int currentStrikes = getJailStrikeCount(chr.getClient().getAccID());

        if (currentStrikes >= 2) {
            applyPermBan("3rd Strike: " + reason);
            return;
        }

        // 2. Apply Jail
        long durationMillis = durationMinutes * 60 * 1000L;
        chr.setFutureJailExpiration(durationMillis);
        chr.changeMap(999999999, 0); // Warp to Jail Map

        // 3. Log to DB
        logJailToDB(reason, durationMinutes);

        // 4. Notify
        chr.dropMessage(1, "[SYSTEM] Security Violation Detected.");
        chr.dropMessage(1, "Reason: " + reason);
        chr.dropMessage(1, "Penalty: Jail for " + durationMinutes + " minutes.");
        chr.dropMessage(1, "Warning: Strike " + (currentStrikes + 1) + "/3. 3 Strikes = Permanent Ban.");

        System.out.println("[AutoJail] " + chr.getName() + " jailed for " + reason);
    }

    private int getJailStrikeCount(int accountId) {
        int strikes = 0;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM jail_log WHERE account_id = ?")) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    strikes = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to check jail strikes", e);
        }
        return strikes;
    }

    private void logJailToDB(String reason, int duration) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO jail_log (character_id, character_name, account_id, reason, duration_minutes) VALUES (?, ?, ?, ?, ?)")) {
            ps.setInt(1, chr.getId());
            ps.setString(2, chr.getName());
            ps.setInt(3, chr.getClient().getAccID());
            ps.setString(4, reason);
            ps.setInt(5, duration);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to log jail", e);
        }
    }

    private void applyPermBan(String reason) {
        // [FIX 2] Manual DB Ban instead of banIP() which didn't exist
        try {
            // Ban Account
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?")) {
                ps.setString(1, reason);
                ps.setInt(2, chr.getClient().getAccID());
                ps.executeUpdate();
            }

            chr.getClient().banMacs();
            chr.getClient().disconnect(true, false);
            System.out.println("[BAN] " + chr.getName() + " permabanned. Reason: " + reason);
        } catch (Exception e) {
            log.error("Failed to ban player", e);
        }
    }

    // --- [ANTI-HACK] 1. Miss God Mode ---
    public void addMiss() {
        this.consecutiveMisses++;

        // Thresholds: Thief/Bowman have high avoid, Warriors do not.
        int threshold = 10;
        if (chr.getJob().isA(Job.THIEF) || chr.getJob().isA(Job.NIGHTWALKER1)) threshold = 35;
        else if (chr.getJob().isA(Job.BOWMAN) || chr.getJob().isA(Job.WINDARCHER1)) threshold = 20;

        // [FIX 3] Removed DIVINE_SHIELD check (Symbol not found)
        if (chr.getBuffedValue(BuffStat.DARKSIGHT) != null) {
            return;
        }

        if (this.consecutiveMisses > threshold) {
            jailPlayer("Miss GodMode (" + consecutiveMisses + " misses in a row)", 30);
            this.consecutiveMisses = 0;
        }
    }

    public void resetMisses() {
        if (this.consecutiveMisses > 0) this.consecutiveMisses = 0;
    }

    // --- [ANTI-HACK] 2. God Mode Watchdog (No Packet) ---
    // Called from AbstractDealDamageHandler whenever they attack
    public void checkGodMode() {
        // [FIX 4] Use getAllMapObjects instead of getMobCount (Method not found)
        if (chr.isGM() || chr.getMap().countMonsters() == 0) return;

        if (chr.getBuffedValue(BuffStat.DARKSIGHT) != null) return;
        // [FIX 3] Removed DIVINE_SHIELD check here too

        long now = System.currentTimeMillis();
        long timeSinceAttack = now - chr.getLastAttackTime();
        long timeSinceHit = now - chr.getLastHitTime();

        // If active (attacked in last 2s) but haven't taken damage in 120s
        if (timeSinceAttack < 2000 && timeSinceHit > 120000) {
            // Thief exception: Give them 5 mins
            if (chr.getJob().isA(Job.THIEF) && timeSinceHit < 300000) return;

            // Jail for 24 Hours
            jailPlayer("God Mode / Packet Block (No damage taken for 120s+)", 1440);

            // Reset timers to prevent spam-jailing
            chr.updateHitAction();
        }
    }

    // --- [ANTI-HACK] 3. Fast Attack Counter ---
    public void checkFastAttack() {
        this.fastAttackCount++;
        if (this.fastAttackCount > 5) {
            jailPlayer("Unlimited Attack / No Delay", 60);
            this.fastAttackCount = 0;
        }
    }

    // --- [LEGACY] Points System (Updated to use Jail) ---
    public void addPoint(AutobanFactory fac, String reason) {
        if (chr.isGM() || chr.isBanned()) return;

        // Decay points over time
        if (lastTime.containsKey(fac)) {
            if (lastTime.get(fac) < (Server.getInstance().getCurrentTime() - fac.getExpire())) {
                points.put(fac, points.get(fac) / 2);
            }
        }
        if (fac.getExpire() != -1) {
            lastTime.put(fac, Server.getInstance().getCurrentTime());
        }

        points.put(fac, points.getOrDefault(fac, 0) + 1);

        if (points.get(fac) >= fac.getMaximum()) {
            jailPlayer(fac.name() + " (" + reason + ")", 60);
            points.put(fac, 0);
        }

        if (YamlConfig.config.server.USE_AUTOBAN_LOG) {
            log.info("AutoJail Accumulator - chr {} caused {} {}", chr.getName(), fac.name(), reason);
        }
    }

    // --- Utilities ---
    public void spam(int type) {
        this.spam[type] = Server.getInstance().getCurrentTime();
    }

    public void spam(int type, int timestamp) {
        this.spam[type] = timestamp;
    }

    public long getLastSpam(int type) {
        return spam[type];
    }

    public void setTimestamp(int type, int time, int times) {
        if (this.timestamp[type] == time) {
            this.timestampcounter[type]++;
            if (this.timestampcounter[type] >= times) {
                jailPlayer("Packet Spamming (Type: " + type + ")", 15);
                log.info("AutoJail - Chr {} was caught spamming TYPE {}", chr.getName(), type);
            }
        } else {
            this.timestamp[type] = time;
            this.timestampcounter[type] = 0;
        }
    }
}