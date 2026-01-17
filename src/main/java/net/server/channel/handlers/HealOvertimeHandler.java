package net.server.channel.handlers;

import client.Character;
import client.Client;
import client.autoban.AutobanFactory;
import client.autoban.AutobanManager;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.server.Server;
import server.maps.MapleMap;
import tools.PacketCreator;

public final class HealOvertimeHandler extends AbstractPacketHandler {
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
            // [DISABLED] Fast HP Healing Check
            // Was causing false positives due to network lag (packet bunching).
            /* abm.setTimestamp(8, timestamp, 28);
            if ((abm.getLastSpam(0) + 1500) > timestamp) {
                AutobanFactory.FAST_HP_HEALING.addPoint(abm, "Fast hp healing");
            }
            */

            MapleMap map = chr.getMap();

            // [DISABLED] High HP Healing Check
            // The limit of ~77 HP is too low for high-level players or special chairs.
            /* int abHeal = (int) (77 * map.getRecovery() * 1.5); 
            if (healHP > abHeal) {
                AutobanFactory.HIGH_HP_HEALING.autoban(chr, "Healing: " + healHP + "; Max is " + abHeal + ".");
                return;
            }
            */

            // Apply the heal freely
            chr.addHP(healHP);
            chr.getMap().broadcastMessage(chr, PacketCreator.showHpHealed(chr.getId(), healHP), false);
            // abm.spam(0, timestamp); // No need to track spam if check is disabled
        }

        short healMP = p.readShort();
        if (healMP != 0) {
            // [DISABLED] Fast MP Healing Check
            /* abm.setTimestamp(9, timestamp, 28);
            if ((abm.getLastSpam(1) + 1500) > timestamp) {
                AutobanFactory.FAST_MP_HEALING.addPoint(abm, "Fast mp healing");
                return;
            }
            */

            // Allow MP heal if reasonable (e.g. < 1000 or just allow all)
            if (healMP < 1000) {
                chr.addMP(healMP);
            }
            // abm.spam(1, timestamp);
        }
    }
}