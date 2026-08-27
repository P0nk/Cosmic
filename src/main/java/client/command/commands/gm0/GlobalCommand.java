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
package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import server.ChatLogger;
import tools.PacketCreator;

public class GlobalCommand extends Command {
    {
        setDescription("Send a message to every player in the world.");
    }

    @Override
    public void execute(Client client, String[] params) {
        Character player = client.getPlayer();
        String message = player.getLastCommandMessage();
        if (message == null || message.isBlank()) {
            player.yellowMessage("Usage: @global <message>");
            return;
        }

        String formattedMessage = "[Global] " + player.getName() + ": " + message;
        client.getWorldServer().broadcastPacket(PacketCreator.serverNotice(6, formattedMessage));
        ChatLogger.log(client, "Global", message);
    }
}
