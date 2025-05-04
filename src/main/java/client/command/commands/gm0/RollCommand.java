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

import client.Client;
import client.Character;
import client.command.Command;
import net.server.Server;
import tools.PacketCreator;
import java.util.Random;

public class RollCommand extends Command {
    {
        setDescription("Rolls a random number between 0 and the specified upper limit and announces it globally.");
    }

    @Override
    public void execute(Client client, String[] params) {
        if (params.length < 1) {
            client.getPlayer().dropMessage("Usage: @roll <upper_limit>");
            return;
        }

        try {
            int upperLimit = Integer.parseInt(params[0]);
            if (upperLimit < 0) {
                client.getPlayer().dropMessage("Upper limit must be a non-negative number.");
                return;
            }

            int rolledNumber = new Random().nextInt(upperLimit + 1);
            String message = client.getPlayer().getName() + " rolled " + rolledNumber + "! From 0 to " + upperLimit;

            // Send global announcement
            Server.getInstance().broadcastMessage(client.getWorld(), PacketCreator.serverNotice(6, "[Roll] " + message));

        } catch (NumberFormatException e) {
            client.getPlayer().dropMessage("Invalid number format. Please enter a valid integer.");
        }
    }
}