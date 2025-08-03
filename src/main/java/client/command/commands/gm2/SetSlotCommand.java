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
   @Author: Ronan
*/
package client.command.commands.gm2;

import client.Character;
import client.Client;
import client.command.Command;
import client.command.CommandContext;

public class SetSlotCommand extends Command {
    {
        setDescription("Set amount of inventory slots in all tabs.");
    }

    @Override
    public void execute(Client c, String[] params, CommandContext ctx) {
        Character player = c.getPlayer();
        if (params.length < 1) {
            player.yellowMessage("Syntax: !setslot <newlevel>");
            return;
        }

        int slots = (Integer.parseInt(params[0]) / 4) * 4;
        for (int i = 1; i < 5; i++) {
            int curSlots = player.getSlots(i);
            if (slots <= -curSlots) {
                continue;
            }

            player.gainSlots(i, slots - curSlots, true);
        }

        player.yellowMessage("Slots updated.");
    }
}
