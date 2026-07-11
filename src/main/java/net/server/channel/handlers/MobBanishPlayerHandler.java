/*
    This file is part of the HeavenMS MapleStory Server
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
package net.server.channel.handlers;

import client.Character;
import client.Client;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import server.life.LifeFactory.BanishInfo;
import server.life.Monster;

public final class MobBanishPlayerHandler extends AbstractPacketHandler {

    @Override
    public final void handlePacket(InPacket p, Client c) {
        int mobid = p.readInt();     // mob banish handling detected thanks to MedicOP

        Character chr = c.getPlayer();
        Monster mob = chr.getMap().getMonsterById(mobid);

        if (mob != null) {
            BanishInfo banishInfo = mob.getBanish();
            if (banishInfo != null) {
                chr.changeMapBanish(banishInfo.getMap(), banishInfo.getPortal(), banishInfo.getMsg());
            }
        }
    }
}