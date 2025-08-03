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
import client.command.CommandContext;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.PacketCreator;

public class ReportBugCommand extends Command {
    {
        setDescription("Send in a bug report.");
    }

    private static final Logger log = LoggerFactory.getLogger(ReportBugCommand.class);

    @Override
    public void execute(Client c, String[] params, CommandContext ctx) {
        Character player = c.getPlayer();

        if (params.length < 1) {
            player.dropMessage(5, "Message too short and not sent. Please do @bug <bug>");
            return;
        }
        String message = player.getLastCommandMessage();
        Server.getInstance().broadcastGMMessage(c.getWorld(), PacketCreator.sendYellowTip("[Bug]:" + Character.makeMapleReadable(player.getName()) + ": " + message));
        Server.getInstance().broadcastGMMessage(c.getWorld(), PacketCreator.serverNotice(1, message));
        log.info("{}: {}", Character.makeMapleReadable(player.getName()), message);
        player.dropMessage(5, "Your bug '" + message + "' was submitted successfully to our developers. Thank you!");

    }
}
