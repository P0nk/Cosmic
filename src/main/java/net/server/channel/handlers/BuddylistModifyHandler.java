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

import client.BuddyList;
import client.BuddyList.BuddyAddResult;
import client.BuddyList.BuddyOperation;
import client.BuddylistEntry;
import client.Character;
import client.Client;
import model.CharacterIdentity;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.server.world.World;
import tools.DatabaseConnection;
import tools.PacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static client.BuddyList.BuddyOperation.ADDED;

public class BuddylistModifyHandler extends AbstractPacketHandler {

    private record BuddyIdentity(String name, int id, int buddyCapacity) {
        public BuddyIdentity {
            if (name == null) {
                throw new IllegalArgumentException("name must not be null");
            }
        }
    }

    private void nextPendingRequest(Client c) {
        CharacterIdentity pendingBuddyRequest = c.getPlayer().getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            c.sendPacket(PacketCreator.requestBuddylistAdd(pendingBuddyRequest.id(), c.getPlayer().getId(), pendingBuddyRequest.name()));
        }
    }

    private BuddyIdentity getBuddyIdentityFromDatabase(String name) throws SQLException {
        BuddyIdentity buddyIdentity = null;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id, name, buddyCapacity FROM characters WHERE name LIKE ?")) {
            ps.setString(1, name);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String actualName = rs.getString("name");
                    int id = rs.getInt("id");
                    int buddyCapacity = rs.getInt("buddyCapacity");
                    buddyIdentity = new BuddyIdentity(actualName, id, buddyCapacity);
                }
            }
        }

        return buddyIdentity;
    }

    @Override
    public void handlePacket(InPacket p, Client c) {
        int mode = p.readByte();
        Character player = c.getPlayer();
        BuddyList buddylist = player.getBuddylist();
        if (mode == 1) { // add
            String addName = p.readString();
            String group = p.readString();
            if (group.length() > 16 || addName.length() < 4 || addName.length() > 13) {
                return; //hax.
            }
            BuddylistEntry ble = buddylist.get(addName);
            if (ble != null && !ble.isVisible() && group.equals(ble.getGroup())) {
                c.sendPacket(PacketCreator.serverNotice(1, "You already have \"" + ble.getName() + "\" on your Buddylist"));
            } else if (buddylist.isFull() && ble == null) {
                c.sendPacket(PacketCreator.serverNotice(1, "Your buddylist is already full"));
            } else if (ble == null) {
                try {
                    World world = c.getWorldServer();
                    final BuddyIdentity buddyIdentity;
                    int channel;
                    Character otherChar = c.getChannelServer().getPlayerStorage().getCharacterByName(addName);
                    if (otherChar != null) {
                        channel = c.getChannel();
                        buddyIdentity = new BuddyIdentity(otherChar.getName(), otherChar.getId(), otherChar.getBuddylist().getCapacity());
                    } else {
                        channel = world.find(addName);
                        buddyIdentity = getBuddyIdentityFromDatabase(addName);
                    }
                    if (buddyIdentity == null) {
                        c.sendPacket(PacketCreator.serverNotice(1, "A character called \"" + addName + "\" does not exist"));
                        return;
                    }
                    BuddyAddResult buddyAddResult = null;
                    if (channel != -1) {
                        buddyAddResult = world.requestBuddyAdd(addName, c.getChannel(), player.getId(), player.getName());
                    } else {
                        try (Connection con = DatabaseConnection.getConnection()) {
                            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) as buddyCount FROM buddies WHERE characterid = ? AND pending = 0")) {
                                ps.setInt(1, buddyIdentity.id());

                                try (ResultSet rs = ps.executeQuery()) {
                                    if (!rs.next()) {
                                        throw new RuntimeException("Result set expected");
                                    } else if (rs.getInt("buddyCount") >= buddyIdentity.buddyCapacity()) {
                                        buddyAddResult = BuddyAddResult.BUDDYLIST_FULL;
                                    }
                                }
                            }

                            try (PreparedStatement ps = con.prepareStatement("SELECT pending FROM buddies WHERE characterid = ? AND buddyid = ?")) {
                                ps.setInt(1, buddyIdentity.id());
                                ps.setInt(2, player.getId());

                                try (ResultSet rs = ps.executeQuery()) {
                                    if (rs.next()) {
                                        buddyAddResult = BuddyAddResult.ALREADY_ON_LIST;
                                    }
                                }
                            }
                        }
                    }
                    if (buddyAddResult == BuddyAddResult.BUDDYLIST_FULL) {
                        c.sendPacket(PacketCreator.serverNotice(1, "\"" + addName + "\"'s Buddylist is full"));
                    } else {
                        int displayChannel;
                        displayChannel = -1;
                        int otherCid = buddyIdentity.id();
                        if (buddyAddResult == BuddyAddResult.ALREADY_ON_LIST && channel != -1) {
                            displayChannel = channel;
                            notifyRemoteChannel(c, channel, otherCid, ADDED);
                        } else if (buddyAddResult != BuddyAddResult.ALREADY_ON_LIST && channel == -1) {
                            try (Connection con = DatabaseConnection.getConnection();
                                 PreparedStatement ps = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `pending`) VALUES (?, ?, 1)")) {
                                ps.setInt(1, buddyIdentity.id());
                                ps.setInt(2, player.getId());
                                ps.executeUpdate();
                            }
                        }
                        buddylist.put(new BuddylistEntry(buddyIdentity.name(), group, otherCid, displayChannel, true));
                        c.sendPacket(PacketCreator.updateBuddylist(buddylist.getBuddies()));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                ble.changeGroup(group);
                c.sendPacket(PacketCreator.updateBuddylist(buddylist.getBuddies()));
            }
        } else if (mode == 2) { // accept buddy
            int otherCid = p.readInt();
            if (!buddylist.isFull()) {
                try {
                    int channel = c.getWorldServer().find(otherCid);//worldInterface.find(otherCid);
                    String otherName = null;
                    Character otherChar = c.getChannelServer().getPlayerStorage().getCharacterById(otherCid);
                    if (otherChar == null) {
                        try (Connection con = DatabaseConnection.getConnection();
                             PreparedStatement ps = con.prepareStatement("SELECT name FROM characters WHERE id = ?")) {
                            ps.setInt(1, otherCid);

                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    otherName = rs.getString("name");
                                }
                            }
                        }
                    } else {
                        otherName = otherChar.getName();
                    }
                    if (otherName != null) {
                        buddylist.put(new BuddylistEntry(otherName, "Default Group", otherCid, channel, true));
                        c.sendPacket(PacketCreator.updateBuddylist(buddylist.getBuddies()));
                        notifyRemoteChannel(c, channel, otherCid, ADDED);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            nextPendingRequest(c);
        } else if (mode == 3) { // delete
            int otherCid = p.readInt();
            player.deleteBuddy(otherCid);
        }
    }

    private void notifyRemoteChannel(Client c, int remoteChannel, int otherCid, BuddyOperation operation) {
        Character player = c.getPlayer();
        if (remoteChannel != -1) {
            c.getWorldServer().buddyChanged(otherCid, player.getId(), player.getName(), c.getChannel(), operation);
        }
    }
}
