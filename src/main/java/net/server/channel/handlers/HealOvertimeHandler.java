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
import client.Client;
import client.autoban.AutobanFactory;
import client.autoban.AutobanManager;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.server.Server;
import server.maps.MapleMap;
import service.BanService;
import tools.PacketCreator;

public final class HealOvertimeHandler extends AbstractPacketHandler {
    private final BanService banService;

    public HealOvertimeHandler(BanService banService) {
        this.banService = banService;
    }

    @Override
    public final void handlePacket(InPacket p, Client c) {
        Character chr = c.getPlayer();
        if (!chr.isLoggedinWorld()) {
            return;
        }

        AutobanManager abm = chr.getAutobanManager();
        int timestamp = Server.getInstance().getCurrentTimestamp();
        p.skip(8);

        short healHP = p.readShort();
        if (healHP != 0) {
            abm.setTimestamp(8, timestamp, 28);  // thanks Vcoc & Thora for pointing out d/c happening here
            if ((abm.getLastSpam(0) + 1500) > timestamp) {
                banService.addPoint(chr, AutobanFactory.FAST_HP_HEALING, "Fast hp healing");
            }

            MapleMap map = chr.getMap();
            int abHeal = (int) (77 * map.getRecovery() * 1.5); // thanks Ari for noticing players not getting healed in sauna in certain cases
            if (healHP > abHeal) {
                banService.autoban(chr, AutobanFactory.HIGH_HP_HEALING, "Healing: " + healHP + "; Max is " + abHeal + ".");
                return;
            }

            chr.addHP(healHP);
            chr.getMap().broadcastMessage(chr, PacketCreator.showHpHealed(chr.getId(), healHP), false);
            abm.spam(0, timestamp);
        }
        short healMP = p.readShort();
        if (healMP != 0 && healMP < 1000) {
            abm.setTimestamp(9, timestamp, 28);
            if ((abm.getLastSpam(1) + 1500) > timestamp) {
                banService.addPoint(chr, AutobanFactory.FAST_MP_HEALING, "Fast mp healing");
                return;     // thanks resinate for noticing mp being gained even after detection
            }
            chr.addMP(healMP);
            abm.spam(1, timestamp);
        }
    }
}
