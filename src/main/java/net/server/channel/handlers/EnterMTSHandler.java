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
import client.inventory.Equip;
import client.inventory.Item;
import client.processor.action.BuybackProcessor;
import config.YamlConfig;
import constants.id.MapId;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.server.Server;
import server.MTSItemInfo;
import server.maps.FieldLimit;
import server.maps.MiniDungeonInfo;
import tools.DatabaseConnection;
import tools.PacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Assuming you have the required imports
import client.Character;
import client.Client;
import config.YamlConfig;
import net.packet.InPacket;
import tools.PacketCreator;

public final class EnterMTSHandler extends AbstractPacketHandler {
    @Override
    public final void handlePacket(InPacket p, Client c) {
        Character mc = c.getPlayer();

        // Check if the player is dead
        if (!mc.isAlive()) {
            c.sendPacket(PacketCreator.serverNotice(5, "You cannot use this feature while you are dead."));
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        // Check if the player is in an event instance
        if (mc.cannotEnterCashShop()) {
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        if (mc.getEventInstance() != null) {
            c.sendPacket(PacketCreator.serverNotice(5, "FM button is disabled when registered for an event."));
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        // Check if the player is in a Mini-Dungeon map
        if (MiniDungeonInfo.isDungeonMap(mc.getMapId())) {
            c.sendPacket(PacketCreator.serverNotice(5, "FM button is disabled when inside a Mini-Dungeon."));
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        // Save the location and change the map to FM_ENTRANCE
        mc.saveLocation("FREE_MARKET");
        mc.changeMap(c.getChannelServer().getMapFactory().getMap(MapId.FM_ENTRANCE), 0);
        mc.saveCharToDB();
    }
}
