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
package client.command.commands.gm3;

import client.Character;
import client.Client;
import client.command.Command;
import client.command.CommandContext;
import net.packet.logging.MonitoredChrLogger;
import net.server.Server;
import tools.PacketCreator;

public class MonitorCommand extends Command {
    {
        setDescription("Toggle monitored packet logging of a character.");
    }

    @Override
    public void execute(Client c, String[] params, CommandContext ctx) {
        Character player = c.getPlayer();
        if (params.length < 1) {
            player.yellowMessage("Syntax: !monitor <ign>");
            return;
        }
        Character victim = c.getWorldServer().getPlayerStorage().getCharacterByName(params[0]);
        if (victim == null) {
            player.message("Player '" + params[0] + "' could not be found on this world.");
            return;
        }
        boolean monitored = MonitoredChrLogger.toggleMonitored(victim.getId());
        player.yellowMessage(victim.getId() + " is " + (monitored ? "now being monitored." : "no longer being monitored."));
        String message = player.getName() + (monitored ? " has started monitoring " : " has stopped monitoring ") + victim.getId() + ".";
        Server.getInstance().broadcastGMMessage(c.getWorld(), PacketCreator.serverNotice(5, message));

    }
}
