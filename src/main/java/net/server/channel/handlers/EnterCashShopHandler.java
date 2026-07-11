/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package net.server.channel.handlers;

import client.Character;
import client.Client;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.server.Server;
import scripting.npc.NPCScriptManager;
import server.maps.MiniDungeonInfo;
import tools.PacketCreator;


/**
 * @author Flav
 */
public class EnterCashShopHandler extends AbstractPacketHandler {
    @Override
    public void handlePacket(InPacket p, Client c) {
        NPCScriptManager.getInstance().start(c, 9010004, c.getPlayer());
        c.sendPacket(PacketCreator.enableActions());
    }
}
