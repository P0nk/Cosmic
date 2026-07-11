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
import client.CharacterManipulator;
import client.Client;
import client.Stat;
import client.command.Command;
import client.command.CommandHelpers;

public class StatLukCommand extends Command {
    {
        setDescription("Assign AP into LUK.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        int current = player.getLuk();

        Integer amount = CommandHelpers.parseApAmount(params, player, current);
        // No need to send a message parseAmount did for us
        if (amount == null) return;

        if (!CharacterManipulator.adjustStat(c, Stat.LUK, amount)) {
            player.dropMessage("Please make sure you have enough AP to distribute.");
        }
    }
}
