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
package client.command.commands.gm1;

import client.Character;
import client.Client;
import client.command.Command;
import server.events.EventManager;

public class BossRaidCommand extends Command {
    {
        setDescription("Starts the Boss Raid event manually. Usage: @bossraid [BossName]");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        EventManager em = c.getChannelServer().getEventSM().getEventManager("BossRaid");

        if (em == null) {
            player.dropMessage(5, "BossRaid event script not found.");
            return;
        }

        String bossName = null;
        if (params.length > 0) {
            bossName = params[0];
        }

        // Invoke forceStart(bossName)
        em.invokeFunction("forceStart", bossName);
        player.dropMessage(6, "Boss Raid triggered" + (bossName != null ? " for " + bossName : " (Random)") + ".");
    }
}
