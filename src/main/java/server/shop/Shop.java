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
package server.shop;

import client.Client;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.Pet;
import client.inventory.manipulator.InventoryManipulator;
import constants.id.ItemId;
import constants.inventory.ItemConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ItemInformationProvider;
import tools.PacketCreator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Matze
 */
public class Shop {
    private static final Logger log = LoggerFactory.getLogger(Shop.class);

    private final int id;
    private final int npcId;
    private final List<ShopItem> items;
    private final int tokenvalue = 1000000000;
    private final int token = ItemId.GOLDEN_MAPLE_LEAF;

    public Shop(int id, int npcId, List<ShopItem> items) {
        this.id = id;
        this.npcId = npcId;
        this.items = new ArrayList<>(items);
    }

    public void sendShop(Client c) {
        c.getPlayer().setShop(this);
        c.sendPacket(PacketCreator.getNPCShop(c, getNpcId(), items));
    }

    public void buy(Client c, short slot, int itemId, short quantity) {
        ShopItem item = findBySlot(slot);
        if (item != null) {
            if (item.itemId() != itemId) {
                log.warn("Wrong slot number in shop {}", id);
                return;
            }
        } else {
            return;
        }
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        if (item.price() > 0) {
            int amount = (int) Math.min((float) item.price() * quantity, Integer.MAX_VALUE);
            if (c.getPlayer().getMeso() >= amount) {
                if (InventoryManipulator.checkSpace(c, itemId, quantity, "")) {
                    if (!ItemConstants.isRechargeable(itemId)) { //Pets can't be bought from shops
                        InventoryManipulator.addById(c, itemId, quantity, "", -1);
                        c.getPlayer().gainMeso(-amount, false);
                    } else {
                        short slotMax = ii.getSlotMax(c, item.itemId());
                        quantity = slotMax;
                        InventoryManipulator.addById(c, itemId, quantity, "", -1);
                        c.getPlayer().gainMeso(-item.price(), false);
                    }
                    c.sendPacket(PacketCreator.shopTransaction((byte) 0));
                } else {
                    c.sendPacket(PacketCreator.shopTransaction((byte) 3));
                }

            } else {
                c.sendPacket(PacketCreator.shopTransaction((byte) 2));
            }

        } else if (item.pitch() > 0) {
            int amount = (int) Math.min((float) item.pitch() * quantity, Integer.MAX_VALUE);

            if (c.getPlayer().getInventory(InventoryType.ETC).countById(ItemId.PERFECT_PITCH) >= amount) {
                if (InventoryManipulator.checkSpace(c, itemId, quantity, "")) {
                    if (!ItemConstants.isRechargeable(itemId)) {
                        InventoryManipulator.addById(c, itemId, quantity, "", -1);
                        InventoryManipulator.removeById(c, InventoryType.ETC, ItemId.PERFECT_PITCH, amount, false, false);
                    } else {
                        short slotMax = ii.getSlotMax(c, item.itemId());
                        quantity = slotMax;
                        InventoryManipulator.addById(c, itemId, quantity, "", -1);
                        InventoryManipulator.removeById(c, InventoryType.ETC, ItemId.PERFECT_PITCH, amount, false, false);
                    }
                    c.sendPacket(PacketCreator.shopTransaction((byte) 0));
                } else {
                    c.sendPacket(PacketCreator.shopTransaction((byte) 3));
                }
            }

        } else if (c.getPlayer().getInventory(InventoryType.CASH).countById(token) != 0) {
            int amount = c.getPlayer().getInventory(InventoryType.CASH).countById(token);
            int value = amount * tokenvalue;
            int cost = item.price() * quantity;
            if (c.getPlayer().getMeso() + value >= cost) {
                int cardreduce = value - cost;
                int diff = cardreduce + c.getPlayer().getMeso();
                if (InventoryManipulator.checkSpace(c, itemId, quantity, "")) {
                    if (ItemConstants.isPet(itemId)) {
                        int petid = Pet.createPet(itemId);
                        InventoryManipulator.addById(c, itemId, quantity, "", petid, -1);
                    } else {
                        InventoryManipulator.addById(c, itemId, quantity, "", -1, -1);
                    }
                    c.getPlayer().gainMeso(diff, false);
                } else {
                    c.sendPacket(PacketCreator.shopTransaction((byte) 3));
                }
                c.sendPacket(PacketCreator.shopTransaction((byte) 0));
            } else {
                c.sendPacket(PacketCreator.shopTransaction((byte) 2));
            }
        }
    }

    private static boolean canSell(Item item, short quantity) {
        if (item == null) { //Basic check
            return false;
        }

        short iQuant = item.getQuantity();
        if (iQuant == 0xFFFF) {
            iQuant = 1;
        } else if (iQuant < 0) {
            return false;
        }

        if (!ItemConstants.isRechargeable(item.getItemId())) {
            return iQuant != 0 && quantity <= iQuant;
        }

        return true;
    }

    private static short getSellingQuantity(Item item, short quantity) {
        if (ItemConstants.isRechargeable(item.getItemId())) {
            quantity = item.getQuantity();
            if (quantity == 0xFFFF) {
                quantity = 1;
            }
        }

        return quantity;
    }

    public void sell(Client c, InventoryType type, short slot, short quantity) {
        if (quantity == 0xFFFF || quantity == 0) {
            quantity = 1;
        } else if (quantity < 0) {
            return;
        }

        Item item = c.getPlayer().getInventory(type).getItem(slot);
        if (canSell(item, quantity)) {
            quantity = getSellingQuantity(item, quantity);
            InventoryManipulator.removeFromSlot(c, type, (byte) slot, quantity, false);

            ItemInformationProvider ii = ItemInformationProvider.getInstance();
            int recvMesos = ii.getPrice(item.getItemId(), quantity);
            if (recvMesos > 0) {
                c.getPlayer().gainMeso(recvMesos, false);
            }
            c.sendPacket(PacketCreator.shopTransaction((byte) 0x8));
        } else {
            c.sendPacket(PacketCreator.shopTransaction((byte) 0x5));
        }
    }

    public void recharge(Client c, short slot) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        Item item = c.getPlayer().getInventory(InventoryType.USE).getItem(slot);
        if (item == null || !ItemConstants.isRechargeable(item.getItemId())) {
            return;
        }
        short slotMax = ii.getSlotMax(c, item.getItemId());
        if (item.getQuantity() < 0) {
            return;
        }
        if (item.getQuantity() < slotMax) {
            int price = (int) Math.ceil(ii.getUnitPrice(item.getItemId()) * (slotMax - item.getQuantity()));
            if (c.getPlayer().getMeso() >= price) {
                item.setQuantity(slotMax);
                c.getPlayer().forceUpdateItem(item);
                c.getPlayer().gainMeso(-price, false, true, false);
                c.sendPacket(PacketCreator.shopTransaction((byte) 0x8));
            } else {
                c.sendPacket(PacketCreator.shopTransaction((byte) 0x2));
            }
        }
    }

    private ShopItem findBySlot(short slot) {
        return items.get(slot);
    }

    public int getNpcId() {
        return npcId;
    }

    public int getId() {
        return id;
    }
}
