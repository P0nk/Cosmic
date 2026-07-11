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

import net.AbstractPacketHandler;
import net.packet.InPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.maps.AnimatedMapObject;
import server.movement.*;
import tools.exceptions.EmptyMovementException;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMovementPacketHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(AbstractMovementPacketHandler.class);

    protected List<LifeMovementFragment> parseMovement(InPacket p) throws EmptyMovementException {
        List<LifeMovementFragment> res = new ArrayList<>();
        byte numCommands = p.readByte();
        if (numCommands < 1) {
            throw new EmptyMovementException(p);
        }
        for (byte i = 0; i < numCommands; i++) {
            byte command = p.readByte();
            switch (command) {
                case 0: // normal move
                case 5:
                case 17: { // Float
                    short xpos = p.readShort();
                    short ypos = p.readShort();
                    short xwobble = p.readShort();
                    short ywobble = p.readShort();
                    short fh = p.readShort();
                    byte newstate = p.readByte();
                    short duration = p.readShort();
                    AbsoluteLifeMovement alm = new AbsoluteLifeMovement(command, new Point(xpos, ypos), duration, newstate);
                    alm.setFh(fh);
                    alm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    res.add(alm);
                    break;
                }
                case 1: // jump
                case 2: // knockback
                case 6: // fj
                case 12:
                case 13: // Shot-jump-back thing
                case 16: // Float
                case 18:
                case 19: // Springs on maps
                case 20: // Aran Combat Step
                case 22: {
                    short xpos = p.readShort();
                    short ypos = p.readShort();
                    byte newstate = p.readByte();
                    short duration = p.readShort();
                    RelativeLifeMovement rlm = new RelativeLifeMovement(command, new Point(xpos, ypos), duration, newstate);
                    res.add(rlm);
                    break;
                }
                case 3: // teleport disappear
                case 4: // teleport appear
                case 7: // assaulter
                case 8: // assassinate
                case 9: // rush
                case 11: //chair
                {
//                case 14: {
                    short xpos = p.readShort();
                    short ypos = p.readShort();
                    short xwobble = p.readShort();
                    short ywobble = p.readShort();
                    byte newstate = p.readByte();
                    TeleportMovement tm = new TeleportMovement(command, new Point(xpos, ypos), newstate);
                    tm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    res.add(tm);
                    break;
                }
                case 14:
                    p.skip(9); // jump down (?)
                    break;
                case 10: // Change Equip
                    res.add(new ChangeEquip(p.readByte()));
                    break;
                /*case 11: { // Chair
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    short fh = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    ChairMovement cm = new ChairMovement(command, new Point(xpos, ypos), duration, newstate);
                    cm.setFh(fh);
                    res.add(cm);
                    break;
                }*/
                case 15: {
                    short xpos = p.readShort();
                    short ypos = p.readShort();
                    short xwobble = p.readShort();
                    short ywobble = p.readShort();
                    short fh = p.readShort();
                    short ofh = p.readShort();
                    byte newstate = p.readByte();
                    short duration = p.readShort();
                    JumpDownMovement jdm = new JumpDownMovement(command, new Point(xpos, ypos), duration, newstate);
                    jdm.setFh(fh);
                    jdm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    jdm.setOriginFh(ofh);
                    res.add(jdm);
                    break;
                }
                case 21: {//Causes aran to do weird stuff when attacking o.o
                    /*byte newstate = lea.readByte();
                     short unk = lea.readShort();
                     AranMovement am = new AranMovement(command, null, unk, newstate);
                     res.add(am);*/
                    p.skip(3);
                    break;
                }
                default:
                    log.warn("Unhandled case: {}", command);
                    throw new EmptyMovementException(p);
            }
        }

        if (res.isEmpty()) {
            throw new EmptyMovementException(p);
        }
        return res;
    }

    protected void generateFloatMovementCommand(Character chr, Point relBodyMove) {

    }

    protected void updatePosition(InPacket p, AnimatedMapObject target, int yOffset) throws EmptyMovementException {

        byte numCommands = p.readByte();
        if (numCommands < 1) {
            throw new EmptyMovementException(p);
        }
        for (byte i = 0; i < numCommands; i++) {
            byte command = p.readByte();
            switch (command) {
                case 0: // normal move
                case 5:
                case 17: { // Float
                    //Absolute movement - only this is important for the server, other movement can be passed to the client
                    short xpos = p.readShort(); //is signed fine here?
                    short ypos = p.readShort();
                    target.setPosition(new Point(xpos, ypos + yOffset));
                    p.skip(6); //xwobble = lea.readShort(); ywobble = lea.readShort(); fh = lea.readShort();
                    byte newstate = p.readByte();
                    target.setStance(newstate);
                    p.readShort(); //duration
                    break;
                }
                case 1:
                case 2:
                case 6: // fj
                case 12:
                case 13: // Shot-jump-back thing
                case 16: // Float
                case 18:
                case 19: // Springs on maps
                case 20: // Aran Combat Step
                case 22: {
                    //Relative movement - server only cares about stance
                    p.skip(4); //xpos = lea.readShort(); ypos = lea.readShort();
                    byte newstate = p.readByte();
                    target.setStance(newstate);
                    p.readShort(); //duration
                    break;
                }
                case 3:
                case 4: // tele... -.-
                case 7: // assaulter
                case 8: // assassinate
                case 9: // rush
                case 11: //chair
                {
//                case 14: {
                    //Teleport movement - same as above
                    p.skip(8); //xpos = lea.readShort(); ypos = lea.readShort(); xwobble = lea.readShort(); ywobble = lea.readShort();
                    byte newstate = p.readByte();
                    target.setStance(newstate);
                    break;
                }
                case 14:
                    p.skip(9); // jump down (?)
                    break;
                case 10: // Change Equip
                    //ignored server-side
                    p.readByte();
                    break;
                /*case 11: { // Chair
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    short fh = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    ChairMovement cm = new ChairMovement(command, new Point(xpos, ypos), duration, newstate);
                    cm.setFh(fh);
                    res.add(cm);
                    break;
                }*/
                case 15: {
                    //Jump down movement - stance only
                    p.skip(12); //short xpos = lea.readShort(); ypos = lea.readShort(); xwobble = lea.readShort(); ywobble = lea.readShort(); fh = lea.readShort(); ofh = lea.readShort();
                    byte newstate = p.readByte();
                    target.setStance(newstate);
                    p.readShort(); // duration
                    break;
                }
                case 21: {//Causes aran to do weird stuff when attacking o.o
                    /*byte newstate = lea.readByte();
                     short unk = lea.readShort();
                     AranMovement am = new AranMovement(command, null, unk, newstate);
                     res.add(am);*/
                    p.skip(3);
                    break;
                }
                default:
                    log.warn("Unhandled Case: {}", command);
                    throw new EmptyMovementException(p);
            }
        }
    }
}
