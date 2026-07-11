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
import client.Skill;
import client.SkillFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import config.YamlConfig;
import constants.skills.*;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.server.Server;
import server.StatEffect;
import server.TimerManager;
import server.life.Monster;
import server.maps.MapleMap;
import tools.PacketCreator;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;

public final class SpecialMoveHandler extends AbstractPacketHandler {

    @Override
    public void handlePacket(InPacket p, Client c) {
        Character chr = c.getPlayer();
        p.readInt();
        chr.getAutobanManager().setTimestamp(4, Server.getInstance().getCurrentTimestamp(), 28);
        int skillid = p.readInt();

        if (skillid == 1321016) { // za warudo
            System.out.println("hello?");
            MapleMap map = chr.getMap();
            map.getAllMonsters().forEach(mob -> {
                Map<MonsterStatus, Integer> zw = new HashMap<>();
                zw.put(MonsterStatus.FREEZE, 1);
                MonsterStatusEffect mse = new MonsterStatusEffect(zw, SkillFactory.getSkill(1321016), null, false);
                mob.applyStatus(c.getPlayer(), mse, false, 8000L, false, true);
            });
        }
        Point pos = null;
        int __skillLevel = p.readByte();
        Skill skill = SkillFactory.getSkill(skillid);
        int skillLevel = chr.getSkillLevel(skill);
        if (skillid % 10000000 == 1010 || skillid % 10000000 == 1011) {
            if (chr.getDojoEnergy() < 10000) { // PE hacking or maybe just lagging
                return;
            }
            skillLevel = 1;
            chr.setDojoEnergy(0);
            c.sendPacket(PacketCreator.getEnergy("energy", chr.getDojoEnergy()));
            c.sendPacket(PacketCreator.serverNotice(5, "As you used the secret skill, your energy bar has been reset."));
        }
        if (skillLevel == 0 || skillLevel != __skillLevel) {
            return;
        }

        StatEffect effect = skill.getEffect(skillLevel);
        if (effect.getCooldown() > 0) {
            if (chr.skillIsCooling(skillid)) {
                return;
            } else if (skillid != Corsair.BATTLE_SHIP) {
                int cooldownTime = effect.getCooldown();
                if (StatEffect.isHerosWill(skillid) && YamlConfig.config.server.USE_FAST_REUSE_HERO_WILL) {
                    cooldownTime /= 60;
                }

                c.sendPacket(PacketCreator.skillCooldown(skillid, cooldownTime));
                chr.addCooldown(skillid, currentServerTime(), SECONDS.toMillis(cooldownTime));
            }
        }
        if (skillid == Hero.MONSTER_MAGNET || skillid == Paladin.MONSTER_MAGNET || skillid == DarkKnight.MONSTER_MAGNET) { // Monster Magnet
            int num = p.readInt();
            for (int i = 0; i < num; i++) {
                int mobOid = p.readInt();
                byte success = p.readByte();
                chr.getMap().broadcastMessage(chr, PacketCreator.catchMonster(mobOid, success), false);
                Monster monster = chr.getMap().getMonsterByOid(mobOid);
                if (monster != null) {
                    if (!monster.isBoss()) {
                        monster.aggroClearDamages();
                        monster.aggroMonsterDamage(chr, 1);

                        // thanks onechord for pointing out Magnet crashing the caster (issue would actually happen upon failing to catch mob)
                        // thanks Conrad for noticing Magnet crashing when trying to pull bosses and fixed mobs
                        monster.aggroSwitchController(chr, true);
                    }
                }
            }
            byte direction = p.readByte();   // thanks MedicOP for pointing some 3rd-party related issues with Magnet
            chr.getMap().broadcastMessage(chr, PacketCreator.showBuffEffect(chr.getId(), skillid, chr.getSkillLevel(skillid), 1, direction), false);
            c.sendPacket(PacketCreator.enableActions());
            return;
        } else if (skillid == Brawler.MP_RECOVERY) {// MP Recovery
            Skill s = SkillFactory.getSkill(skillid);
            StatEffect ef = s.getEffect(chr.getSkillLevel(s));

            int lose = chr.safeAddHP(-1 * (chr.getCurrentMaxHp() / ef.getX()));
            int gain = -lose * (ef.getY() / 100);
            chr.addMP(gain);
        } else if (skillid == SuperGM.HEAL_PLUS_DISPEL) {
            p.skip(11);
            chr.getMap().broadcastMessage(chr, PacketCreator.showBuffEffect(chr.getId(), skillid, chr.getSkillLevel(skillid)), false);
        } else if (skillid % 10000000 == 1004) {
            p.readShort();
        }

        if (p.available() == 5) {
            pos = new Point(p.readShort(), p.readShort());
        }
        if (chr.isAlive()) {
            if (skill.getId() != Priest.MYSTIC_DOOR) {
                if (skill.getId() % 10000000 != 1005) {
                    skill.getEffect(skillLevel).applyTo(chr, pos);
                } else {
                    skill.getEffect(skillLevel).applyEchoOfHero(chr);
                }
            } else {
                if (c.tryacquireClient()) {
                    try {
                        if (chr.canDoor()) {
                            chr.cancelMagicDoor();
                            skill.getEffect(skillLevel).applyTo(chr, pos);
                        } else {
                            chr.message("Please wait 5 seconds before casting Mystic Door again.");
                        }
                    } finally {
                        c.releaseClient();
                    }
                }

                c.sendPacket(PacketCreator.enableActions());
            }
        }
    }
}