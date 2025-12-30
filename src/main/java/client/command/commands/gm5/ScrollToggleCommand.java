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

package client.command.commands.gm5;

import client.Client;
import client.command.Command;
import server.scrollshop.ScrollShopManager;

/**
 * Command to toggle the tosell status of a scroll item.
 */
public class ScrollToggleCommand extends Command {
    {
        setDescription("Toggles the tosell status of a scroll item. Usage: !scrolltoggle <itemid>");
    }

    @Override
    public void execute(Client c, String[] params) {
        // Check if the correct number of parameters is provided
        System.out.println("params: "+ params);
        if (params.length != 1) {
            c.getPlayer().dropMessage("Usage: !scrolltoggle <itemid>");
            return;
        }

        try {
            // Parse the item ID from the command parameters
            int itemid = Integer.parseInt(params[0]);

            // Call the toggletosell method from ScrollShopManager to toggle the sale status
            int result = ScrollShopManager.toggletoSell(itemid);

            // Send feedback to the GM based on the result
            if (result == 1) {
                c.getPlayer().dropMessage("Scroll with item ID " + itemid + " is now available for sale.");
            } else if (result == 0) {
                c.getPlayer().dropMessage("Scroll with item ID " + itemid + " is now not for sale.");
            } else {
                c.getPlayer().dropMessage("Scroll with item ID " + itemid + " not found.");
            }
        } catch (NumberFormatException e) {
            // Handle invalid item ID input
            c.getPlayer().dropMessage("Invalid item ID. Please provide a valid integer.");
        }
    }
}
