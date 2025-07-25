package client.command.commands.gm3;

import client.Character;
import client.Client;
import client.command.Command;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

public class WorldChatBanCommand extends Command {
    {
        setDescription("Ban a player from world chat.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        if (params.length < 2) {
            player.yellowMessage("Syntax: !chatban <IGN> <PeriodInMinutes>");
            return;
        }

        String ign = params[0];
        int periodMinutes;
        try {
            periodMinutes = Integer.parseInt(params[1]);
        } catch (NumberFormatException e) {
            player.yellowMessage("Invalid period. Please enter a number.");
            return;
        }

        Character target = c.getChannelServer().getPlayerStorage().getCharacterByName(ign);
        if (target == null) {
            player.yellowMessage("Player not found: " + ign);
            return;
        }

        int characterId = target.getId();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "REPLACE INTO worldchatban (characterid, banned, period, `on`) VALUES (?, TRUE, ?, ?)")) {
            ps.setInt(1, characterId);
            ps.setInt(2, periodMinutes);
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
            player.yellowMessage("Player " + ign + " has been banned from world chat for " + periodMinutes + " minutes.");
        } catch (Exception e) {
            e.printStackTrace();
            player.yellowMessage("An error occurred while banning the player.");
        }
    }
}
