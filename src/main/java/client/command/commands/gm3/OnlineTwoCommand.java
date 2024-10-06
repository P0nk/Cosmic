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
package client.command.commands.gm3;

import client.Character;
import client.Client;
import client.command.Command;
import client.command.CommandContext;
import net.server.Server;
import net.server.channel.Channel;

public class OnlineTwoCommand extends Command {
    {
        setDescription("Show all online players.");
    }

    @Override
    public void execute(Client c, String[] params, CommandContext ctx) {
        Character player = c.getPlayer();
        int total = 0;
        for (Channel ch : Server.getInstance().getChannelsFromWorld(player.getWorld())) {
            int size = ch.getPlayerStorage().getAllCharacters().size();
            total += size;
            String s = "(Channel " + ch.getId() + " Online: " + size + ") : ";
            if (ch.getPlayerStorage().getAllCharacters().size() < 50) {
                for (Character chr : ch.getPlayerStorage().getAllCharacters()) {
                    s += Character.makeMapleReadable(chr.getName()) + ", ";
                }
                player.dropMessage(6, s.substring(0, s.length() - 2));
            }
        }

        //player.dropMessage(6, "There are a total of " + total + " players online.");
        player.showHint("Players online: #e#r" + total + "#k#n.", 300);
    }
}
