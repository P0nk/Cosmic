package client.command.commands.gm6;

import client.Client;
import client.command.Command;
import server.donor.DonorCreditManager;
import tools.DatabaseConnection;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;

public class RewardDonorCommand extends Command {

    {
        setDescription("Credits Donor Credits by SGD amount. Usage: !rewarddonor <playerName> <amountSGD>");
    }

    @Override
    public void execute(Client c, String[] params) {

        // ===== DEBUG: show raw params =====
        try {
            c.getPlayer().dropMessage(6,
                    "[DBG][rewarddonor] raw params.length=" + (params == null ? 0 : params.length)
                            + " raw params=" + (params == null ? "null" : Arrays.toString(params)));
        } catch (Exception ignored) {}

        if (params == null) {
            usage(c);
            return;
        }

        // Detect whether params[0] is the command name or the player name
        int offset = 0;
        if (params.length >= 1 && params[0] != null) {
            String p0 = params[0].toLowerCase();
            if (p0.equals("rewarddonor") || p0.equals("!rewarddonor")) {
                offset = 1;
            }
        }

        try { c.getPlayer().dropMessage(6, "[DBG][rewarddonor] offset=" + offset); } catch (Exception ignored) {}

        if (params.length < offset + 2) {
            try {
                c.getPlayer().dropMessage(6,
                        "[DBG][rewarddonor] insufficient args: need " + (offset + 2) + " but got " + params.length);
            } catch (Exception ignored) {}
            usage(c);
            return;
        }

        String name = params[offset];
        String amountStr = params[offset + 1];

        try {
            c.getPlayer().dropMessage(6, "[DBG][rewarddonor] parsed name='" + name + "' amountStr='" + amountStr + "'");
        } catch (Exception ignored) {}

        // Parse amount (supports 12.34)
        BigDecimal amt;
        try {
            amt = new BigDecimal(amountStr).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            c.getPlayer().dropMessage(6, "Invalid amount. Example: !rewarddonor IzmeMero 10 or 12.34");
            return;
        }

        if (amt.compareTo(BigDecimal.ZERO) <= 0) {
            c.getPlayer().dropMessage(6, "Amount must be > 0.");
            return;
        }

        int cents;
        try {
            cents = amt.multiply(new BigDecimal("100")).intValueExact();
        } catch (Exception e) {
            c.getPlayer().dropMessage(6, "Amount invalid (too many decimals / too large).");
            return;
        }

        try { c.getPlayer().dropMessage(6, "[DBG][rewarddonor] donation cents=" + cents); } catch (Exception ignored) {}

        // Online lookup
        var victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);

        try {
            c.getPlayer().dropMessage(6,
                    "[DBG][rewarddonor] victim=" + (victim == null ? "null" : (victim.getName() + " (cid=" + victim.getId() + ")")));
        } catch (Exception ignored) {}

        if (victim == null) {
            c.getPlayer().dropMessage(6, "Player not found online: " + name);
            return;
        }

        int accountId = victim.getClient().getAccID();
        String adminRef = "ADMIN:" + c.getPlayer().getName();

