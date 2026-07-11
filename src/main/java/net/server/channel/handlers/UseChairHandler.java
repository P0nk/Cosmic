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
import client.inventory.InventoryType;
import constants.id.ItemId;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import server.ItemInformationProvider;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;
import server.movement.TeleportMovement;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class UseChairHandler extends AbstractPacketHandler {
    @Override
    public final void handlePacket(InPacket p, Client c) {
        int itemId = p.readInt();

        // thanks Darter (YungMoozi) for reporting unchecked chair item
        if (!ItemId.isChair(itemId) || c.getPlayer().getInventory(InventoryType.SETUP).findById(itemId) == null) {
            return;
        }

//        ItemInformationProvider ii = ItemInformationProvider.getInstance();
//
//        Point bodyRelMovePoint = ii.getBodyRelMove(itemId);
//        Point playerPosition = c.getPlayer().getPosition();
//        playerPosition.translate(bodyRelMovePoint.x, bodyRelMovePoint.y);
//
//        List<LifeMovementFragment> moves = new ArrayList<>();
//        TeleportMovement tm = new TeleportMovement((byte)11, new Point(playerPosition.x, playerPosition.y), c.getPlayer().getStance());
//        tm.setPixelsPerSecond(new Point(0, 0));
//
//        moves.add(tm);

        if (c.tryacquireClient()) {
            try {
                c.getPlayer().sitChair(itemId);
            } finally {
                c.releaseClient();
            }
        }
    }
}
