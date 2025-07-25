package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import net.server.Server;
import net.server.world.World;
import tools.DatabaseConnection;
import tools.PacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class WorldChatCommand extends Command {
    private int world;

    {
        setDescription("Sends chat to the whole world");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        int characterId = player.getId();

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT banned, period, `on` FROM worldchatban WHERE characterid = ? AND banned = TRUE")) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int period = rs.getInt("period"); // in minutes
                    Timestamp banTime = rs.getTimestamp("on");
                    long banEndMillis = banTime.getTime() + (period * 60L * 1000L);
                    long now = System.currentTimeMillis();

                    if (now < banEndMillis) {
                        long minutesRemaining = (banEndMillis - now) / 60000;
                        player.yellowMessage("You are banned from world chat for another " + minutesRemaining + " minute(s).");
                        return; // do not broadcast
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            player.yellowMessage("An error occurred while checking world chat ban status.");
            return;
        }

        // If not banned, broadcast the message
        Server.getInstance().broadcastMessage(
                c.getWorld(),
                PacketCreator.serverNotice(6, "[" + player.getName() + "]: " + player.getLastCommandMessage())
        );
    }
}
