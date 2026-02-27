package client.command.commands.gm4;

import client.Character;
import client.Client;
import client.command.Command;
import net.server.Server;
import net.server.world.World;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SetGuildRankCommand extends Command {
    {
        setDescription("Sets the guild rank of an online or offline player.");
    }

    @Override
    public void execute(Client c, String[] params) {
        if (params.length < 2) {
            c.getPlayer().dropMessage(5, "Syntax: !setguildrank <character name> <1-5>");
            return;
        }

        String targetName = params[0];
        int newRank;
        try {
            newRank = Integer.parseInt(params[1]);
        } catch (NumberFormatException e) {
            c.getPlayer().dropMessage(5, "The rank must be a number between 1 and 5.");
            return;
        }

        if (newRank < 1 || newRank > 5) {
            c.getPlayer().dropMessage(5, "The rank must be between 1 and 5.");
            return;
        }

        Character target = Server.getInstance().getWorld(c.getPlayer().getWorld()).getPlayerStorage()
                .getCharacterByName(targetName);

        if (target != null) {
            // Player is online
            if (target.getGuildId() <= 0) {
                c.getPlayer().dropMessage(5, targetName + " is not currently in a guild.");
                return;
            }
            target.getGuild().changeRank(target.getId(), newRank);
            c.getPlayer().dropMessage(5, "Successfully changed " + targetName + "'s guild rank to " + newRank + ".");
        } else {
            // Player is offline, perform direct DB update
            try (Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("SELECT id, guildid FROM characters WHERE name = ?")) {
                ps.setString(1, targetName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int targetId = rs.getInt("id");
                        int guildId = rs.getInt("guildid");

                        if (guildId <= 0) {
                            c.getPlayer().dropMessage(5, targetName + " is not currently in a guild.");
                            return;
                        }

                        // Use the World's setOfflineGuildStatus method which also broadcasts if
                        // applicable
                        Server.getInstance().getWorld(c.getPlayer().getWorld()).setOfflineGuildStatus((short) guildId,
                                (byte) newRank, targetId);

                        // Also forcefully update it in DB directly just in case they are entirely
                        // offline and world broadcast missed it
                        try (PreparedStatement updatePs = con
                                .prepareStatement("UPDATE characters SET guildrank = ? WHERE id = ?")) {
                            updatePs.setInt(1, newRank);
                            updatePs.setInt(2, targetId);
                            updatePs.executeUpdate();
                        }

                        c.getPlayer().dropMessage(5, "Successfully changed offline player " + targetName
                                + "'s guild rank to " + newRank + ".");
                    } else {
                        c.getPlayer().dropMessage(5, "Character " + targetName + " could not be found.");
                    }
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(5,
                        "An error occurred while trying to update the guild rank in the database.");
                e.printStackTrace();
            }
        }
    }
}
