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
import client.Ring;
import client.inventory.Equip;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.manipulator.InventoryManipulator;
import client.processor.npc.DueyProcessor;
import constants.id.ItemId;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.server.channel.Channel;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.events.EventInstanceManager;
import server.ItemInformationProvider;
import service.NoteService;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.Pair;
import tools.packets.WeddingPackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Jvlaple
 * @author Ronan - major overhaul on Ring handling mechanics
 * @author Drago (Dragohe4rt) - on Wishlist
 */
public final class RingActionHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(RingActionHandler.class);

    private final NoteService noteService;

    public RingActionHandler(NoteService noteService) {
        this.noteService = noteService;
    }

    private static int getEngagementBoxId(int useItemId) {
        return switch (useItemId) {
            case ItemId.ENGAGEMENT_BOX_MOONSTONE -> ItemId.EMPTY_ENGAGEMENT_BOX_MOONSTONE;
            case ItemId.ENGAGEMENT_BOX_STAR -> ItemId.EMPTY_ENGAGEMENT_BOX_STAR;
            case ItemId.ENGAGEMENT_BOX_GOLDEN -> ItemId.EMPTY_ENGAGEMENT_BOX_GOLDEN;
            case ItemId.ENGAGEMENT_BOX_SILVER -> ItemId.EMPTY_ENGAGEMENT_BOX_SILVER;
            default -> ItemId.CARAT_RING_BASE + (useItemId - ItemId.CARAT_RING_BOX_BASE);
        };
    }

    public static void sendEngageProposal(final Client c, final String name, final int itemid) {
        final int newBoxId = getEngagementBoxId(itemid);
        final Character target = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
        final Character source = c.getPlayer();

        // TODO: get the correct packet bytes for these popups
        if (source.isMarried()) {
            source.dropMessage(1, "You're already married!");
            source.sendPacket(WeddingPackets.OnMarriageResult((byte) 0));
            return;
        } else if (source.getPartnerId() > 0) {
            source.dropMessage(1, "You're already engaged!");
            source.sendPacket(WeddingPackets.OnMarriageResult((byte) 0));
            return;
        } else if (source.getMarriageItemId() > 0) {
            source.dropMessage(1, "You're already engaging someone!");
            source.sendPacket(WeddingPackets.OnMarriageResult((byte) 0));
            return;
        } else if (target == null) {
            source.dropMessage(1, "Unable to find " + name + " on this channel.");
            source.sendPacket(WeddingPackets.OnMarriageResult((byte) 0));
            return;
        } else if (target == source) {
            source.dropMessage(1, "You can't engage yourself.");
            source.sendPacket(WeddingPackets.OnMarriageResult((byte) 0));
            return;
        } else if (target.getLevel() < 50) {
            source.dropMessage(1, "You can only propose to someone level 50 or higher.");
            source.sendPacket(WeddingPackets.OnMarriageResult((byte) 0));
            return;
        } else if (source.getLevel() < 50) {
            source.dropMessage(1, "You can only propose being level 50 or higher.");
            source.sendPacket(WeddingPackets.OnMarriageResult((byte) 0));
            return;
        } else if (!target.getMap().equals(source.getMap())) {
            source.dropMessage(1, "Make sure your partner is on the same map!");
            source.sendPacket(WeddingPackets.OnMarriageResult((byte) 0));
            return;
        } else if (!source.haveItem(itemid) || itemid < ItemId.ENGAGEMENT_BOX_MIN || itemid > ItemId.ENGAGEMENT_BOX_MAX) {
            source.sendPacket(WeddingPackets.OnMarriageResult((byte) 0));
            return;
        } else if (target.isMarried()) {
            source.dropMessage(1, "The player is already married!");
            source.sendPacket(WeddingPackets.OnMarriageResult((byte) 0));
            return;
        } else if (target.getPartnerId() > 0 || target.getMarriageItemId() > 0) {
            source.dropMessage(1, "The player is already engaged!");
            source.sendPacket(WeddingPackets.OnMarriageResult((byte) 0));
            return;
        } else if (target.haveWeddingRing()) {
            source.dropMessage(1, "The player already holds a marriage ring...");
            source.sendPacket(WeddingPackets.OnMarriageResult((byte) 0));
            return;
        } else if (source.haveWeddingRing()) {
            source.dropMessage(1, "You can't propose while holding a marriage ring!");
            source.sendPacket(WeddingPackets.OnMarriageResult((byte) 0));
            return;
        } else if (target.getGender() == source.getGender()) {
            source.dropMessage(1, "You may only propose to a " + (source.getGender() == 1 ? "male" : "female") + "!");
            source.sendPacket(WeddingPackets.OnMarriageResult((byte) 0));
            return;
        } else if (!InventoryManipulator.checkSpace(c, newBoxId, 1, "")) {
            source.dropMessage(5, "You don't have a ETC slot available right now!");
            source.sendPacket(WeddingPackets.OnMarriageResult((byte) 0));
            return;
        } else if (!InventoryManipulator.checkSpace(target.getClient(), newBoxId + 1, 1, "")) {
            source.dropMessage(5, "The girl you proposed doesn't have a ETC slot available right now.");
            source.sendPacket(WeddingPackets.OnMarriageResult((byte) 0));
            return;
        }

        source.setMarriageItemId(itemid);
        target.sendPacket(WeddingPackets.onMarriageRequest(source.getName(), source.getId()));
    }

    private static void eraseEngagementOffline(int characterId) {
        try (Connection con = DatabaseConnection.getConnection()) {
            eraseEngagementOffline(characterId, con);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
    }

    private static void eraseEngagementOffline(int characterId, Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET marriageItemId=-1, partnerId=-1 WHERE id=?")) {
            ps.setInt(1, characterId);
            ps.executeUpdate();
        }
    }

    private static void breakEngagementOffline(int characterId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT marriageItemId FROM characters WHERE id=?")) {
            ps.setInt(1, characterId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int marriageItemId = rs.getInt("marriageItemId");

                    if (marriageItemId > 0) {
                        try (PreparedStatement ps2 = con.prepareStatement("UPDATE inventoryitems SET expiration=0 WHERE itemid=? AND characterid=?")) {
                            ps2.setInt(1, marriageItemId);
                            ps2.setInt(2, characterId);

                            ps2.executeUpdate();
                        }
                    }
                }
            }

            eraseEngagementOffline(characterId, con);
        } catch (SQLException ex) {
            log.error("Error updating offline breakup", ex);
        }
    }

    private synchronized static void breakMarriage(Character chr) {
        int partnerid = chr.getPartnerId();
        if (partnerid <= 0) {
            return;
        }

        chr.getClient().getWorldServer().deleteRelationship(chr.getId(), partnerid);
        Ring.removeRing(chr.getMarriageRing());

        Character partner = chr.getClient().getWorldServer().getPlayerStorage().getCharacterById(partnerid);
        if (partner == null) {
            eraseEngagementOffline(partnerid);
        } else {
            partner.dropMessage(5, chr.getName() + " has decided to break up the marriage.");

            //partner.sendPacket(Wedding.OnMarriageResult((byte) 0)); ok, how to gracefully unengage someone without the need to cc?
            partner.sendPacket(WeddingPackets.OnNotifyWeddingPartnerTransfer(0, 0));
            resetRingId(partner);
            partner.setPartnerId(-1);
            partner.setMarriageItemId(-1);
            partner.addMarriageRing(null);
        }

        chr.dropMessage(5, "You have successfully break the marriage with " + Character.getNameById(partnerid) + ".");

        //chr.sendPacket(Wedding.OnMarriageResult((byte) 0));
        chr.sendPacket(WeddingPackets.OnNotifyWeddingPartnerTransfer(0, 0));
        resetRingId(chr);
        chr.setPartnerId(-1);
        chr.setMarriageItemId(-1);
        chr.addMarriageRing(null);
    }

    private static void resetRingId(Character player) {
        int ringitemid = player.getMarriageRing().getItemId();

        Item it = player.getInventory(InventoryType.EQUIP).findById(ringitemid);
        if (it == null) {
            it = player.getInventory(InventoryType.EQUIPPED).findById(ringitemid);
        }

        if (it != null) {
            Equip eqp = (Equip) it;
            eqp.setRingId(-1);
        }
    }

    private synchronized static void breakEngagement(Character chr) {
        int partnerid = chr.getPartnerId();
        int marriageitemid = chr.getMarriageItemId();

        chr.getClient().getWorldServer().deleteRelationship(chr.getId(), partnerid);

        Character partner = chr.getClient().getWorldServer().getPlayerStorage().getCharacterById(partnerid);
        if (partner == null) {
            breakEngagementOffline(partnerid);
        } else {
            partner.dropMessage(5, chr.getName() + " has decided to break up the engagement.");

            int partnerMarriageitemid = marriageitemid + ((chr.getGender() == 0) ? 1 : -1);
            if (partner.haveItem(partnerMarriageitemid)) {
                InventoryManipulator.removeById(partner.getClient(), InventoryType.ETC, partnerMarriageitemid, (short) 1, false, false);
            }

            //partner.sendPacket(Wedding.OnMarriageResult((byte) 0)); ok, how to gracefully unengage someone without the need to cc?
            partner.sendPacket(WeddingPackets.OnNotifyWeddingPartnerTransfer(0, 0));
            partner.setPartnerId(-1);
            partner.setMarriageItemId(-1);
        }

        if (chr.haveItem(marriageitemid)) {
            InventoryManipulator.removeById(chr.getClient(), InventoryType.ETC, marriageitemid, (short) 1, false, false);
        }
        chr.dropMessage(5, "You have successfully break the engagement with " + Character.getNameById(partnerid) + ".");

        //chr.sendPacket(Wedding.OnMarriageResult((byte) 0));
        chr.sendPacket(WeddingPackets.OnNotifyWeddingPartnerTransfer(0, 0));
        chr.setPartnerId(-1);
        chr.setMarriageItemId(-1);
    }

    public static void breakMarriageRing(Character chr, final int wItemId) {
        final InventoryType type = InventoryType.getByType((byte) (wItemId / 1000000));
        final Item wItem = chr.getInventory(type).findById(wItemId);
        final boolean weddingToken = (wItem != null && type == InventoryType.ETC && wItemId / 10000 == 403);
        final boolean weddingRing = (wItem != null && wItemId / 10 == 111280);

        if (weddingRing) {
            if (chr.getPartnerId() > 0) {
                breakMarriage(chr);
            }

            chr.getMap().disappearingItemDrop(chr, chr, wItem, chr.getPosition());
        } else if (weddingToken) {
            if (chr.getPartnerId() > 0) {
                breakEngagement(chr);
            }

            chr.getMap().disappearingItemDrop(chr, chr, wItem, chr.getPosition());
        }
    }

    public static void giveMarriageRings(Character player, Character partner, int marriageRingId) {
        Pair<Integer, Integer> rings = Ring.createRing(marriageRingId, player, partner);
        ItemInformationProvider ii = ItemInformationProvider.getInstance();

        Item ringObj = ii.getEquipById(marriageRingId);
        Equip ringEqp = (Equip) ringObj;
        ringEqp.setRingId(rings.getLeft());
        player.addMarriageRing(Ring.loadFromDb(rings.getLeft()));
        InventoryManipulator.addFromDrop(player.getClient(), ringEqp, false, -1);
        player.broadcastMarriageMessage();

        ringObj = ii.getEquipById(marriageRingId);
        ringEqp = (Equip) ringObj;
        ringEqp.setRingId(rings.getRight());
        partner.addMarriageRing(Ring.loadFromDb(rings.getRight()));
        InventoryManipulator.addFromDrop(partner.getClient(), ringEqp, false, -1);
        partner.broadcastMarriageMessage();
    }

    @Override
    public final void handlePacket(InPacket p, Client c) {
        byte mode = p.readByte();
        String name;
        byte slot;
        switch (mode) {
            case 0: // Send Proposal
                sendEngageProposal(c, p.readString(), p.readInt());
                break;

            case 1: // Cancel Proposal
                if (c.getPlayer().getMarriageItemId() / 1000000 != 4) {
                    c.getPlayer().setMarriageItemId(-1);
                }
                break;

            case 2: // Accept/Deny Proposal
                final boolean accepted = p.readByte() > 0;
                name = p.readString();
                final int id = p.readInt();

                final Character source = c.getWorldServer().getPlayerStorage().getCharacterByName(name);
                final Character target = c.getPlayer();

                if (source == null) {
                    target.sendPacket(PacketCreator.enableActions());
                    return;
                }

                final int itemid = source.getMarriageItemId();
                if (target.getPartnerId() > 0 || source.getId() != id || itemid <= 0 || !source.haveItem(itemid) || source.getPartnerId() > 0 || !source.isAlive() || !target.isAlive()) {
                    target.sendPacket(PacketCreator.enableActions());
                    return;
                }

                if (accepted) {
                    final int newItemId = getEngagementBoxId(itemid);
                    if (!InventoryManipulator.checkSpace(c, newItemId, 1, "") || !InventoryManipulator.checkSpace(source.getClient(), newItemId, 1, "")) {
                        target.sendPacket(PacketCreator.enableActions());
                        return;
                    }

                    try {
                        InventoryManipulator.removeById(source.getClient(), InventoryType.USE, itemid, 1, false, false);

                        int marriageId = c.getWorldServer().createRelationship(source.getId(), target.getId());
                        source.setPartnerId(target.getId()); // engage them (new marriageitemid, partnerid for both)
                        target.setPartnerId(source.getId());

                        source.setMarriageItemId(newItemId);
                        target.setMarriageItemId(newItemId + 1);

                        InventoryManipulator.addById(source.getClient(), newItemId, (short) 1);
                        InventoryManipulator.addById(c, (newItemId + 1), (short) 1);

                        source.sendPacket(WeddingPackets.OnMarriageResult(marriageId, source, false));
                        target.sendPacket(WeddingPackets.OnMarriageResult(marriageId, source, false));

                        source.sendPacket(WeddingPackets.OnNotifyWeddingPartnerTransfer(target.getId(), target.getMapId()));
                        target.sendPacket(WeddingPackets.OnNotifyWeddingPartnerTransfer(source.getId(), source.getMapId()));
                    } catch (Exception e) {
                        log.error("Error with engagement", e);
                    }
                } else {
                    source.dropMessage(1, "She has politely declined your engagement request.");
                    source.sendPacket(WeddingPackets.OnMarriageResult((byte) 0));

                    source.setMarriageItemId(-1);
                }
                break;

            case 3: // Break Engagement
                breakMarriageRing(c.getPlayer(), p.readInt());
                break;

            case 5: // Invite %s to Wedding
                name = p.readString();
                int marriageId = p.readInt();
                slot = p.readByte(); // this is an int

                int itemId;
                try {
                    itemId = c.getPlayer().getInventory(InventoryType.ETC).getItem(slot).getItemId();
                } catch (NullPointerException npe) {
                    c.sendPacket(PacketCreator.enableActions());
                    return;
                }

                if ((itemId != ItemId.INVITATION_CHAPEL && itemId != ItemId.INVITATION_CATHEDRAL) || !c.getPlayer().haveItem(itemId)) {
                    c.sendPacket(PacketCreator.enableActions());
                    return;
                }

                String groom = c.getPlayer().getName();
                String bride = Character.getNameById(c.getPlayer().getPartnerId());
                int guest = Character.getIdByName(name);
                if (groom == null || bride == null || groom.equals("") || bride.equals("") || guest <= 0) {
                    c.getPlayer().dropMessage(5, "Unable to find " + name + "!");
                    return;
                }

                try {
                    World wserv = c.getWorldServer();
                    Pair<Boolean, Boolean> registration = wserv.getMarriageQueuedLocation(marriageId);

                    if (registration != null) {
                        if (wserv.addMarriageGuest(marriageId, guest)) {
                            boolean cathedral = registration.getLeft();
                            int newItemId = cathedral ? ItemId.RECEIVED_INVITATION_CATHEDRAL : ItemId.RECEIVED_INVITATION_CHAPEL;

                            Channel cserv = c.getChannelServer();
                            int resStatus = cserv.getWeddingReservationStatus(marriageId, cathedral);
                            if (resStatus > 0) {
                                long expiration = cserv.getWeddingTicketExpireTime(resStatus + 1);

                                String baseMessage = "You've been invited to %s and %s's Wedding!".formatted(groom, bride);
                                Character guestChr = c.getWorldServer().getPlayerStorage().getCharacterById(guest);
                                if (guestChr != null && InventoryManipulator.checkSpace(guestChr.getClient(), newItemId, 1, "") && InventoryManipulator.addById(guestChr.getClient(), newItemId, (short) 1, expiration)) {
                                    guestChr.dropMessage(6, "[Wedding] %s".formatted(baseMessage));
                                } else {
                                    String dueyMessage = baseMessage + " Receive your invitation from Duey!";
                                    if (guestChr != null && guestChr.isLoggedinWorld()) {
                                        guestChr.dropMessage(6, "[Wedding] %s".formatted(dueyMessage));
                                    } else {
                                        noteService.sendNormal(dueyMessage, groom, name);
                                    }

                                    Item weddingTicket = new Item(newItemId, (short) 0, (short) 1);
                                    weddingTicket.setExpiration(expiration);

                                    DueyProcessor.dueyCreatePackage(weddingTicket, 0, groom, guest);
                                }
                            } else {
                                c.getPlayer().dropMessage(5, "Wedding is already under way. You cannot invite any more guests for the event.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "'" + name + "' is already invited for your marriage.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "Invitation was not sent to '" + name + "'. Either the time for your marriage reservation already came or it was not found.");
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    return;
                }

                c.getAbstractPlayerInteraction().gainItem(itemId, (short) -1);
                break;

            case 6: // Open Wedding Invitation
                slot = (byte) p.readInt();
                int invitationid = p.readInt();

                if (invitationid == ItemId.RECEIVED_INVITATION_CHAPEL || invitationid == ItemId.RECEIVED_INVITATION_CATHEDRAL) {
                    Item item = c.getPlayer().getInventory(InventoryType.ETC).getItem(slot);
                    if (item == null || item.getItemId() != invitationid) {
                        c.sendPacket(PacketCreator.enableActions());
                        return;
                    }

                    // collision case: most soon-to-come wedding will show up
                    Pair<Integer, Integer> coupleId = c.getWorldServer().getWeddingCoupleForGuest(c.getPlayer().getId(), invitationid == ItemId.RECEIVED_INVITATION_CATHEDRAL);
                    if (coupleId != null) {
                        int groomId = coupleId.getLeft(), brideId = coupleId.getRight();
                        c.sendPacket(WeddingPackets.sendWeddingInvitation(Character.getNameById(groomId), Character.getNameById(brideId)));
                    }
                }

                break;

            case 9:
                try {
                    // By -- Dragoso (Drago)
                    // Groom and Bride's Wishlist

                    Character player = c.getPlayer();

                    EventInstanceManager eim = player.getEventInstance();
                    if (eim != null) {
                        boolean isMarrying = (player.getId() == eim.getIntProperty("groomId") || player.getId() == eim.getIntProperty("brideId"));

                        if (isMarrying) {
                            int amount = p.readShort();
                            if (amount > 10) {
                                amount = 10;
                            }

                            String wishlistItems = "";
                            for (int i = 0; i < amount; i++) {
                                String s = p.readString();
                                wishlistItems += (s + "\r\n");
                            }

                            String wlKey;
                            if (player.getId() == eim.getIntProperty("groomId")) {
                                wlKey = "groomWishlist";
                            } else {
                                wlKey = "brideWishlist";
                            }

                            if (eim.getProperty(wlKey).contentEquals("")) {
                                eim.setProperty(wlKey, wishlistItems);
                            }
                        }
                    }
                } catch (NumberFormatException nfe) {
                }

                break;

            default:
                log.warn("Unhandled RING_ACTION mode. Packet: {}", p);
                break;
        }

        c.sendPacket(PacketCreator.enableActions());
    }
}
