package client.command.commands.gm3;

import client.Character;
import client.Client;
import client.command.Command;
import net.server.Server;
import server.TimerManager;
import tools.DatabaseConnection;
import tools.PacketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * BanCommand
 * Bans a player by IGN, including IP and MAC address.
 * Displays proper feedback to GM and other players.
 */
public class Ban2Command extends Command {
    private static final Logger log = LoggerFactory.getLogger(Ban2Command.class);

    {
        setDescription("Ban a player by IGN with IP and MAC enforcement.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character gm = c.getPlayer();
        log.info("Ban2Command started by GM: {} with params: {}", gm.getName(), Arrays.toString(params));

        // 1. Validate command arguments
        if (params.length < 2) {
            log.warn("Invalid command syntax - insufficient parameters");
            gm.yellowMessage("Syntax: !ban <IGN> <Reason>");
            return;
        }

        String ign = params[0];
        String reason = joinStringFrom(params, 1);
        log.info("Processing ban request for player: {} with reason: {}", ign, reason);

        // 2. Attempt to retrieve the target player
        Character target = c.getChannelServer().getPlayerStorage().getCharacterByName(ign);
        if (target != null) {
            log.info("Target player found online: {}", target.getName());

            String readableTargetName = Character.makeMapleReadable(target.getName());

            // === IP Ban Section ===
            String ip = target.getClient().getRemoteAddress();
            ip = ip.split(":")[0].replace("/", ""); // clean up "/127.0.0.1:port" to "127.0.0.1"
            log.info("Processing IP ban for: {} (cleaned IP: {})", target.getName(), ip);

            try (Connection con = DatabaseConnection.getConnection()) {
                log.info("Database connection established for IP ban");
                try (PreparedStatement ps = con.prepareStatement("INSERT IGNORE INTO ipbans (ip, aid) VALUES (?, ?)")) {
                    ps.setString(1, ip);
                    ps.setInt(2, target.getClient().getAccID());
                    int rowsAffected = ps.executeUpdate();
                    log.info("IP ban inserted successfully. Rows affected: {}", rowsAffected);
                }
            } catch (SQLException ex) {
                log.error("Database error during IP ban for player: " + target.getName(), ex);
                gm.message(":warning: Error banning IP: " + ip);
            }

            // === MAC Ban Section ===
            log.info("Processing MAC ban for: {}", target.getName());
            target.getClient().banMacs();
            log.info("MAC ban applied for: {}", target.getName());

            // === Final Ban and Logging ===
            String banReason = gm.getName() + " banned " + readableTargetName + " for " + reason +
                    " (IP: " + ip + ", MAC: " + target.getClient().getMacs() + ")";
            log.info("Applying character ban with reason: {}", banReason);
            target.ban(banReason);
            log.info("Character ban applied successfully");

            // Inform the banned user
            target.yellowMessage("You have been banned by #b" + gm.getName() + "#k.");
            target.yellowMessage("Reason: " + reason);
            log.info("Ban notification sent to target player");

            // Visual GM effect
            c.sendPacket(PacketCreator.getGMEffect(4, (byte) 0));
            log.info("GM effect packet sent");

            // Kick player from the server after delay
            final Character toDisconnect = target;
            log.info("Scheduling disconnect for player: {} in 5 seconds", toDisconnect.getName());
            TimerManager.getInstance().schedule(() -> {
                log.info("Executing scheduled disconnect for player: {}", toDisconnect.getName());
                toDisconnect.getClient().forceDisconnect();
                log.info("Disconnect completed for player: {}", toDisconnect.getName());
            }, 1000);

            // Broadcast to the world
            log.info("Broadcasting ban message to world");
            Server.getInstance().broadcastMessage(c.getWorld(),
                    PacketCreator.serverNotice(6, "[RIP]: " + ign + " has been banned."));
            log.info("Ban broadcast completed");
        }

        // 3. If target not online, try offline ban
        else if (Character.ban(ign, reason, false)) {
            log.info("Offline ban successful for player: {}", ign);
            c.sendPacket(PacketCreator.getGMEffect(4, (byte) 0));
            Server.getInstance().broadcastMessage(c.getWorld(),
                    PacketCreator.serverNotice(6, "[RIP]: " + ign + " has been banned."));
        }

        // 4. Target does not exist
        else {
            log.warn("Ban failed - character not found: {}", ign);
            c.sendPacket(PacketCreator.getGMEffect(6, (byte) 1));
            gm.message(":warning: Failed to ban " + ign + ". Character not found.");
        }
        
        log.info("Ban2Command execution completed for target: {}", ign);
    }
}