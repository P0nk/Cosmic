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
import client.autoban.AutobanFactory;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.WeaponType;
import client.status.MonsterStatusEffect;
import constants.skills.Outlaw;
import net.packet.InPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ItemInformationProvider;
import server.StatEffect;
import server.life.Monster;
import server.life.MonsterInformationProvider;
import server.maps.Summon;
import tools.PacketCreator;

import java.util.ArrayList;
import java.util.List;

public final class SummonDamageHandler extends AbstractDealDamageHandler {
    private static final Logger log = LoggerFactory.getLogger(SummonDamageHandler.class);

    public final class SummonAttackEntry {

        private final int monsterOid;
        private final int damage;

        public SummonAttackEntry(int monsterOid, int damage) {
            this.monsterOid = monsterOid;
            this.damage = damage;
        }

        public int getMonsterOid() {
            return monsterOid;
        }

        public int getDamage() {
            return damage;
        }

    }

    @Override
    public void handlePacket(InPacket p, Client c) {
        int oid = p.readInt();
        Character player = c.getPlayer();
        if (!player.isAlive()) {
            return;
        }
        Summon summon = null;
        for (Summon sum : player.getSummonsValues()) {
            if (sum.getObjectId() == oid) {
                summon = sum;
            }
        }
        if (summon == null) {
            return;
        }
        Skill summonSkill = SkillFactory.getSkill(summon.getSkill());
        StatEffect summonEffect = summonSkill.getEffect(summon.getSkillLevel());
        p.skip(4);
        List<SummonAttackEntry> allDamage = new ArrayList<>();
        byte direction = p.readByte();
        int numAttacked = p.readByte();
        p.skip(8); // I failed lol (mob x,y and summon x,y), Thanks Gerald
        for (int x = 0; x < numAttacked; x++) {
            int monsterOid = p.readInt(); // attacked oid
            p.skip(18);
            int damage = p.readInt();
            allDamage.add(new SummonAttackEntry(monsterOid, damage));
        }
        player.getMap().broadcastMessage(player, PacketCreator.summonAttack(player.getId(), summon.getObjectId(), direction, allDamage), summon.getPosition());

        if (player.getMap().isOwnershipRestricted(player)) {
            return;
        }

        boolean magic = summonEffect.getWatk() == 0;
        int maxDmg = calcMaxDamage(summonEffect, player, magic);    // thanks Darter (YungMoozi) for reporting unchecked max dmg
        for (SummonAttackEntry attackEntry : allDamage) {
            int damage = attackEntry.getDamage();
            Monster target = player.getMap().getMonsterByOid(attackEntry.getMonsterOid());
            if (target != null) {
                /*if (damage > maxDmg) {
                    AutobanFactory.DAMAGE_HACK.alert(c.getPlayer(), "Possible packet editing summon damage exploit.");
                    final String mobName = MonsterInformationProvider.getInstance().getMobNameFromId(target.getId());
                    log.info("Possible exploit - chr {} used a summon of skillId {} to attack {} with damage {} (max: {})",
                            c.getPlayer().getName(), summon.getSkill(), mobName, damage, maxDmg);
                    damage = maxDmg;
                }*/
                if(player.damageCheckinProgress) {
                    player.damageCheck += damage;
                }

                if (damage > 0 && summonEffect.getMonsterStati().size() > 0) {
                    if (summonEffect.makeChanceResult()) {
                        target.applyStatus(player, new MonsterStatusEffect(summonEffect.getMonsterStati(), summonSkill, null, false), summonEffect.isPoison(), 4000);
                    }
                }
                player.getMap().damageMonster(player, target, damage);
            }
        }

        if (summon.getSkill() == Outlaw.GAVIOTA) {  // thanks Periwinks for noticing Gaviota not cancelling after grenade toss
            player.cancelEffect(summonEffect, false, -1);
        }
    }

    private static int calcMaxDamage(StatEffect summonEffect, Character player, boolean magic) {
        double maxDamage;

        if (magic) {
            int matk = Math.max(player.getTotalMagic(), 14);
            maxDamage = player.calculateMaxBaseMagicDamage(matk) * (0.05 * summonEffect.getMatk());
        } else {
            int watk = Math.max(player.getTotalWatk(), 14);
            Item weapon_item = player.getInventory(InventoryType.EQUIPPED).getItem((short) -11);

            int maxBaseDmg;  // thanks Conrad, Atoot for detecting some summons legitimately hitting over the calculated limit
            if (weapon_item != null) {
                maxBaseDmg = player.calculateMaxBaseDamage(watk, ItemInformationProvider.getInstance().getWeaponType(weapon_item.getItemId()));
            } else {
                maxBaseDmg = player.calculateMaxBaseDamage(watk, WeaponType.SWORD1H);
            }

            float summonDmgMod = (maxBaseDmg >= 438) ? 0.054f : 0.077f;
            maxDamage = maxBaseDmg * (summonDmgMod * summonEffect.getWatk());
        }

        return (int) maxDamage;
    }
}
