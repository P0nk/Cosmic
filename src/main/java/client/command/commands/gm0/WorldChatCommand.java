package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import net.server.Server;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.EnvLoader;
import tools.DiscordWebhook;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class WorldChatCommand extends Command {

    {
        setDescription("Sends chat to the whole world");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        int characterId = player.getId();

        // 1) Ban check (unchanged)
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT banned, period, `on` FROM worldchatban WHERE characterid = ? AND banned = TRUE")) {

            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int period = rs.getInt("period");
                    Timestamp banTime = rs.getTimestamp("on");
                    long banEndMillis = banTime.getTime() + (period * 60L * 1000L);
                    long now = System.currentTimeMillis();

                    if (now < banEndMillis) {
                        long minutesRemaining = (banEndMillis - now) / 60000;
                        player.yellowMessage(
                                "You are banned from world chat for another "
                                        + minutesRemaining + " minute(s)."
                        );
                        return;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            player.yellowMessage("An error occurred while checking world chat ban status.");
            return;
        }

        // 2) Build message
        String message = player.getLastCommandMessage();
        String inGameText = "[" + player.getName() + "]: " + message;

        // 3) Broadcast in-game
        Server.getInstance().broadcastMessage(
                c.getWorld(),
                PacketCreator.serverNotice(6, inGameText)
        );

        // 4) Send to Discord (env-based, async)
        String webhook = EnvLoader.get("DISCORD_WORLDCHAT_WEBHOOK");
        if (webhook != null) {
            String discordText =
                    "**[World " + c.getWorld() + "]** "
                            + player.getName()
                            + ": "
                            + sanitize(message);

            DiscordWebhook.sendAsync(webhook, discordText);
        }
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        s = s.replace("@", "@\u200B"); // prevent @everyone abuse
        s = s.replace("\r", " ").replace("\n", " ");
        if (s.length() > 1800) s = s.substring(0, 1800) + "...";
        return s;
    }
}
