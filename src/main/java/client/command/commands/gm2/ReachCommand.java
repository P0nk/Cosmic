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
package client.command.commands.gm2;

import client.Character;
import client.Client;
import client.command.Command;
import client.command.CommandContext;
import server.maps.MapleMap;

public class ReachCommand extends Command {
    {
        setDescription("Warp to a player.");
    }

    @Override
    public void execute(Client c, String[] params, CommandContext ctx) {
        Character player = c.getPlayer();
        if (params.length < 1) {
            player.yellowMessage("Syntax: !reach <playername>");
            return;
        }

        Character victim = c.getWorldServer().getPlayerStorage().getCharacterByName(params[0]);
        if (victim != null && victim.isLoggedin()) {
            if (player.getClient().getChannel() != victim.getClient().getChannel()) {
                player.dropMessage(5, "Player '" + victim.getName() + "' is at channel " + victim.getClient().getChannel() + ".");
            } else {
                MapleMap map = victim.getMap();
                player.saveLocationOnWarp();
                player.forceChangeMap(map, map.findClosestPortal(victim.getPosition()));
            }
        } else {
            player.dropMessage(6, "Unknown player.");
        }
    }
}