        // ===== 1) Credit DC =====
        try {
            DonorCreditManager.CreditResult res = DonorCreditManager.creditDonation(accountId, cents, adminRef);

            // ===== 2) Promote to GM1 ONLY if their LIVE GM < 1 =====
            PromoteResult pr = promoteToDonorGm1IfNeeded(victim.getClient(), victim.getId(), accountId);

            c.getPlayer().dropMessage(6,
                    "Credited " + victim.getName()
                            + ": +" + fmtCents(res.creditedTotalCents) + " DC"
                            + " (donation " + fmtCents(res.donationCents)
                            + ", milestone bonus " + fmtCents(res.bonusCents) + ")."
                            + " New balance: " + fmtCents(res.newBalanceCents) + " DC."
                            + " Lifetime: $" + fmtCents(res.newLifetimeCents) + " SGD."
                            + (pr.promoted ? " [Donor GM1 granted]" : " [GM unchanged]")
                            + " (liveGM=" + pr.liveGm + ")"
            );

            victim.dropMessage(5,
                    "[Donor] You received +" + fmtCents(res.creditedTotalCents)
                            + " DC. Type @donorreward to open the Donor Shop."
            );

            if (pr.promoted) {
                victim.dropMessage(5, "[Donor] Donor access enabled. If you can't use donor commands yet, please relog once.");
            }

        } catch (Exception e) {
            c.getPlayer().dropMessage(6, "Failed to credit donor: " + e.getMessage());
            System.err.println("[RewardDonorCommand] error: " + e.getMessage());
        }
    }

    private static final class PromoteResult {
        final boolean promoted;
        final int liveGm;
        PromoteResult(boolean promoted, int liveGm) {
            this.promoted = promoted;
            this.liveGm = liveGm;
        }
    }

    /**
     * Promote donor to GM level 1 ONLY if LIVE GM < 1.
     * This prevents demoting staff even if DB fields are inconsistent.
     */
    private static PromoteResult promoteToDonorGm1IfNeeded(Client victimClient, int charId, int accountId) {
        int liveGm = getLiveGmLevel(victimClient);

        // HARD STOP: never touch staff/donors already >= 1
        if (liveGm >= 1) {
            return new PromoteResult(false, liveGm);
        }

        boolean promoted = false;

        // DB update (persistent) — safe because we already confirmed live GM < 1
        try (Connection con = DatabaseConnection.getConnection()) {

            // accounts.gm -> 1 (only if <1)
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE cosmic.accounts SET gm = 1 WHERE id = ? AND gm < 1"
            )) {
                ps.setInt(1, accountId);
                promoted = ps.executeUpdate() > 0;
            } catch (Exception ignored) {}

            // optional mirrors (only if promoting)
            if (promoted) {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE cosmic.characters SET gm = 1 WHERE id = ? AND (gm IS NULL OR gm < 1)"
                )) {
                    ps.setInt(1, charId);
                    ps.executeUpdate();
                } catch (Exception ignored) {}

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE cosmic.characters SET gmlevel = 1 WHERE id = ? AND (gmlevel IS NULL OR gmlevel < 1)"
                )) {
                    ps.setInt(1, charId);
                    ps.executeUpdate();
                } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            System.err.println("[RewardDonorCommand] GM1 DB update error: " + e.getMessage());
            return new PromoteResult(false, liveGm);
        }

        // In-memory setter ONLY if live GM < 1 and DB promotion happened
        if (promoted) {
            try {
                Object chr = victimClient.getPlayer();
                if (chr != null) {
                    tryInvoke(chr, "setGMLevel", new Class[]{int.class}, new Object[]{1});
                    tryInvoke(chr, "setGmLevel", new Class[]{int.class}, new Object[]{1});
                    tryInvoke(chr, "setGM", new Class[]{int.class}, new Object[]{1});
                    tryInvoke(chr, "setGm", new Class[]{int.class}, new Object[]{1});
                }
                tryInvoke(victimClient, "setGMLevel", new Class[]{int.class}, new Object[]{1});
                tryInvoke(victimClient, "setGmLevel", new Class[]{int.class}, new Object[]{1});
            } catch (Exception ignored) {}
        }

        return new PromoteResult(promoted, liveGm);
    }

    /**
     * Reads the victim's CURRENT live GM level from Character using reflection.
     * Tries common getter names across HeavenMS forks.
     */
    private static int getLiveGmLevel(Client victimClient) {
        try {
            Object chr = victimClient.getPlayer();
            if (chr == null) return -1;

            Integer v;

            v = tryGetInt(chr, "getGMLevel");
            if (v != null) return v;

            v = tryGetInt(chr, "getGmLevel");
            if (v != null) return v;

            v = tryGetInt(chr, "gmLevel"); // unlikely, but harmless
            if (v != null) return v;

        } catch (Exception ignored) {}
        return -1;
    }

    private static Integer tryGetInt(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            m.setAccessible(true);
            Object out = m.invoke(target);
            if (out instanceof Integer) return (Integer) out;
            if (out instanceof Number) return ((Number) out).intValue();
        } catch (Exception ignored) {}
        return null;
    }

    private static void tryInvoke(Object target, String method, Class<?>[] sig, Object[] args) {
        try {
            Method m = target.getClass().getMethod(method, sig);
            m.setAccessible(true);
            m.invoke(target, args);
        } catch (Exception ignored) {}
    }

    private static void usage(Client c) {
        c.getPlayer().dropMessage(6, "Usage: !rewarddonor <playerName> <amountSGD>");
        c.getPlayer().dropMessage(6, "Example: !rewarddonor IzmeMero 10 or 12.34");
    }

    private static String fmtCents(long cents) {
        long abs = Math.abs(cents);
        long dollars = abs / 100;
        long rem = abs % 100;
        String s = dollars + "." + (rem < 10 ? "0" + rem : rem);
        return (cents < 0 ? "-" : "") + s;
    }
}
