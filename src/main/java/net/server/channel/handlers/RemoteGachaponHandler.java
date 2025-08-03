/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

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
package net.server.channel.handlers;

import client.Client;
import client.autoban.AutobanFactory;
import constants.id.ItemId;
import constants.id.NpcId;
import constants.inventory.ItemConstants;
import net.AbstractPacketHandler;
import net.netty.GameViolationException;
import net.packet.InPacket;
import scripting.npc.NPCScriptManager;

/**
 * @author Generic
 */
public final class RemoteGachaponHandler extends AbstractPacketHandler {
    @Override
    public final void handlePacket(InPacket p, Client c) {
        int ticket = p.readInt();
        int gacha = p.readInt();
        if (ticket != ItemId.REMOTE_GACHAPON_TICKET) {
            AutobanFactory.GENERAL.alert(c.getPlayer(), " Tried to use RemoteGachaponHandler with item id: " + ticket);
            throw new GameViolationException("Use Remote gachapon without owning the item");
        } else if (gacha < 0 || gacha > 11) {
            AutobanFactory.GENERAL.alert(c.getPlayer(), " Tried to use RemoteGachaponHandler with mode: " + gacha);
            throw new GameViolationException("Use Remote gachapon with invalid mode");
        } else if (c.getPlayer().getInventory(ItemConstants.getInventoryType(ticket)).countById(ticket) < 1) {
            AutobanFactory.GENERAL.alert(c.getPlayer(), " Tried to use RemoteGachaponHandler without a ticket.");
            throw new GameViolationException("Use Remote gachapon without a ticket");
        }
        int npcId = NpcId.GACHAPON_HENESYS;
        if (gacha != 8 && gacha != 9) {
            npcId += gacha;
        } else {
            npcId = gacha == 8 ? NpcId.GACHAPON_NLC : NpcId.GACHAPON_NAUTILUS;
        }
        NPCScriptManager.getInstance().start(c, npcId, "gachaponRemote");
    }
}
