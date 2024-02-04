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
import constants.id.MapId;
import server.maps.MapleMap;
import server.maps.Portal;

import static java.util.concurrent.TimeUnit.MINUTES;

public class JailCommand extends Command {
    {
        setDescription("Move a player to the jail.");
    }

    @Override
    public void execute(Client c, String[] params, CommandContext ctx) {
        Character player = c.getPlayer();
        if (params.length < 1) {
            player.yellowMessage("Syntax: !jail <playername> [<minutes>]");
            return;
        }

        int minutesJailed = 5;
        if (params.length >= 2) {
            minutesJailed = Integer.parseInt(params[1]);
            if (minutesJailed <= 0) {
                player.yellowMessage("Syntax: !jail <playername> [<minutes>]");
                return;
            }
        }

        Character victim = c.getWorldServer().getPlayerStorage().getCharacterByName(params[0]);
        if (victim != null) {
            victim.addJailExpirationTime(MINUTES.toMillis(minutesJailed));

            if (victim.getMapId() != MapId.JAIL) {    // those gone to jail won't be changing map anyway
                MapleMap target = c.getChannelServer().getMapFactory().getMap(MapId.JAIL);
                Portal targetPortal = target.getPortal(0);
                victim.saveLocationOnWarp();
                victim.changeMap(target, targetPortal);
                player.message(victim.getName() + " was jailed for " + minutesJailed + " minutes.");
            } else {
                player.message(victim.getName() + "'s time in jail has been extended for " + minutesJailed + " minutes.");
            }

        } else {
            player.message("Player '" + params[0] + "' could not be found.");
        }
    }
}
