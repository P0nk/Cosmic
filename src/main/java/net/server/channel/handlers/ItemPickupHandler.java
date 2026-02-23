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
import net.AbstractPacketHandler;
import net.packet.InPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.maps.MapObject;

import java.awt.*;

/**
 * @author Matze
 * @author Ronan
 */
public final class ItemPickupHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(ItemPickupHandler.class);

    @Override
    public void handlePacket(final InPacket p, final Client c) {
        p.readInt(); // Timestamp
        p.readByte();
        p.readPos(); // cpos
        int oid = p.readInt();
        Character chr = c.getPlayer();
        MapObject ob = chr.getMap().getMapObject(oid);
        if (ob == null) {
            return;
        }

        Point charPos = chr.getPosition();
        Point obPos = ob.getPosition();
        if (Math.abs(charPos.getX() - obPos.getX()) > 800 || Math.abs(charPos.getY() - obPos.getY()) > 600) {
            log.warn("Chr {} tried to pick up an item too far away. Mapid: {}, player pos: {}, object pos: {}",
                    c.getPlayer().getName(), chr.getMapId(), charPos, obPos);
            return;
        }

        if (ob instanceof server.maps.MapItem) {
            server.maps.MapItem mapItem = (server.maps.MapItem) ob;

            // [RESTRICTION] Bera Ironman - Cannot pick up items dropped by other players
            if (chr.getWorld() == 1) {
                if (mapItem.isPlayerDrop() && mapItem.getOwnerId() != chr.getId()) {
                    chr.dropMessage(5, "As an Ironman, you cannot pick up drops belonging to other players.");
                    c.sendPacket(tools.PacketCreator.enableActions());
                    return;
                }
            }
        }

        if (chr.isAutoSellEnabled() && ob instanceof server.maps.MapItem) {
            server.maps.MapItem mapItem = (server.maps.MapItem) ob;
            client.inventory.Item item = mapItem.getItem();

            // [RESTRICTION] Auto-Sell only applies to Equips
            if (item.getInventoryType() != client.inventory.InventoryType.EQUIP) {
                chr.pickupItem(ob);
                return;
            }

            // [GLOBAL EXCLUSION] Pink Bean Summon Item
            if (item.getItemId() == 4001193) {
                return;
            }

            // Try to sell
            int gain = client.command.commands.gm0.SellAllCommand.sellItem(c, chr, item, null);

            if (gain > 0) {
                chr.gainMeso(gain, true);
                if (chr.getMapKillCount() % 5 == 0) { // Limit spam, or just show concise message
                    chr.dropMessage(5, "Auto-sold item for " + gain + " mesos.");
                }

                // Remove from map visually and logically
                // Remove from map visually and logically using proper cleanup
                chr.getMap().pickItemDrop(
                        tools.PacketCreator.removeItemFromMap(ob.getObjectId(), 2, chr.getId()),
                        (server.maps.MapItem) ob);
                return;
            }
        }

        chr.pickupItem(ob);
    }
}
