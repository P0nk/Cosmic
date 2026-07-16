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
import config.YamlConfig;

public class RatesCommand extends Command {

    public RatesCommand() {
        setDescription("Display the current ForgeMS server rates.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        StringBuilder rates = new StringBuilder();

        rates.append("#e========== ForgeMS Server Rates ==========#n\r\n\r\n");

        rates.append("#bEXP Rate:#k ")
                .append(player.getExpRate())
                .append("x");

        if (player.hasNoviceExpRate()) {
            rates.append(" #g(Novice Rate)#k");
        }

        rates.append("\r\n");

        rates.append("#bMeso Rate:#k ")
                .append(player.getMesoRate())
                .append("x\r\n");

        rates.append("#bDrop Rate:#k ")
                .append(player.getDropRate())
                .append("x\r\n");

        rates.append("#bBoss Drop Rate:#k ")
                .append(player.getBossDropRate())
                .append("x\r\n");

        if (YamlConfig.config.server.USE_QUEST_RATE) {
            rates.append("#bQuest EXP Rate:#k ")
                    .append(c.getWorldServer().getQuestRate())
                    .append("x\r\n");
        }

        rates.append("\r\n");
        rates.append("#dRebirth Level:#k 200\r\n");
        rates.append("#dAuto Rebirth:#k Available after your first manual rebirth\r\n");

        rates.append("\r\n");
        rates.append("#ePrestige is earned. Power is forged.#n\r\n");
        rates.append("============================================");

        player.showHint(rates.toString(), 400);
    }
}
