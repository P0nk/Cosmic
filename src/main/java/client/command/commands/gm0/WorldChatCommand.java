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

        // 1) Ban check
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

        // 2) Broadcast in-game
        String message = player.getLastCommandMessage();
        String inGameText = "[" + player.getName() + "]: " + message;

        Server.getInstance().broadcastMessage(
                c.getWorld(),
                PacketCreator.serverNotice(6, inGameText)
        );

        // 3) Send to Discord (Rich Embed)
        // We use the specific World Chat webhook for player chatter
        String webhook = EnvLoader.get("DISCORD_WORLDCHAT_WEBHOOK");

        if (webhook != null && !webhook.isEmpty()) {
            // A. Sanitize Logic (Length & Discord Abuse)
            String logicSafeMessage = sanitizeForLogic(message);

            // B. Sanitize Syntax (JSON Special Chars like quotes and slashes)
            // We use the helper we added to DiscordWebhook.java previously
            String jsonSafeMessage = DiscordWebhook.escape(logicSafeMessage);
            String jsonSafeName = DiscordWebhook.escape(player.getName());

            // C. Construct the JSON Payload for the Embed
            String jsonPayload = "{"
                    + "\"username\": \"World Chat\","
                    + "\"embeds\": [{"
                    +    "\"author\": { \"name\": \"" + jsonSafeName + "\" },"
                    +    "\"description\": \"" + jsonSafeMessage + "\","
                    +    "\"color\": 3447003," // Blue Color (0x3498DB)
                    +    "\"footer\": { \"text\": \"World " + c.getWorld() + "\" }"
                    + "}]"
                    + "}";

            DiscordWebhook.sendEmbedAsync(webhook, jsonPayload);
        }
    }

    // Handles game-logic cleaning (truncating length, removing @everyone)
    private static String sanitizeForLogic(String s) {
        if (s == null) return "";
        s = s.replace("@", "@\u200B"); // Zero-width space to prevent pings
        if (s.length() > 1000) s = s.substring(0, 1000) + "..."; // Discord limit is usually 2000, 1000 is safe for embed desc
        return s;
    }
}