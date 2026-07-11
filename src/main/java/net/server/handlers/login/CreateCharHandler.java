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
package net.server.handlers.login;

import client.Client;
import client.creator.novice.BeginnerCreator;
import client.creator.novice.LegendCreator;
import client.creator.novice.NoblesseCreator;
import constants.id.ItemId;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.PacketCreator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class CreateCharHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(CreateCharHandler.class);

    private final static Set<Integer> IDs = new HashSet<>(Arrays.asList(
            ItemId.SWORD, ItemId.HAND_AXE, ItemId.WOODEN_CLUB, ItemId.BASIC_POLEARM,// weapons
            ItemId.WHITE_UNDERSHIRT, ItemId.UNDERSHIRT, ItemId.GREY_TSHIRT, ItemId.WHITE_TUBETOP, ItemId.YELLOW_TSHIRT,
            ItemId.GREEN_TSHIRT, ItemId.RED_STRIPED_TOP, ItemId.SIMPLE_WARRIOR_TOP,// bottom
            ItemId.BLUE_JEAN_SHORTS, ItemId.BROWN_COTTON_SHORTS, ItemId.RED_MINISKIRT, ItemId.INDIGO_MINISKIRT,
            ItemId.SIMPLE_WARRIOR_PANTS, // top
            ItemId.RED_RUBBER_BOOTS, ItemId.LEATHER_SANDALS, ItemId.YELLOW_RUBBER_BOOTS, ItemId.BLUE_RUBBER_BOOTS,
            ItemId.AVERAGE_MUSASHI_SHOES,// shoes
            ItemId.BLACK_TOBEN, ItemId.ZETA, ItemId.BLACK_REBEL, ItemId.BLACK_BUZZ, ItemId.BLACK_SAMMY,
            ItemId.BLACK_EDGY, ItemId.BLACK_CONNIE,// hair
            ItemId.MOTIVATED_LOOK_M, ItemId.PERPLEXED_STARE, ItemId.LEISURE_LOOK_M, ItemId.MOTIVATED_LOOK_F,
            ItemId.FEARFUL_STARE_M, ItemId.LEISURE_LOOK_F, ItemId.FEARFUL_STARE_F, ItemId.PERPLEXED_STARE_HAZEL,
            ItemId.LEISURE_LOOK_HAZEL, ItemId.MOTIVATED_LOOK_AMETHYST, ItemId.MOTIVATED_LOOK_BLUE  //face
            //#NeverTrustStevenCode
    ));

    private static boolean isLegal(Integer toCompare) {
        return IDs.contains(toCompare);
    }


    @Override
    public final void handlePacket(InPacket p, Client c) {
        String name = p.readString();
        int job = p.readInt();
        int face = p.readInt();

        int hair = p.readInt();
        int haircolor = p.readInt();
        int skincolor = p.readInt();

        int top = p.readInt();
        int bottom = p.readInt();
        int shoes = p.readInt();
        int weapon = p.readInt();
        int gender = p.readByte();

        int[] items = new int[]{weapon, top, bottom, shoes, hair, face};
        for (int item : items) {
            if (!isLegal(item)) {
                log.warn("Owner from account {} tried to packet edit in chr creation", c.getAccountName());
                c.disconnect(true, false);
                return;
            }
        }

        int status;
        switch (job) {
        case 0: // Knights of Cygnus
            status = NoblesseCreator.createCharacter(c, name, face, hair + haircolor, skincolor, top, bottom, shoes, weapon, gender);
            break;
        case 1: // Adventurer
            status = BeginnerCreator.createCharacter(c, name, face, hair + haircolor, skincolor, top, bottom, shoes, weapon, gender);
            break;
        case 2: // Aran
            status = LegendCreator.createCharacter(c, name, face, hair + haircolor, skincolor, top, bottom, shoes, weapon, gender);
            break;
        default:
            c.sendPacket(PacketCreator.deleteCharResponse(0, 9));
            return;
        }

        if (status == -2) {
            c.sendPacket(PacketCreator.deleteCharResponse(0, 9));
        }
    }
}