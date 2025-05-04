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
 * BanCommand
 * Bans a player by IGN, including IP and MAC address.
 * Displays proper feedback to GM and other players.
 */
public class Ban2Command extends Command {

    {
        setDescription("Ban a player by IGN with IP and MAC enforcement.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character gm = c.getPlayer();

        // 1. Validate command arguments
        if (params.length < 2) {
            gm.yellowMessage("Syntax: !ban <IGN> <Reason>");
            return;
        }

        String ign = params[0];
        String reason = joinStringFrom(params, 1);

        // 2. Attempt to retrieve the target player
        Character target = c.getChannelServer().getPlayerStorage().getCharacterByName(ign);
        if (target != null) {

            String readableTargetName = Character.makeMapleReadable(target.getName());

            // === IP Ban Section ===
            String ip = target.getClient().getRemoteAddress();
            ip = ip.split(":")[0].replace("/", ""); // clean up "/127.0.0.1:port" to "127.0.0.1"

            try (Connection con = DatabaseConnection.getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("INSERT IGNORE INTO ipbans (ip, accid) VALUES (?, ?)")) {
                    ps.setString(1, ip);
                    ps.setInt(2, target.getClient().getAccID());
                    ps.executeUpdate();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                gm.message(":warning: Error banning IP: " + ip);
            }
            // === MAC Ban Section ===
            target.getClient().banMacs(); // Ensure this method actually inserts into DB

            // === Final Ban and Logging ===
            String banReason = gm.getName() + " banned " + readableTargetName + " for " + reason +
                    " (IP: " + ip + ", MAC: " + target.getClient().getMacs() + ")";
            target.ban(banReason);

            // Inform the banned user
            target.yellowMessage("You have been banned by #b" + gm.getName() + "#k.");
            target.yellowMessage("Reason: " + reason);

            // Visual GM effect
            c.sendPacket(PacketCreator.getGMEffect(4, (byte) 0));

            // Kick player from the server after delay
            final Character toDisconnect = target;
            TimerManager.getInstance().schedule(() -> toDisconnect.getClient().disconnect(false, false), 5000);

            // Broadcast to the world
            Server.getInstance().broadcastMessage(c.getWorld(),
                    PacketCreator.serverNotice(6, "[RIP]: " + ign + " has been banned."));
        }

        // 3. If target not online, try offline ban
        else if (Character.ban(ign, reason, false)) {
            c.sendPacket(PacketCreator.getGMEffect(4, (byte) 0));
            Server.getInstance().broadcastMessage(c.getWorld(),
                    PacketCreator.serverNotice(6, "[RIP]: " + ign + " has been banned."));
        }

        // 4. Target does not exist
        else {
            c.sendPacket(PacketCreator.getGMEffect(6, (byte) 1));
            gm.message(":warning: Failed to ban " + ign + ". Character not found.");
        }
    }
}