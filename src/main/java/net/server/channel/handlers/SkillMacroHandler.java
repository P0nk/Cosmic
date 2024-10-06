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
import client.SkillMacro;
import client.autoban.AutobanFactory;
import net.AbstractPacketHandler;
import net.netty.GameViolationException;
import net.packet.InPacket;

public final class SkillMacroHandler extends AbstractPacketHandler {

    @Override
    public final void handlePacket(InPacket p, Client c) {
        Character chr = c.getPlayer();
        int num = p.readByte();
        if (num > 5) {
            return;
        }

        for (int i = 0; i < num; i++) {
            String name = p.readString();
            if (name.length() > 12) {
                AutobanFactory.PACKET_EDIT.alert(chr, "Invalid name length " + name + " (" + name.length() + ") for skill macro.");
                throw GameViolationException.textLength(name);
            }

            int shout = p.readByte();
            int skill1 = p.readInt();
            int skill2 = p.readInt();
            int skill3 = p.readInt();
            SkillMacro macro = new SkillMacro(skill1, skill2, skill3, name, shout, i);
            chr.updateMacros(i, macro);
        }
    }
}
