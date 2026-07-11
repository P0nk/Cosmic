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
import constants.id.MobId;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import server.life.Monster;
import tools.PacketCreator;

public final class MonsterBombHandler extends AbstractPacketHandler {
    private static boolean isBombMob(int mobid) {
        return switch (mobid) {
            case MobId.HIGH_DARKSTAR, MobId.LOW_DARKSTAR, 9600069 -> true;
            default -> false;
        };

    }

    @Override
    public final void handlePacket(InPacket p, Client c) {
        int oid = p.readInt();
        Monster monster = c.getPlayer().getMap().getMonsterByOid(oid);
        if (!c.getPlayer().isAlive() || monster == null) {
            return;
        }
        //if ((monster.getStats().selfDestruction().getAction()& 2) !=0) maybe use when more than 10 mobs?
        if (isBombMob(monster.getId())) {
            monster.dispatchMonsterKilled(false);
            monster.getMap().broadcastMessage(PacketCreator.killMonster(monster.getObjectId(), 4));
            c.getPlayer().getMap().removeMapObject(oid);
        }
    }
}
