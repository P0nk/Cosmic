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
package client.command.commands.gm6;

import client.Character;
import client.Client;
import client.command.Command;
import client.command.CommandContext;
import net.server.Server;
import net.server.world.World;
import server.TimerManager;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class ShutdownCommand extends Command {
    {
        setDescription("Shut down the server.");
    }

    @Override
    public void execute(Client c, String[] params, CommandContext ctx) {
        Character player = c.getPlayer();
        if (params.length < 1) {
            player.yellowMessage("Syntax: !shutdown [<time>|NOW]");
            return;
        }

        int time = 60000;
        if (params[0].equalsIgnoreCase("now")) {
            time = 1;
        } else {
            time *= Integer.parseInt(params[0]);
        }

        if (time > 1) {
            int seconds = (time / (int) SECONDS.toMillis(1)) % 60;
            int minutes = (time / (int) MINUTES.toMillis(1)) % 60;
            int hours = (time / (int) HOURS.toMillis(1)) % 24;
            int days = (time / (int) DAYS.toMillis(1));

            String strTime = "";
            if (days > 0) {
                strTime += days + " days, ";
            }
            if (hours > 0) {
                strTime += hours + " hours, ";
            }
            strTime += minutes + " minutes, ";
            strTime += seconds + " seconds";

            for (World w : Server.getInstance().getWorlds()) {
                for (Character chr : w.getPlayerStorage().getAllCharacters()) {
                    chr.dropMessage("Server is undergoing maintenance process, and will be shutdown in " + strTime + ". Prepare yourself to quit safely in the mean time.");
                }
            }
        }

        TimerManager.getInstance().schedule(Server.getInstance().shutdown(false), time);
    }
}
