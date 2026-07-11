/*
    This file is part of the HeavenMS MapleStory Server, commands OdinMS-based
    Copyleft (L) 2016 - 2019 RonanLana

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/*
   @Author: Arthur L - Refactored command content into modules
*/
package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import constants.id.NpcId;
import server.ItemInformationProvider;
import server.life.MonsterInformationProvider;
import tools.DatabaseConnection;
import tools.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;

public class WhoDropsCommand extends Command {
    {
        setDescription("Find out which mobs drop a specific item");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        if (params.length < 1) {
            player.dropMessage(5, "Please type @wd <item name>");
            return;
        }

        if (c.tryacquireClient()) {
            try {
                String searchString = String.join(" ", params);

                StringBuilder output = new StringBuilder();
                Iterator<Pair<Integer, String>> listIterator = ItemInformationProvider.getInstance().getItemDataByName(searchString).iterator();

                if (listIterator.hasNext()) {
                    int count = 1;
                    while (listIterator.hasNext() && count <= 3) {
                        Pair<Integer, String> data = listIterator.next();
                        boolean exists = false;

                        try (Connection con = DatabaseConnection.getConnection()) {
                            try (PreparedStatement ps = con.prepareStatement("SELECT chance, dropperid FROM drop_data d WHERE itemid = ? LIMIT 100")) {
                                ps.setInt(1, data.getLeft());
                                try (ResultSet rs = ps.executeQuery()) {
                                    while (rs.next()) {
                                        if (!exists) {
                                            output.append("#i").append(data.getLeft()).append(" # #b#z").append(data.getLeft()).append("##k is dropped by:\r\n");
                                            exists = true;
                                            count++;
                                        }
                                        String resultName = MonsterInformationProvider.getInstance().getMobNameFromId(rs.getInt("dropperid"));
                                        if (resultName != null) {
                                            float dropChance = rs.getFloat("chance");
                                            float chance = Float.parseFloat(String.format("%.2f", 1000000 / dropChance / (
                                                    !MonsterInformationProvider.getInstance().isBoss(rs.getInt("dropperid")) ? player.getDropRate()
                                                            : player.getBossDropRate())));
                                            String chanceStr = String.format("%.2f", 100 / chance);
                                            if (chanceStr.endsWith(".00")) {
                                                chanceStr = chanceStr.substring(0, chanceStr.length() - 3);
                                            }
                                            output.append(resultName).append(" (#b").append(chanceStr).append("%#k)\r\n");
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            player.dropMessage(6, "There was a problem retrieving the required data. Please try again.");
                            e.printStackTrace();
                            return;
                        }

                        if (exists) {
                            output.append("\r\n\r\n");
                        } else {
                            output.append("Nothing drops ").append(data.getRight()).append(" (ID: ").append(data.getLeft()).append(")\r\n\r\n");
                        }
                    }
                } else {
                    player.dropMessage(5, "The item you searched for doesn't exist.");
                    return;
                }

                c.getAbstractPlayerInteraction().npcTalk(NpcId.MAPLE_ADMINISTRATOR, output.toString());
            } finally {
                c.releaseClient();
            }
        } else {
            player.dropMessage(5, "Please wait a while for your request to be processed.");
        }
    }
}
