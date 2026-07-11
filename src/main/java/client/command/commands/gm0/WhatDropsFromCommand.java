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
import server.life.MonsterDropEntry;
import server.life.MonsterInformationProvider;
import tools.Pair;

import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;

public class WhatDropsFromCommand extends Command {
    {
        setDescription("Show what items drop from a mob.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        if (params.length < 1) {
            player.dropMessage(5, "Please type @wdf <monster name>");
            return;
        }
        String monsterName = String.join(" ", params);
        StringBuilder output = new StringBuilder();
        int limit = 3;
        Iterator<Pair<Integer, String>> listIterator = MonsterInformationProvider.getMobsIDsFromName(monsterName).iterator();
        for (int i = 0; i < limit; i++) {
            if (listIterator.hasNext()) {
                Pair<Integer, String> data = listIterator.next();
                int mobId = data.getLeft();
                String mobName = data.getRight();

                List<MonsterDropEntry> dropList = MonsterInformationProvider.getInstance().retrieveDrop(mobId);

                if (dropList.isEmpty()) {
                    limit++;
                    continue;
                }

                output.append("#b").append(mobName).append(" #k(ID: ").append(mobId).append(") drops the following items:\r\n\r\n");
                for (MonsterDropEntry drop : dropList) {
                    try {
                        // Check for meso first
                        if (drop.itemId == 0) {
                            continue;
                        }

                        // Now check to make sure the item is valid
                        if (!ItemInformationProvider.getInstance().itemHasEssentialData(drop.itemId)) {
                            System.out.println("Invalid drop found: Item ID: " + drop.itemId + " || Monster ID: " + mobId);
                            continue;
                        }
                        float chance = 1000000f / drop.chance / (
                                !MonsterInformationProvider.getInstance().isBoss(mobId) ? player.getDropRate()
                                        : player.getBossDropRate());
                        NumberFormat nf = NumberFormat.getInstance();
                        nf.setMaximumFractionDigits(4);

                        double calculatedPercentage = 100 / chance;

                        String str = calculatedPercentage == Math.floor(calculatedPercentage) ? String.format("%.0f%%", calculatedPercentage) : String.format("%.2f%%", calculatedPercentage);
                        if (player.gmLevel() > 1) {
                            output.append("#i").append(drop.itemId).append("#   #z").append(drop.itemId).append("# (ID: ").append(drop.itemId).append(") (#b").append(str).append("#k)\r\n");
                        } else {
                            output.append("#i").append(drop.itemId).append("#   #z").append(drop.itemId).append("# (#b").append(str).append("#k)\r\n");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                output.append("\r\n");
            }
        }

        c.getAbstractPlayerInteraction().npcTalk(NpcId.MAPLE_ADMINISTRATOR, output.toString());
    }
}
