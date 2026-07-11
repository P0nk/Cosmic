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

import client.Character;
import client.Client;
import client.Skill;
import client.SkillFactory;
import constants.skills.*;
import net.packet.Packet;
import server.StatEffect;
import server.life.MobSkill;
import server.life.Monster;
import tools.PacketCreator;

import java.awt.*;

/**
 * @author LaiLaiNoob
 */
public class Mist extends AbstractMapObject {
    private final Rectangle mistPosition;
    private Character owner = null;
    private Monster mob = null;
    private StatEffect source;
    private MobSkill skill;
    private final boolean isMobMist;
    private boolean isPoisonMist;
    private boolean isRecoveryMist;
    private final int skillDelay;

    public Mist(Rectangle mistPosition, Monster mob, MobSkill skill) {
        this.mistPosition = mistPosition;
        this.mob = mob;
        this.skill = skill;
        isMobMist = true;
        isPoisonMist = true;
        isRecoveryMist = false;
        skillDelay = 0;
    }

    public Mist(Rectangle mistPosition, Character owner, StatEffect source) {
        this.mistPosition = mistPosition;
        this.owner = owner;
        this.source = source;
        this.skillDelay = 8;
        this.isMobMist = false;
        this.isRecoveryMist = false;
        this.isPoisonMist = false;
        switch (source.getSourceId()) {
            case Evan.RECOVERY_AURA:
                isRecoveryMist = true;
                break;

            case Shadower.SMOKE_SCREEN: // Smoke Screen
                isPoisonMist = false;
                break;

            case FPMage.POISON_MIST: // FP mist
            case BlazeWizard.FLAME_GEAR: // Flame Gear
            case NightWalker.POISON_BOMB: // Poison Bomb
                isPoisonMist = true;
                break;
        }
    }

    @Override
    public MapObjectType getType() {
        return MapObjectType.MIST;
    }

    @Override
    public Point getPosition() {
        return mistPosition.getLocation();
    }

    public Skill getSourceSkill() {
        return SkillFactory.getSkill(source.getSourceId());
    }

    public boolean isMobMist() {
        return isMobMist;
    }

    public boolean isPoisonMist() {
        return isPoisonMist;
    }

    public boolean isRecoveryMist() {
        return isRecoveryMist;
    }

    public int getSkillDelay() {
        return skillDelay;
    }

    public Monster getMobOwner() {
        return mob;
    }

    public Character getOwner() {
        return owner;
    }

    public Rectangle getBox() {
        return mistPosition;
    }

    @Override
    public void setPosition(Point position) {
        throw new UnsupportedOperationException();
    }

    public final Packet makeDestroyData() {
        return PacketCreator.removeMist(getObjectId());
    }

    public final Packet makeSpawnData() {
        if (owner != null) {
            return PacketCreator.spawnMist(getObjectId(), owner.getId(), getSourceSkill().getId(), owner.getSkillLevel(SkillFactory.getSkill(source.getSourceId())), this, false);
        }
        return PacketCreator.spawnMobMist(getObjectId(), mob.getId(), skill.getId(), this);
    }

    public final Packet makeFakeSpawnData(int level, boolean veil) {
        if (owner != null) {
            return PacketCreator.spawnMist(getObjectId(), owner.getId(), 8001007, level, this, veil);
        }
        return PacketCreator.spawnMobMist(getObjectId(), mob.getId(), skill.getId(), this);
    }

    @Override
    public void sendSpawnData(Client client) {
        client.sendPacket(makeSpawnData());
    }

    @Override
    public void sendDestroyData(Client client) {
        client.sendPacket(makeDestroyData());
    }

    public boolean makeChanceResult() {
        return source.makeChanceResult();
    }
}
