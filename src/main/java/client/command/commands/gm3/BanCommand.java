package client.command.commands.gm3;

import client.Character;
import client.Client;
import client.command.Command;
import net.server.Server;
import server.TimerManager;
import tools.DatabaseConnection;
import tools.PacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Enhanced Ban Command
 * Supports 'Account Only' and 'Max' (Nuclear) modes.
 * Default: Account Only
 */
public class BanCommand extends Command {
    {
        setDescription("Bans a player. Syntax: !ban [account|max] <IGN> <Reason>");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character gm = c.getPlayer();

        if (params.length < 2) {
            // Updated Help Message to reflect new default
            gm.yellowMessage("Syntax: !ban <IGN> <Reason> (Default: Account Only)");
            gm.yellowMessage("Option: !ban max <IGN> <Reason> (Nuke: Account + IP + MAC + HWID)");
            return;
        }

        // --- 1. Parse Mode & Arguments ---
        // [CHANGE] Default set to "account" instead of "max"
        String mode = "account";
        String targetName;
        String reason;

        // Check if first arg is a subcommand
        if (params[0].equalsIgnoreCase("max") || params[0].equalsIgnoreCase("full") || params[0].equalsIgnoreCase("ip")) {
            mode = "max";
            targetName = params[1];
            reason = joinStringFrom(params, 2);
        } else if (params[0].equalsIgnoreCase("account") || params[0].equalsIgnoreCase("acc")) {
            mode = "account";
            targetName = params[1];
            reason = joinStringFrom(params, 2);
        } else {
            // No subcommand, use default (Account)
            targetName = params[0];
            reason = joinStringFrom(params, 1);
        }

        if (targetName == null || reason.isEmpty()) {
            gm.yellowMessage("Error: You must provide a name and a reason.");
            return;
        }

        Character target = c.getChannelServer().getPlayerStorage().getCharacterByName(targetName);

        // --- 2. Execute Ban Logic ---
        if (target != null) {
            // === ONLINE BAN ===
            Client targetClient = target.getClient();
            String ip = targetClient.getRemoteAddress();
            int accId = targetClient.getAccID();
            String readableTargetName = Character.makeMapleReadable(target.getName());

            // A. Hardware/Network Bans (Only if mode is explicitly MAX)
            if (mode.equals("max")) {
                // IP Ban (Regex Safe)
                if (ip.matches("[0-9]{1,3}\\..*") && !ip.equals("127.0.0.1")) {
                    try (Connection con = DatabaseConnection.getConnection();
                         PreparedStatement ps = con.prepareStatement("INSERT IGNORE INTO ipbans (ip, aid) VALUES (?, ?)")) {
                        ps.setString(1, ip);
                        ps.setInt(2, accId);
                        ps.executeUpdate();
                    } catch (SQLException ex) {
                        gm.message("Error banning IP: " + ex.getMessage());
                    }
                }

                // MAC & HWID Bans
                targetClient.banMacs();
                targetClient.banHWID();
            }

            // B. Account Ban (Always applied)
            String fullReason = "[" + mode.toUpperCase() + "] Banned by " + gm.getName() + ": " + reason;
            target.ban(fullReason);

            // C. Notify & Disconnect
            target.yellowMessage("You have been banned by " + gm.getName() + ".");
            target.yellowMessage("Reason: " + reason);
            c.sendPacket(PacketCreator.getGMEffect(4, (byte) 0));

            // Delayed Disconnect
            TimerManager.getInstance().schedule(() -> {
                if (target != null && target.getClient() != null) {
                    target.getClient().disconnect(true, false);
                }
            }, 3000);

            // D. Feedback
            Server.getInstance().broadcastMessage(c.getWorld(),
                    PacketCreator.serverNotice(6, "[System] " + targetName + " has been banned (" + mode + ") for " + reason + "."));

        } else {
            // === OFFLINE BAN ===
            if (Character.ban(targetName, "Offline banned by " + gm.getName() + ": " + reason, false)) {
                c.sendPacket(PacketCreator.getGMEffect(4, (byte) 0));
                gm.message("Offline ban successful for " + targetName + " (Account Only).");
                Server.getInstance().broadcastMessage(c.getWorld(),
                        PacketCreator.serverNotice(6, "[System] " + targetName + " has been banned for " + reason + "."));
            } else {
                c.sendPacket(PacketCreator.getGMEffect(6, (byte) 1));
                gm.message("Failed to ban " + targetName + ". Character not found.");
            }
        }
    }
}