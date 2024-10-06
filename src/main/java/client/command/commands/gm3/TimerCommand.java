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
   @Author: MedicOP - Add clock commands
*/
package client.command.commands.gm3;

import client.Character;
import client.Client;
import client.command.Command;
import client.command.CommandContext;
import tools.PacketCreator;

public class TimerCommand extends Command {
    {
        setDescription("Set timer on a player in current map.");
    }

    @Override
    public void execute(Client c, String[] params, CommandContext ctx) {
        Character player = c.getPlayer();
        if (params.length < 2) {
            player.yellowMessage("Syntax: !timer <playername> <seconds>|remove");
            return;
        }

        Character victim = c.getWorldServer().getPlayerStorage().getCharacterByName(params[0]);
        if (victim != null) {
            if (params[1].equalsIgnoreCase("remove")) {
                victim.sendPacket(PacketCreator.removeClock());
            } else {
                try {
                    victim.sendPacket(PacketCreator.getClock(Integer.parseInt(params[1])));
                } catch (NumberFormatException e) {
                    player.yellowMessage("Syntax: !timer <playername> <seconds>|remove");
                }
            }
        } else {
            player.message("Player '" + params[0] + "' could not be found.");
        }
    }
}
