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

package server.events.gm;

import client.Character;
import constants.id.MapId;
import net.packet.Packet; // <--- Added Import
import server.TimerManager;
import server.maps.MapleMap;
import tools.PacketCreator;

import java.util.LinkedList;
import java.util.List;

/**
 * @author kevintjuh93
 */
public class Snowball {
    private final MapleMap map;
    private int position = 0;
    private int hits = 3;
    private int snowmanHp = 1000;
    private boolean hittable = false;
    private final int team;
    private boolean winner = false;
    private final List<Character> characters = new LinkedList<>();

    public Snowball(int team, MapleMap map) {
        this.map = map;
        this.team = team;

        for (Character chr : map.getCharacters()) {
            if (chr.getTeam() == team) {
                characters.add(chr);
            }
        }
    }

    public void startEvent() {
        if (hittable) return;

        for (Character chr : characters) {
            if (chr != null) {
                chr.sendPacket(PacketCreator.rollSnowBall(false, 1, map.getSnowball(0), map.getSnowball(1)));
                chr.sendPacket(PacketCreator.getClock(600));
            }
        }
        hittable = true;
        TimerManager.getInstance().schedule(() -> {
            int posTeam = map.getSnowball(team).getPosition();
            int posEnemy = map.getSnowball(team == 0 ? 1 : 0).getPosition();

            if (posTeam > posEnemy) {
                broadcast(PacketCreator.rollSnowBall(false, 3, map.getSnowball(0), map.getSnowball(0)));
                winner = true;
            } else if (posEnemy > posTeam) {
                broadcast(PacketCreator.rollSnowBall(false, 4, map.getSnowball(0), map.getSnowball(0)));
                winner = true;
            }
            warpOut();
        }, 600000);
    }

    public boolean isHittable() { return hittable; }
    public void setHittable(boolean hit) { this.hittable = hit; }

    public int getPosition() { return position; }

    public int getSnowmanHP() { return snowmanHp; }
    public void setSnowmanHP(int hp) { this.snowmanHp = hp; }

    public void hit(int what, int damage) {
        if (what < 2) {
            if (damage > 0) {
                this.hits--;
            } else {
                if (this.snowmanHp - damage < 0) {
                    this.snowmanHp = 0;
                    TimerManager.getInstance().schedule(() -> {
                        setSnowmanHP(7500);
                        message(5);
                    }, 10000);
                } else {
                    this.snowmanHp -= damage;
                }
                map.broadcastMessage(PacketCreator.rollSnowBall(false, 1, map.getSnowball(0), map.getSnowball(1)));
            }
        }

        if (this.hits <= 0) {
            this.position += 1;

            Snowball otherBall = map.getSnowball(team == 0 ? 1 : 0);
            if (this.position == 45) otherBall.message(1);
            else if (this.position == 290) otherBall.message(2);
            else if (this.position == 560) otherBall.message(3);

            this.hits = 3;
            map.broadcastMessage(PacketCreator.rollSnowBall(false, 0, map.getSnowball(0), map.getSnowball(1)));
            map.broadcastMessage(PacketCreator.rollSnowBall(false, 1, map.getSnowball(0), map.getSnowball(1)));
        }
        map.broadcastMessage(PacketCreator.hitSnowBall(what, damage));
    }

    public void message(int message) {
        broadcast(PacketCreator.snowballMessage(team, message));
    }

    // FIX IS HERE: Changed parameter from byte[] to Packet
    private void broadcast(Packet packet) {
        for (Character chr : characters) {
            if (chr != null) chr.sendPacket(packet);
        }
    }

    public void warpOut() {
        TimerManager.getInstance().schedule(() -> {
            if (winner) {
                map.warpOutByTeam(team, MapId.EVENT_WINNER);
            } else {
                map.warpOutByTeam(team, MapId.EVENT_EXIT);
            }
            map.setSnowball(team, null);
        }, 10000);
    }
}