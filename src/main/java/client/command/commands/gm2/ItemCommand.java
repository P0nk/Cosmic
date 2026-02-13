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
import client.inventory.Pet;
import client.inventory.manipulator.InventoryManipulator;
import config.YamlConfig;
import constants.inventory.ItemConstants;
import server.ItemInformationProvider;

import static java.util.concurrent.TimeUnit.DAYS;

public class ItemCommand extends Command {
    {
        setDescription("Spawn an item into your inventory.");
    }

    @Override
    public void execute(Client c, String[] params, CommandContext ctx) {
        Character player = c.getPlayer();

        if (params.length < 1) {
            player.yellowMessage("Syntax: !item <itemid> <quantity>");
            return;
        }

        int itemId = Integer.parseInt(params[0]);
        ItemInformationProvider ii = ItemInformationProvider.getInstance();

        if (ii.getName(itemId) == null) {
            player.yellowMessage("Item id '" + params[0] + "' does not exist.");
            return;
        }

        short quantity = 1;
        if (params.length >= 2) {
            quantity = Short.parseShort(params[1]);
        }

        if (YamlConfig.config.server.BLOCK_GENERATE_CASH_ITEM && ii.isCash(itemId)) {
            player.yellowMessage("You cannot create a cash item with this command.");
            return;
        }

        if (ItemConstants.isPet(itemId)) {
            if (params.length >= 2) {   // thanks to istreety & TacoBell
                quantity = 1;
                long days = Math.max(1, Integer.parseInt(params[1]));
                long expiration = System.currentTimeMillis() + DAYS.toMillis(days);
                int petid = Pet.createPet(itemId);

                InventoryManipulator.addById(c, itemId, quantity, player.getName(), petid, expiration);
                return;
            } else {
                player.yellowMessage("Pet Syntax: !item <itemid> <expiration>");
                return;
            }
        }

        short flag = 0;
        if (player.gmLevel() < 3) {
            flag |= ItemConstants.ACCOUNT_SHARING;
            flag |= ItemConstants.UNTRADEABLE;
        }

        InventoryManipulator.addById(c, itemId, quantity, player.getName(), -1, flag, -1);
    }
}
