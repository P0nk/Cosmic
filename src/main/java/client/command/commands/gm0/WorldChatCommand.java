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

        // 1) Ban check (Kept exactly the same)
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
                        player.yellowMessage("You are banned from world chat for another " + minutesRemaining + " minute(s).");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            player.yellowMessage("An error occurred while checking world chat ban status.");
            return;
        }

        // 2) Broadcast in-game (Kept same)
        String message = player.getLastCommandMessage(); // Assuming you have logic to set this from the command args
        String inGameText = "[" + player.getName() + "]: " + message;

        Server.getInstance().broadcastMessage(
                c.getWorld(),
                PacketCreator.serverNotice(6, inGameText)
        );

        // 3) Send to Discord (Reverted to Old Method)
        String webhook = EnvLoader.get("DISCORD_WORLDCHAT_WEBHOOK");

        if (webhook != null && !webhook.isEmpty()) {
            // A. Sanitize Logic
            String safeMessage = sanitizeForLogic(message);

            // B. Format the "Old Style" String
            // This will look like: **[World] PlayerName:** Hello World
            String discordContent = "**[World] " + player.getName() + ":** " + safeMessage;

            // C. Send using the fixed sendAsync (which now wraps this string in JSON for you)
            DiscordWebhook.sendAsync(webhook, discordContent);
        }
    }

    private static String sanitizeForLogic(String s) {
        if (s == null) return "";
        s = s.replace("@", "@\u200B"); // Prevent @everyone / @here pings
        if (s.length() > 1000) s = s.substring(0, 1000) + "...";
        return s;
    }
}