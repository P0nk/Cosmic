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
package server.maps;

import io.netty.buffer.Unpooled;
import net.packet.*;

import java.util.Arrays;

public abstract class AbstractAnimatedMapObject extends AbstractMapObject implements AnimatedMapObject {
    public static final int IDLE_MOVEMENT_PACKET_LENGTH = 15;
    private static final Packet IDLE_MOVEMENT_PACKET = createIdleMovementPacket();

    private int stance;

    @Override
    public int getStance() {
        return stance;
    }

    @Override
    public void setStance(int stance) {
        this.stance = stance;
    }

    @Override
    public boolean isFacingLeft() {
        return Math.abs(stance) % 2 == 1;
    }

    public InPacket getIdleMovement() {
        final byte[] idleMovementBytes = IDLE_MOVEMENT_PACKET.getBytes();
        byte[] movementData = Arrays.copyOf(idleMovementBytes, idleMovementBytes.length);
        //seems wasteful to create a whole packet writer when only a few values are changed
        int x = getPosition().x;
        int y = getPosition().y;
        movementData[2] = (byte) (x & 0xFF); //x
        movementData[3] = (byte) (x >> 8 & 0xFF);
        movementData[4] = (byte) (y & 0xFF); //y
        movementData[5] = (byte) (y >> 8 & 0xFF);
        movementData[12] = (byte) (getStance() & 0xFF);
        return new ByteBufInPacket(Unpooled.wrappedBuffer(movementData));
    }

    private static Packet createIdleMovementPacket() {
        OutPacket p = new ByteBufOutPacket();
        p.writeByte(1); //movement command count
        p.writeByte(0);
        p.writeShort(-1); //x
        p.writeShort(-1); //y
        p.writeShort(0); //xwobble
        p.writeShort(0); //ywobble
        p.writeShort(0); //fh
        p.writeByte(-1); //stance
        p.writeShort(0); //duration
        return p;
    }
}
