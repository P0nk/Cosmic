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
package server.life;

import client.Client;
import server.maps.MapObjectType;
import tools.PacketCreator;

public class NPC extends AbstractLoadedLife {
    private final NPCStats stats;

    public NPC(int id, NPCStats stats) {
        super(id);
        this.stats = stats;
    }

    @Override
    public void sendSpawnData(Client client) {
        client.sendPacket(PacketCreator.spawnNPC(this));
        client.sendPacket(PacketCreator.spawnNPCRequestController(this, true));
    }

    @Override
    public void sendDestroyData(Client client) {
        client.sendPacket(PacketCreator.removeNPCController(getObjectId()));
        client.sendPacket(PacketCreator.removeNPC(getObjectId()));
    }

    @Override
    public MapObjectType getType() {
        return MapObjectType.NPC;
    }

    public String getName() {
        return stats.getName();
    }
}
