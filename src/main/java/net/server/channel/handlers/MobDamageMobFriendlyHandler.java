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

import client.Client;
import constants.id.MobId;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import scripting.event.EventInstanceManager;
import server.life.Monster;
import server.maps.MapleMap;
import tools.PacketCreator;
import tools.Randomizer;

/**
 * @author Xotic (XoticStory) & BubblesDev
 */

public final class MobDamageMobFriendlyHandler extends AbstractPacketHandler {
    @Override
    public final void handlePacket(InPacket p, Client c) {
        int attacker = p.readInt();
        p.readInt();
        int damaged = p.readInt();

        MapleMap map = c.getPlayer().getMap();
        Monster monster = map.getMonsterByOid(damaged);

        if (monster == null || map.getMonsterByOid(attacker) == null) {
            return;
        }

        int damage = Randomizer.nextInt((int) (((monster.getMaxHp() / 13 + monster.getPADamage() * 10)) * 2 + 500)) / 10; // Formula planned by Beng.

        if (monster.getHp() - damage < 1) {     // friendly dies
            switch (monster.getId()) {
            case MobId.WATCH_HOG:
                map.broadcastMessage(PacketCreator.serverNotice(6, "The Watch Hog has been injured by the aliens. Better luck next time..."));
                break;
            case MobId.MOON_BUNNY: //moon bunny
                map.broadcastMessage(PacketCreator.serverNotice(6, "The Moon Bunny went home because he was sick."));
                break;
            case MobId.TYLUS: //tylus
                map.broadcastMessage(PacketCreator.serverNotice(6, "Tylus has fallen by the overwhelming forces of the ambush."));
                break;
            case MobId.JULIET: //juliet
                map.broadcastMessage(PacketCreator.serverNotice(6, "Juliet has fainted in the middle of the combat."));
                break;
            case MobId.ROMEO: //romeo
                map.broadcastMessage(PacketCreator.serverNotice(6, "Romeo has fainted in the middle of the combat."));
                break;
            case MobId.GIANT_SNOWMAN_LV1_EASY, MobId.GIANT_SNOWMAN_LV1_MEDIUM, MobId.GIANT_SNOWMAN_LV1_HARD:
                map.broadcastMessage(PacketCreator.serverNotice(6, "The Snowman has melted on the heat of the battle."));
                break;
            case MobId.DELLI: //delli
                map.broadcastMessage(PacketCreator.serverNotice(6, "Delli vanished after the ambush, sheets still laying on the ground..."));
                break;
            }
            
            map.killFriendlies(monster);
        } else {
            EventInstanceManager eim = map.getEventInstance();
            if (eim != null) {
                eim.friendlyDamaged(monster);
            }
        }

        monster.applyAndGetHpDamage(damage, false);
        long remainingHp = monster.getHp(); // slimy edits
        if (remainingHp <= 0) {
            remainingHp = 0;
            map.removeMapObject(monster);
        }

        map.broadcastMessage(PacketCreator.MobDamageMobFriendly(monster, damage, remainingHp), monster.getPosition());
        c.sendPacket(PacketCreator.enableActions());
    }
}