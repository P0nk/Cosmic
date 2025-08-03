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

import client.Character;
import client.Character.FameStatus;
import client.Client;
import client.autoban.AutobanFactory;
import net.AbstractPacketHandler;
import net.netty.GameViolationException;
import net.packet.InPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.PacketCreator;

public final class GiveFameHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(GiveFameHandler.class);

    @Override
    public void handlePacket(InPacket p, Client c) {
        Character target = (Character) c.getPlayer().getMap().getMapObject(p.readInt());
        int mode = p.readByte();
        int famechange = 2 * mode - 1;
        Character player = c.getPlayer();
        if (target == null || target.getId() == player.getId() || player.getLevel() < 15) {
            return;
        } else if (famechange != 1 && famechange != -1) {
            AutobanFactory.PACKET_EDIT.alert(c.getPlayer(), c.getPlayer().getName() + " tried to packet edit fame.");
            log.warn("Chr {} tried to fame hack with famechange {}", c.getPlayer().getName(), famechange);
            throw new GameViolationException("Give too much fame");
        }

        FameStatus status = player.canGiveFame(target);
        if (status == FameStatus.OK) {
            if (target.gainFame(famechange, player, mode)) {
                if (!player.isGM()) {
                    player.hasGivenFame(target);
                }
            } else {
                player.message("Could not process the request, since this character currently has the minimum/maximum level of fame.");
            }
        } else {
            c.sendPacket(PacketCreator.giveFameErrorResponse(status == FameStatus.NOT_TODAY ? 3 : 4));
        }
    }
}
