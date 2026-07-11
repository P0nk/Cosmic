/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    Copyleft (L) 2016 - 2019 RonanLana (HeavenMS)

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
import client.inventory.Item;
import client.inventory.manipulator.InventoryManipulator;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.CashShop;
import server.ItemInformationProvider;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author Penguins (Acrylic)
 * @author Ronan (HeavenMS)
 */
public final class CouponCodeHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(CouponCodeHandler.class);

    private static List<Pair<Integer, Pair<Integer, Integer>>> getNXCodeItems(Character chr, Connection con, int codeid) throws SQLException {
        Map<Integer, Integer> couponItems = new HashMap<>();
        Map<Integer, Integer> couponPoints = new HashMap<>(5);

        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM nxcode_items WHERE codeid = ?")) {
            ps.setInt(1, codeid);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int type = rs.getInt("type"), quantity = rs.getInt("quantity");
                    if (type < 5) {
                        Integer i = couponPoints.get(type);
                        if (i != null) {
                            couponPoints.put(type, i + quantity);
                        } else {
                            couponPoints.put(type, quantity);
                        }
                    } else {
                        int item = rs.getInt("item");

                        Integer i = couponItems.get(item);
                        if (i != null) {
                            couponItems.put(item, i + quantity);
                        } else {
                            couponItems.put(item, quantity);
                        }
                    }
                }
            }
        }

        List<Pair<Integer, Pair<Integer, Integer>>> ret = new LinkedList<>();
        if (!couponItems.isEmpty()) {
            for (Entry<Integer, Integer> e : couponItems.entrySet()) {
                int item = e.getKey(), qty = e.getValue();

                if (ItemInformationProvider.getInstance().getName(item) == null) {
                    item = 4000000;
                    qty = 1;

                    log.warn("Error trying to redeem itemid {} from coupon codeid {}", item, codeid);
                }

                if (!chr.canHold(item, qty)) {
                    return null;
                }

                ret.add(new Pair<>(5, new Pair<>(item, qty)));
            }
        }

        if (!couponPoints.isEmpty()) {
            for (Entry<Integer, Integer> e : couponPoints.entrySet()) {
                ret.add(new Pair<>(e.getKey(), new Pair<>(777, e.getValue())));
            }
        }

        return ret;
    }

    private static Pair<Integer, List<Pair<Integer, Pair<Integer, Integer>>>> getNXCodeResult(Character chr, String code) {
        Client c = chr.getClient();
        List<Pair<Integer, Pair<Integer, Integer>>> ret = new LinkedList<>();
        try {
            if (!c.attemptCsCoupon()) {
                return new Pair<>(-5, null);
            }

            try (Connection con = DatabaseConnection.getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM nxcode WHERE code = ?")) {
                    ps.setString(1, code);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            return new Pair<>(-1, null);
                        }

                        if (rs.getString("retriever") != null) {
                            return new Pair<>(-2, null);
                        }

                        if (rs.getLong("expiration") < Server.getInstance().getCurrentTime()) {
                            return new Pair<>(-3, null);
                        }

                        final int codeid = rs.getInt("id");

                        ret = getNXCodeItems(chr, con, codeid);
                        if (ret == null) {
                            return new Pair<>(-4, null);
                        }
                    }
                }

                try (PreparedStatement ps = con.prepareStatement("UPDATE nxcode SET retriever = ? WHERE code = ?")) {
                    ps.setString(1, chr.getName());
                    ps.setString(2, code);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        c.resetCsCoupon();
        return new Pair<>(0, ret);
    }

    private static int parseCouponResult(int res) {
        switch (res) {
            case -1:
                return 0xB0;

            case -2:
                return 0xB3;

            case -3:
                return 0xB2;

            case -4:
                return 0xBB;

            default:
                return 0xB1;
        }
    }

    @Override
    public final void handlePacket(InPacket p, Client c) {
        p.skip(2);
        String code = p.readString();

        if (c.tryacquireClient()) {
            try {
                Pair<Integer, List<Pair<Integer, Pair<Integer, Integer>>>> codeRes = getNXCodeResult(c.getPlayer(), code.toUpperCase());
                int type = codeRes.getLeft();
                if (type < 0) {
                    c.sendPacket(PacketCreator.showCashShopMessage((byte) parseCouponResult(type)));
                } else {
                    List<Item> cashItems = new LinkedList<>();
                    List<Pair<Integer, Integer>> items = new LinkedList<>();
                    int nxCredit = 0;
                    int maplePoints = 0;
                    int nxPrepaid = 0;
                    int mesos = 0;

                    for (Pair<Integer, Pair<Integer, Integer>> pair : codeRes.getRight()) {
                        type = pair.getLeft();
                        int quantity = pair.getRight().getRight();

                        CashShop cs = c.getPlayer().getCashShop();
                        switch (type) {
                            case 0:
                                c.getPlayer().gainMeso(quantity, false); //mesos
                                mesos += quantity;
                                break;
                            case 4:
                                cs.gainCash(1, quantity);    //nxCredit
                                nxCredit += quantity;
                                break;
                            case 1:
                                cs.gainCash(2, quantity);    //maplePoint
                                maplePoints += quantity;
                                break;
                            case 2:
                                cs.gainCash(4, quantity);    //nxPrepaid
                                nxPrepaid += quantity;
                                break;
                            case 3:
                                cs.gainCash(1, quantity);
                                nxCredit += quantity;
                                cs.gainCash(4, (quantity / 5000));
                                nxPrepaid += quantity / 5000;
                                break;

                            default:
                                int item = pair.getRight().getLeft();

                                short qty;
                                if (quantity > Short.MAX_VALUE) {
                                    qty = Short.MAX_VALUE;
                                } else if (quantity < Short.MIN_VALUE) {
                                    qty = Short.MIN_VALUE;
                                } else {
                                    qty = (short) quantity;
                                }

                                if (ItemInformationProvider.getInstance().isCash(item)) {
                                    Item it = CashShop.generateCouponItem(item, qty);

                                    cs.addToInventory(it);
                                    cashItems.add(it);
                                } else {
                                    InventoryManipulator.addById(c, item, qty, "", -1);
                                    items.add(new Pair<>((int) qty, item));
                                }
                                break;
                        }
                    }
                    if (cashItems.size() > 255) {
                        List<Item> oldList = cashItems;
                        cashItems = Arrays.asList(new Item[255]);
                        int index = 0;
                        for (Item item : oldList) {
                            cashItems.set(index, item);
                            index++;
                        }
                    }
                    if (nxCredit != 0 || nxPrepaid != 0) { //coupon packet can only show maple points (afaik)
                        c.sendPacket(PacketCreator.showBoughtQuestItem(0));
                    } else {
                        c.sendPacket(PacketCreator.showCouponRedeemedItems(c.getAccID(), maplePoints, mesos, cashItems, items));
                    }
                    c.enableCSActions();
                }
            } finally {
                c.releaseClient();
            }
        }
    }
}
