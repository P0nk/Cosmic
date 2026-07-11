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
import client.autoban.AutobanManager;
import client.inventory.InventoryType;
import client.inventory.manipulator.InventoryManipulator;
import constants.id.ItemId;
import constants.id.MobId;
import constants.inventory.ItemConstants;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.server.Server;
import server.ItemInformationProvider;
import server.life.Monster;
import tools.PacketCreator;

/**
 * @author kevintjuh93
 */
public final class UseCatchItemHandler extends AbstractPacketHandler {
    @Override
    public final void handlePacket(InPacket p, Client c) {
        Character chr = c.getPlayer();
        AutobanManager abm = chr.getAutobanManager();
        p.readInt();
        abm.setTimestamp(5, Server.getInstance().getCurrentTimestamp(), 4);
        p.readShort();
        int itemId = p.readInt();
        int monsterid = p.readInt();

        Monster mob = chr.getMap().getMonsterByOid(monsterid);
        if (chr.getInventory(ItemConstants.getInventoryType(itemId)).countById(itemId) <= 0) {
            return;
        }
        if (mob == null) {
            return;
        }
        switch (itemId) {
            case ItemId.PHEROMONE_PERFUME:
                if (mob.getId() == MobId.TAMABLE_HOG) {
                    if (mob.getHp() < ((mob.getMaxHp() / 10) * 4)) {
                        chr.getMap().broadcastMessage(PacketCreator.catchMonster(monsterid, itemId, (byte) 1));
                        mob.getMap().killMonster(mob, null, false);
                        InventoryManipulator.removeById(c, InventoryType.USE, itemId, 1, true, true);
                        InventoryManipulator.addById(c, ItemId.HOG, (short) 1, "", -1);
                    }
                }
                c.sendPacket(PacketCreator.enableActions());
                break;
            case ItemId.MUTANT_PIG_ROPE:
                if (mob.getId() == MobId.MUTANT_PIG) {
                    if (mob.getHp() < ((mob.getMaxHp() / 10) * 4)) {
                        chr.getMap().broadcastMessage(PacketCreator.catchMonster(monsterid, itemId, (byte) 1));
                        mob.getMap().killMonster(mob, null, false);
                        InventoryManipulator.removeById(c, InventoryType.USE, itemId, 1, true, true);
                        InventoryManipulator.addById(c, ItemId.TAMED_MUTANT_PIG, (short) 1, "", -1);
                    } else {
                        c.sendPacket(PacketCreator.catchMessage(0));
                    }
                }
                c.sendPacket(PacketCreator.enableActions());
                break;
            case ItemId.POUCH:
                if (mob.getId() == MobId.GHOST) {
                    if ((abm.getLastSpam(10) + 1000) < currentServerTime()) {
                        if (mob.getHp() < ((mob.getMaxHp() / 10) * 4)) {
                            chr.getMap().broadcastMessage(PacketCreator.catchMonster(monsterid, itemId, (byte) 1));
                            mob.getMap().killMonster(mob, null, false);
                            InventoryManipulator.removeById(c, InventoryType.USE, itemId, 1, true, true);
                            InventoryManipulator.addById(c, ItemId.GHOST_SACK, (short) 1, "", -1);
                        } else {
                            abm.spam(10);
                            c.sendPacket(PacketCreator.catchMessage(0));
                        }
                    }
                    c.sendPacket(PacketCreator.enableActions());
                }
                break;
            case ItemId.ARPQ_ELEMENT_ROCK:
                if (mob.getId() == MobId.ARPQ_SCORPION) {
                    if ((abm.getLastSpam(10) + 800) < currentServerTime()) {
                        if (mob.getHp() < ((mob.getMaxHp() / 10) * 4)) {
                            if (chr.canHold(ItemId.ARPQ_SPIRIT_JEWEL, 1)) {
                                if (Math.random() < 0.5) { // 50% chance
                                    chr.getMap().broadcastMessage(PacketCreator.catchMonster(monsterid, itemId, (byte) 1));
                                    mob.getMap().killMonster(mob, null, false);
                                    InventoryManipulator.removeById(c, InventoryType.USE, itemId, 1, true, true);
                                    InventoryManipulator.addById(c, ItemId.ARPQ_SPIRIT_JEWEL, (short) 1, "", -1);
                                } else {
                                    chr.getMap().broadcastMessage(PacketCreator.catchMonster(monsterid, itemId, (byte) 0));
                                }
                            } else {
                                chr.dropMessage(5, "Make a ETC slot available before using this item.");
                            }

                            abm.spam(10);
                        } else {
                            c.sendPacket(PacketCreator.catchMessage(0));
                        }
                    }
                    c.sendPacket(PacketCreator.enableActions());
                }
                break;
            case ItemId.MAGIC_CANE:
                if (mob.getId() == MobId.LOST_RUDOLPH) {
                    if (mob.getHp() < ((mob.getMaxHp() / 10) * 4)) {
                        chr.getMap().broadcastMessage(PacketCreator.catchMonster(monsterid, itemId, (byte) 1));
                        mob.getMap().killMonster(mob, null, false);
                        InventoryManipulator.removeById(c, InventoryType.USE, itemId, 1, true, true);
                        InventoryManipulator.addById(c, ItemId.TAMED_RUDOLPH, (short) 1, "", -1);
                    } else {
                        c.sendPacket(PacketCreator.catchMessage(0));
                    }
                }
                c.sendPacket(PacketCreator.enableActions());
                break;
            case ItemId.TRANSPARENT_MARBLE_1:
                if (mob.getId() == MobId.KING_SLIME_DOJO) {
                    if (mob.getHp() < ((mob.getMaxHp() / 10) * 3)) {
                        chr.getMap().broadcastMessage(PacketCreator.catchMonster(monsterid, itemId, (byte) 1));
                        mob.getMap().killMonster(mob, null, false);
                        InventoryManipulator.removeById(c, InventoryType.USE, itemId, 1, true, true);
                        InventoryManipulator.addById(c, ItemId.MONSTER_MARBLE_1, (short) 1, "", -1);
                    } else {
                        c.sendPacket(PacketCreator.catchMessage(0));
                    }
                }
                c.sendPacket(PacketCreator.enableActions());
                break;
            case ItemId.TRANSPARENT_MARBLE_2:
                if (mob.getId() == MobId.FAUST_DOJO) {
                    if (mob.getHp() < ((mob.getMaxHp() / 10) * 3)) {
                        chr.getMap().broadcastMessage(PacketCreator.catchMonster(monsterid, itemId, (byte) 1));
                        mob.getMap().killMonster(mob, null, false);
                        InventoryManipulator.removeById(c, InventoryType.USE, itemId, 1, true, true);
                        InventoryManipulator.addById(c, ItemId.MONSTER_MARBLE_2, (short) 1, "", -1);
                    } else {
                        c.sendPacket(PacketCreator.catchMessage(0));
                    }
                }
                c.sendPacket(PacketCreator.enableActions());
                break;
            case ItemId.TRANSPARENT_MARBLE_3:
                if (mob.getId() == MobId.MUSHMOM_DOJO) {
                    if (mob.getHp() < ((mob.getMaxHp() / 10) * 3)) {
                        chr.getMap().broadcastMessage(PacketCreator.catchMonster(monsterid, itemId, (byte) 1));
                        mob.getMap().killMonster(mob, null, false);
                        InventoryManipulator.removeById(c, InventoryType.USE, itemId, 1, true, true);
                        InventoryManipulator.addById(c, ItemId.MONSTER_MARBLE_3, (short) 1, "", -1);
                    } else {
                        c.sendPacket(PacketCreator.catchMessage(0));
                    }
                }
                c.sendPacket(PacketCreator.enableActions());
                break;
            case ItemId.EPQ_PURIFICATION_MARBLE:
                if (mob.getId() == MobId.POISON_FLOWER) {
                    if (mob.getHp() < ((mob.getMaxHp() / 10) * 4)) {
                        chr.getMap().broadcastMessage(PacketCreator.catchMonster(monsterid, itemId, (byte) 1));
                        mob.getMap().killMonster(mob, null, false);
                        InventoryManipulator.removeById(c, InventoryType.USE, itemId, 1, true, true);
                        InventoryManipulator.addById(c, ItemId.EPQ_MONSTER_MARBLE, (short) 1, "", -1);
                    } else {
                        c.sendPacket(PacketCreator.catchMessage(0));
                    }
                }
                c.sendPacket(PacketCreator.enableActions());
                break;
            case ItemId.FISH_NET:
                if (mob.getId() == MobId.P_JUNIOR) {
                    if ((abm.getLastSpam(10) + 3000) < currentServerTime()) {
                        abm.spam(10);
                        chr.getMap().broadcastMessage(PacketCreator.catchMonster(monsterid, itemId, (byte) 1));
                        mob.getMap().killMonster(mob, null, false);
                        InventoryManipulator.removeById(c, InventoryType.USE, itemId, 1, true, true);
                        InventoryManipulator.addById(c, ItemId.FISH_NET_WITH_A_CATCH, (short) 1, "", -1);
                    } else {
                        chr.message("You cannot use the Fishing Net yet.");
                    }
                    c.sendPacket(PacketCreator.enableActions());
                }
                break;
            default:
                // proper Fish catch, thanks to Dragohe4rt

                ItemInformationProvider ii = ItemInformationProvider.getInstance();
                int itemGanho = ii.getCreateItem(itemId);
                int mobItem = ii.getMobItem(itemId);

                if (itemGanho != 0 && mobItem == mob.getId()) {
                    int timeCatch = ii.getUseDelay(itemId);
                    int mobHp = ii.getMobHP(itemId);

                    if (timeCatch != 0 && (abm.getLastSpam(10) + timeCatch) < currentServerTime()) {
                        if (mobHp != 0 && mob.getHp() < ((mob.getMaxHp() / 100) * mobHp)) {
                            chr.getMap().broadcastMessage(PacketCreator.catchMonster(monsterid, itemId, (byte) 1));
                            mob.getMap().killMonster(mob, null, false);
                            InventoryManipulator.removeById(c, InventoryType.USE, itemId, 1, true, true);
                            InventoryManipulator.addById(c, itemGanho, (short) 1, "", -1);
                        } else if (mob.getId() != MobId.P_JUNIOR) {
                            if (mobHp != 0) {
                                abm.spam(10);
                                c.sendPacket(PacketCreator.catchMessage(0));
                            }
                        } else {
                            chr.message("You cannot use the Fishing Net yet.");
                        }
                    }
                }
                c.sendPacket(PacketCreator.enableActions());

                // System.out.println("UseCatchItemHandler: \r\n" + slea.toString());
        }
    }
}
