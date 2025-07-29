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

public class BanCommand extends Command {
    {
        setDescription("Ban a player.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        if (params.length < 2) {
            player.yellowMessage("Syntax: !ban <IGN> <Reason> (Please be descriptive)");
            return;
        }

        // Step 1: Extract parameters
        String ign = params[0];
        String reason = joinStringFrom(params, 1);
        System.out.println("[BanCommand] Target IGN: " + ign);
        System.out.println("[BanCommand] Reason provided: " + reason);

        // Step 2: Attempt to find the player online
        Character target = c.getChannelServer().getPlayerStorage().getCharacterByName(ign);
        System.out.println("[BanCommand] Target character found online: " + (target != null));

        if (target != null) {
            String readableTargetName = Character.makeMapleReadable(target.getName());
            String ip = target.getClient().getRemoteAddress();
            System.out.println("[BanCommand] Readable IGN: " + readableTargetName);
            System.out.println("[BanCommand] IP Address: " + ip);
            System.out.println("[BanCommand] MAC Address: " + c.getMacs());

            // Step 3: Insert IP ban
            try (Connection con = DatabaseConnection.getConnection()) {
                if (ip.matches("/[0-9]{1,3}\\..*")) {
                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?, ?)")) {
                        ps.setString(1, ip);
                        ps.setString(2, String.valueOf(target.getClient().getAccID()));
                        int rowsAffected = ps.executeUpdate();
                        System.out.println("[BanCommand] IP Ban inserted, rows affected: " + rowsAffected);
                    }
                } else {
                    System.out.println("[BanCommand] IP pattern did not match expected format, skipping IP ban.");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                c.getPlayer().message("Error occurred while banning IP address");
                c.getPlayer().message(target.getName() + "'s IP was not banned: " + ip);
            }

            // Step 4: Ban MACs
            target.getClient().banMacs();
            reason = c.getPlayer().getName() + " banned " + readableTargetName + " for " + reason + " (IP: " + ip + ") " + "(MAC: " + c.getMacs() + ")";
            System.out.println("[BanCommand] Final reason string: " + reason);

            // Step 5: Apply ban
            target.ban(reason);
            target.yellowMessage("You have been banned by #b" + c.getPlayer().getName() + " #k.");
            target.yellowMessage("Reason: " + reason);

            // Step 6: Visual effect
            c.sendPacket(PacketCreator.getGMEffect(4, (byte) 0));

            // Step 7: Disconnect
            final Character rip = target;
            TimerManager.getInstance().schedule(() -> {
                System.out.println("[BanCommand] Disconnecting player after 5 seconds: " + rip.getName());
                rip.getClient().disconnect(false, false);
            }, 5000);

            // Step 8: Global broadcast
            Server.getInstance().broadcastMessage(c.getWorld(), PacketCreator.serverNotice(6, "[RIP]: " + ign + " has been banned."));
        } else {
            // Offline ban fallback
            System.out.println("[BanCommand] Target is offline. Attempting offline ban for: " + ign);
            if (Character.ban(ign, reason, false)) {
                System.out.println("[BanCommand] Offline ban successful for: " + ign);
                c.sendPacket(PacketCreator.getGMEffect(4, (byte) 0));
                Server.getInstance().broadcastMessage(c.getWorld(), PacketCreator.serverNotice(6, "[RIP]: " + ign + " has been banned."));
            } else {
                System.out.println("[BanCommand] Offline ban failed for: " + ign);
                c.sendPacket(PacketCreator.getGMEffect(6, (byte) 1));
            }
        }
    }
}
