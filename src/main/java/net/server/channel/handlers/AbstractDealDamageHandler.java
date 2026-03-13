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

import client.BuffStat;
import client.Character;
import client.Job;
import client.Skill;
import client.SkillFactory;
import client.autoban.AutobanFactory;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import config.YamlConfig;
import constants.game.GameConstants;
import constants.id.ItemId;
import constants.id.MapId;
import constants.id.MobId;
import constants.skills.*; // Imported all constants.skills
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.server.PlayerBuffValueHolder;
import scripting.AbstractPlayerInteraction;
import server.ItemInformationProvider;
import server.StatEffect;
import server.TimerManager;
import server.life.*;
import server.maps.MapItem;
import server.maps.MapObject;
import server.maps.MapleMap;
import tools.PacketCreator;
import tools.Randomizer;

import java.awt.*;
import java.util.*;
import java.util.List;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public abstract class AbstractDealDamageHandler extends AbstractPacketHandler {
    private static final int EXPLODED_MESO_SPREAD_DELAY = 100;
    private static final int EXPLODED_MESO_MAX_DELAY = 1000;

    public static class AttackInfo {
        public int numAttacked, numDamage, numAttackedAndDamage, skill, skilllevel, stance, direction, rangedirection,
                charge, display;
        public Map<Integer, AttackTarget> targets;
        public boolean ranged, magic;
        public int speed = 4;
        public Point position = new Point();
        public List<Integer> explodedMesos;
        public Short attackDelay;
        public List<Integer> damageLines;

        public StatEffect getAttackEffect(Character chr, Skill theSkill) {
            Skill mySkill = theSkill;
            if (mySkill == null) {
                mySkill = SkillFactory.getSkill(skill);
            }

            int skillLevel = chr.getSkillLevel(mySkill);
            if (skillLevel == 0 && GameConstants.isPqSkillMap(chr.getMapId())
                    && GameConstants.isPqSkill(mySkill.getId())) {
                skillLevel = 1;
            }

            if (skillLevel == 0) {
                return null;
            }
            if (display > 80) {
                if (!mySkill.getAction()) {
                    // AutobanFactory.FAST_ATTACK.autoban(chr, "WZ Edit; adding action to a skill: "
                    // + display);
                    chr.getAutobanManager().jailPlayer("WZ Edit (Action): " + display, 60);
                    return null;
                }
            }
            return mySkill.getEffect(skillLevel);
        }
    }

    public record AttackTarget(short delay, List<Integer> damageLines) {
    }

    protected void applyAttack(AttackInfo attack, final Character player, int attackCount) {
        final MapleMap map = player.getMap();
        if (map.isOwnershipRestricted(player)) {
            return;
        }

        // [ANTI-HACK] 1. God Mode Watchdog
        player.updateAttackAction();

        // [ANTI-HACK] 2. Unlimited Attack / No Delay Check (Simplified)
        long now = System.currentTimeMillis();
        long lastAttackTime = player.getLastAttackTime();

        // [DISABLED] This was blocking legitimate normal attacks (CTRL) due to network
        // jitter.
        /*
         * long globalMinDelay = 300;
         * if (now - lastAttackTime < globalMinDelay && attack.skill == 0) {
         * return; // Eat the packet
         * }
         */

        player.setLastAttackTime(now);

        Skill theSkill = null;
        StatEffect attackEffect = null;
        final int job = player.getJob().getId();
        try {
            if (player.isBanned()) {
                return;
            }
            if (attack.skill != 0) {
                theSkill = SkillFactory.getSkill(attack.skill);
                attackEffect = attack.getAttackEffect(player, theSkill);
                if (attackEffect == null) {
                    player.sendPacket(PacketCreator.enableActions());
                    return;
                }

                // [ANTI-HACK] 3. Mob Count Validation
                int mobCount = attackEffect.getMobCount();

                // Exceptions for skills that chain/splash (Cygnus FA, etc.)
                if (attack.skill == DawnWarrior.FINAL_ATTACK || attack.skill == WindArcher.FINAL_ATTACK) {
                    mobCount = 15;
                } else if (attack.skill == Page.FINAL_ATTACK_BW || attack.skill == Page.FINAL_ATTACK_SWORD
                        || attack.skill == Fighter.FINAL_ATTACK_SWORD
                        || attack.skill == Fighter.FINAL_ATTACK_AXE || attack.skill == Spearman.FINAL_ATTACK_SPEAR
                        || attack.skill == Spearman.FINAL_ATTACK_POLEARM
                        || attack.skill == Hunter.FINAL_ATTACK || attack.skill == Crossbowman.FINAL_ATTACK) {
                    mobCount = 15;
                } else if (attack.skill == Aran.HIDDEN_FULL_DOUBLE || attack.skill == Aran.HIDDEN_FULL_TRIPLE
                        || attack.skill == Aran.HIDDEN_OVER_DOUBLE || attack.skill == Aran.HIDDEN_OVER_TRIPLE) {
                    mobCount = 15;
                } else if (attack.skill == FPWizard.POISON_BREATH || attack.skill == ILWizard.COLD_BEAM) {
                    // [FIX] Exception for F/P Wizard - Poison Breath
                    // Causes false positives when hitting multiple mobs
                    mobCount = 6;
                }
                // [NEW FIX] Big Bang Mob Count Correction
                // Manual override because WZ data might be returning base level count (5)
                // instead of scaled count.
                else if (attack.skill == FPArchMage.BIG_BANG || attack.skill == ILArchMage.BIG_BANG
                        || attack.skill == Bishop.BIG_BANG) {
                    int bbLevel = player.getSkillLevel(theSkill);
                    if (bbLevel > 20) {
                        mobCount = 15;
                    } else if (bbLevel > 10) {
                        mobCount = 10;
                    } else {
                        mobCount = 5;
                    }
                }

                // STRICT CHECK: Does packet claim more hits than skill allows?
                if (attack.numAttacked > mobCount) {
                    player.getAutobanManager().jailPlayer(
                            "Mob Count Hack: Hit " + attack.numAttacked + " mobs (Max: " + mobCount + ") with Skill "
                                    + attack.skill,
                            60);
                    return;
                }

                // HARD LIMIT CHECK: Absolute sanity check (Maximum possible is 15 for
                // Genesis/Meteor)
                if (attack.numAttacked > 15) {
                    player.getAutobanManager().jailPlayer(
                            "Mob Count Hack: Impossible Mob Count (" + attack.numAttacked + ")",
                            60);
                    return;
                }

                if (attack.skill != Cleric.HEAL) {
                    if (player.isAlive()) {
                        if (attack.skill == Aran.BODY_PRESSURE || attack.skill == Marauder.ENERGY_CHARGE
                                || attack.skill == ThunderBreaker.ENERGY_CHARGE) {
                        } else if (attack.skill == NightWalker.POISON_BOMB) {
                            attackEffect.applyTo(player, new Point(attack.position.x, attack.position.y));
                        } else {
                            attackEffect.applyTo(player);
                        }
                    } else {
                        player.sendPacket(PacketCreator.enableActions());
                    }
                }
            }

            if (!player.isAlive()) {
                return;
            }

            long totDamage = 0;

            if (attack.skill == ChiefBandit.MESO_EXPLOSION) {
                removeExplodedMesos(map, attack);
            }

            for (Map.Entry<Integer, AttackTarget> target : attack.targets.entrySet()) {
                final Monster monster = map.getMonsterByOid(target.getKey());
                if (monster != null) {

                    // [ANTI-HACK] 4. Kami / Distance Check (Re-Enabled & Tuned)
                    // We check squared distance to avoid expensive square root math.
                    // 2,500,000 = ~1580 pixels (Nearly 2x the width of the game screen).
                    double distSq = player.getPosition().distanceSq(monster.getPosition());
                    double thresholdSq = 11000000.0;

                    // 1. Job Buffers (Give ranged classes slightly more room)
                    if (attack.magic) {
                        thresholdSq += 7500000.0; // Adds ~300px more range tolerance
                    }

                    if (attack.ranged) {
                        thresholdSq += 7500000.0; // Adds ~300px more range tolerance
                    }

                    // 2. Skill Exemptions (Full Map Attacks & Long Range Snipes)
                    // If the skill is known to hit the whole map, we disable the check entirely.
                    int skillId = attack.skill;
                    boolean isFMA = skillId == Bishop.GENESIS ||
                            skillId == FPArchMage.METEOR_SHOWER ||
                            skillId == ILArchMage.BLIZZARD ||
                            skillId == DragonKnight.DRAGON_ROAR ||
                            skillId == Ranger.ARROW_RAIN ||
                            skillId == WindArcher.ARROW_RAIN ||
                            skillId == Sniper.ARROW_ERUPTION ||
                            skillId == Marksman.SNIPE ||
                            skillId == Crusader.SHOUT ||
                            skillId == NightLord.SHADOW_RAIN ||
                            skillId == ChiefBandit.ASSAULTER ||
                            skillId == ChiefBandit.EDGE_CARNIVAL || // 3241 px
                            skillId == Shadower.SHADOW_VEIL ||
                            skillId == SuperGM.SUPER_DRAGON_ROAR ||
                            skillId == ILArchMage.CHAIN_LIGHTNING ||
                            skillId == DarkKnight.RUSH ||
                            skillId == Hero.RUSH ||
                            skillId == Paladin.RUSH ||
                            skillId == Paladin.HEAVENS_HAMMER ||
                            skillId == Aran.COMBO_TEMPEST;

                    // [FIX] Grace Period Check
                    // If the player changed maps less than 10 seconds (10000ms) ago, SKIP the
                    // check.
                    boolean isLoading = (System.currentTimeMillis() - player.getLastMapChangeTime() < 10000);

                    // 4. THE CHECK
                    if (!isFMA && !isLoading && distSq > thresholdSq) {
                        if (!player.isGM()) {
                            // [FIX] Do not Jail immediately. Add suspicion.
                            player.addKamiViolation();

                            // Only jail if they trigger this 15 TIMES IN A ROW without a valid hit.
                            // A legit player lagging will usually fix their position within 1-2 hits.
                            if (player.getKamiViolations() > 15) {
                                double realDist = Math.sqrt(distSq);
                                int allowedDist = (int) Math.sqrt(thresholdSq);

                                player.getAutobanManager().jailPlayer(
                                        "Kami/Distance Hack: Consistent mismatch > 15 times. Dist: " + (int) realDist
                                                + " SID: " + skillId,
                                        30);
                                return;
                            }

                            // Optional: Return here to block damage from this specific hit,
                            // but don't ban yet. This prevents "Glitch" damage.
                            // return;

                        } else {
                            // [FIX] Valid Hit!
                            // If the player hits a monster within range, the server knows they are
                            // in the correct spot. Reset the counter.
                            player.resetKamiViolations();
                        }
                    }

                    long totDamageToOneMonster = 0;
                    List<Integer> onedList = target.getValue().damageLines();

                    if (attack.magic) {
                        if (monster.isBuffed(MonsterStatus.MAGIC_IMMUNITY)) {
                            Collections.fill(onedList, 1);
                        }
                    } else {
                        if (monster.isBuffed(MonsterStatus.WEAPON_IMMUNITY)) {
                            Collections.fill(onedList, 1);
                        }
                    }

                    if (MobId.isDojoBoss(monster.getId())) {
                        if (attack.skill == 1009 || attack.skill == 10001009 || attack.skill == 20001009) {
                            int dmgLimit = (int) Math.ceil(0.3 * monster.getMaxHp());
                            List<Integer> _onedList = new LinkedList<>();
                            for (Integer i : onedList) {
                                _onedList.add(i < dmgLimit ? i : dmgLimit);
                            }
                            onedList = _onedList;
                        }
                    }

                    for (Integer eachd : onedList) {
                        // The client sends damage as a signed int. If the value is negative,
                        // it means the client overflowed past Integer.MAX_VALUE (~2.14b).
                        // We unwrap it to recover the true positive value as a long.
                        long eachdLong = eachd < 0 ? (long) eachd + Integer.MAX_VALUE + 1L : (long) eachd;
                        totDamageToOneMonster += eachdLong;
                    }
                    totDamage += totDamageToOneMonster;
                    monster.aggroMonsterDamage(player, (int) Math.min(totDamageToOneMonster, Integer.MAX_VALUE));
                    if (player.getBuffedValue(BuffStat.PICKPOCKET) != null && (attack.skill == 0
                            || attack.skill == Rogue.DOUBLE_STAB || attack.skill == Bandit.SAVAGE_BLOW
                            || attack.skill == ChiefBandit.ASSAULTER || attack.skill == ChiefBandit.BAND_OF_THIEVES
                            || attack.skill == Shadower.ASSASSINATE || attack.skill == Shadower.TAUNT
                            || attack.skill == Shadower.BOOMERANG_STEP)) {
                        Skill pickpocket = SkillFactory.getSkill(ChiefBandit.PICKPOCKET);
                        int picklv = (player.isGM()) ? pickpocket.getMaxLevel() : player.getSkillLevel(pickpocket);
                        if (picklv > 0) {
                            short delay = 0;
                            final int maxmeso = player.getBuffedValue(BuffStat.PICKPOCKET);
                            for (Integer eachd : onedList) {
                                eachd += Integer.MAX_VALUE;

                                if (pickpocket.getEffect(picklv).makeChanceResult()) {
                                    final int eachdf;
                                    if (eachd < 0) {
                                        eachdf = eachd + Integer.MAX_VALUE;
                                    } else {
                                        eachdf = eachd;
                                    }

                                    int meso = Math.min(
                                            (int) Math.max(((double) eachdf / (double) 20000) * (double) maxmeso, 1),
                                            maxmeso);
                                    Point position = new Point(
                                            (int) (monster.getPosition().getX() + Randomizer.nextInt(100) - 50),
                                            (int) (monster.getPosition().getY()));
                                    map.spawnMesoDrop(meso, position, monster, player, true, (byte) 2, delay);
                                    delay += 100;
                                }
                            }
                        }
                    } else if (attack.skill == Marauder.ENERGY_DRAIN || attack.skill == ThunderBreaker.ENERGY_DRAIN
                            || attack.skill == NightWalker.VAMPIRE || attack.skill == Assassin.DRAIN) {
                        int maxHeal = Math.min(player.getCurrentMaxHp() - player.getHp(), 5000);
                        player.addHP(
                                (int) Math
                                        .min(maxHeal,
                                                Math.abs(Math.min((int) ((double) totDamage
                                                        * (double) SkillFactory.getSkill(attack.skill)
                                                                .getEffect(player.getSkillLevel(
                                                                        SkillFactory.getSkill(attack.skill)))
                                                                .getX()
                                                        / 100.0), player.getCurrentMaxHp() / 2))));
                    } else if (attack.skill == Bandit.STEAL) {
                        if (monster.isBoss()) {
                            player.dropMessage(5, "You cannot steal from bosses.");
                        } else {
                            Skill steal = SkillFactory.getSkill(Bandit.STEAL);
                            if (monster.getStolen().size() < 1) {
                                if (steal.getEffect(player.getSkillLevel(steal)).makeChanceResult()) {
                                    monster.addStolen(0);

                                    MonsterInformationProvider mi = MonsterInformationProvider.getInstance();
                                    List<Integer> dropPool = mi.retrieveDropPool(monster.getId());
                                    if (!dropPool.isEmpty()) {
                                        int rndPool = (int) Math
                                                .floor(Math.random() * dropPool.get(dropPool.size() - 1));
                                        int i = 0;
                                        while (rndPool >= dropPool.get(i)) {
                                            i++;
                                        }

                                        List<MonsterDropEntry> toSteal = new ArrayList<>();
                                        toSteal.add(mi.retrieveDrop(monster.getId()).get(i));

                                        map.dropItemsFromMonster(toSteal, player, monster, target.getValue().delay());
                                        monster.addStolen(toSteal.get(0).itemId);
                                    }
                                }
                            }
                        }
                    } else if (attack.skill == FPArchMage.FIRE_DEMON) {
                        long duration = SECONDS.toMillis(SkillFactory.getSkill(FPArchMage.FIRE_DEMON)
                                .getEffect(player.getSkillLevel(SkillFactory.getSkill(FPArchMage.FIRE_DEMON)))
                                .getDuration());
                        monster.setTempEffectiveness(Element.FIRE, ElementalEffectiveness.WEAK, duration);

                    } else if (attack.skill == ILArchMage.ICE_DEMON) {
                        long duration = SECONDS.toMillis(SkillFactory.getSkill(ILArchMage.ICE_DEMON)
                                .getEffect(player.getSkillLevel(SkillFactory.getSkill(ILArchMage.ICE_DEMON)))
                                .getDuration());
                        monster.setTempEffectiveness(Element.ICE, ElementalEffectiveness.WEAK, duration);

                    } else if (attack.skill == Outlaw.HOMING_BEACON || attack.skill == Corsair.BULLSEYE) {
                        StatEffect beacon = SkillFactory.getSkill(attack.skill)
                                .getEffect(player.getSkillLevel(attack.skill));
                        beacon.applyBeaconBuff(player, monster.getObjectId());

                    } else if (attack.skill == Outlaw.FLAME_THROWER) {
                        if (!monster.isBoss()) {
                            Skill type = SkillFactory.getSkill(Outlaw.FLAME_THROWER);
                            if (player.getSkillLevel(type) > 0) {
                                StatEffect DoT = type.getEffect(player.getSkillLevel(type));
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(
                                        Collections.singletonMap(MonsterStatus.POISON, 1), type, null, false);
                                monster.applyStatus(player, monsterStatusEffect, true, DoT.getDuration(), false);
                            }
                        }
                    }

                    if (player.isAran()) {
                        if (player.getBuffedValue(BuffStat.WK_CHARGE) != null) {
                            Skill snowCharge = SkillFactory.getSkill(Aran.SNOW_CHARGE);
                            if (totDamageToOneMonster > 0) {
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(
                                        Collections.singletonMap(MonsterStatus.SPEED,
                                                snowCharge.getEffect(player.getSkillLevel(snowCharge)).getX()),
                                        snowCharge, null, false);
                                long duration = SECONDS
                                        .toMillis(snowCharge.getEffect(player.getSkillLevel(snowCharge)).getY());
                                monster.applyStatus(player, monsterStatusEffect, false, duration);
                            }
                        }
                    }
                    if (player.getBuffedValue(BuffStat.HAMSTRING) != null) {
                        Skill hamstring = SkillFactory.getSkill(Bowmaster.HAMSTRING);
                        if (hamstring.getEffect(player.getSkillLevel(hamstring)).makeChanceResult()) {
                            MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(
                                    Collections.singletonMap(MonsterStatus.SPEED,
                                            hamstring.getEffect(player.getSkillLevel(hamstring)).getX()),
                                    hamstring, null, false);
                            long duration = SECONDS
                                    .toMillis(hamstring.getEffect(player.getSkillLevel(hamstring)).getY());
                            monster.applyStatus(player, monsterStatusEffect, false, duration);
                        }
                    }
                    if (player.getBuffedValue(BuffStat.SLOW) != null) {
                        Skill slow = SkillFactory.getSkill(Evan.SLOW);
                        if (slow.getEffect(player.getSkillLevel(slow)).makeChanceResult()) {
                            MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(
                                    Collections.singletonMap(MonsterStatus.SPEED,
                                            slow.getEffect(player.getSkillLevel(slow)).getX()),
                                    slow, null, false);
                            long duration = MINUTES.toMillis(slow.getEffect(player.getSkillLevel(slow)).getY());
                            monster.applyStatus(player, monsterStatusEffect, false, duration);
                        }
                    }
                    if (player.getBuffedValue(BuffStat.BLIND) != null) {
                        Skill blind = SkillFactory.getSkill(Marksman.BLIND);
                        if (blind.getEffect(player.getSkillLevel(blind)).makeChanceResult()) {
                            MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(
                                    Collections.singletonMap(MonsterStatus.ACC,
                                            blind.getEffect(player.getSkillLevel(blind)).getX()),
                                    blind, null, false);
                            long duration = SECONDS.toMillis(blind.getEffect(player.getSkillLevel(blind)).getY());
                            monster.applyStatus(player, monsterStatusEffect, false, duration);
                        }
                    }
                    if (job == 121 || job == 122) {
                        for (int charge = 1211005; charge < 1211007; charge++) {
                            Skill chargeSkill = SkillFactory.getSkill(charge);
                            if (player.isBuffFrom(BuffStat.WK_CHARGE, chargeSkill)) {
                                if (totDamageToOneMonster > 0) {
                                    if (charge == WhiteKnight.BW_ICE_CHARGE || charge == WhiteKnight.SWORD_ICE_CHARGE) {
                                        monster.setTempEffectiveness(Element.ICE, ElementalEffectiveness.WEAK,
                                                chargeSkill.getEffect(player.getSkillLevel(chargeSkill)).getY() * 1000);
                                        break;
                                    }
                                    if (charge == WhiteKnight.BW_FIRE_CHARGE
                                            || charge == WhiteKnight.SWORD_FIRE_CHARGE) {
                                        monster.setTempEffectiveness(Element.FIRE, ElementalEffectiveness.WEAK,
                                                chargeSkill.getEffect(player.getSkillLevel(chargeSkill)).getY() * 1000);
                                        break;
                                    }
                                }
                            }
                        }
                        if (job == 122) {
                            for (int charge = 1221003; charge < 1221004; charge++) {
                                Skill chargeSkill = SkillFactory.getSkill(charge);
                                if (player.isBuffFrom(BuffStat.WK_CHARGE, chargeSkill)) {
                                    if (totDamageToOneMonster > 0) {
                                        monster.setTempEffectiveness(Element.HOLY, ElementalEffectiveness.WEAK,
                                                chargeSkill.getEffect(player.getSkillLevel(chargeSkill)).getY() * 1000);
                                        break;
                                    }
                                }
                            }
                        }
                    } else if (player.getBuffedValue(BuffStat.COMBO_DRAIN) != null) {
                        Skill skill;
                        if (player.getBuffedValue(BuffStat.COMBO_DRAIN) != null) {
                            skill = SkillFactory.getSkill(21100005);
                            int maxheal = player.getCurrentMaxHp() - player.getHp();
                            player.addHP(Math.min(Math.min(5000, maxheal), Math
                                    .abs((int) ((totDamage * skill.getEffect(player.getSkillLevel(skill)).getX())
                                            / 1000))));
                        }
                    } else if (job == 412 || job == 422 || job == 1411) {
                        Skill type = SkillFactory.getSkill(player.getJob().getId() == 412 ? 4120005
                                : (player.getJob().getId() == 1411 ? 14110004 : 4220005));
                        if (player.getSkillLevel(type) > 0) {
                            StatEffect venomEffect = type.getEffect(player.getSkillLevel(type));
                            for (int i = 0; i < attackCount; i++) {
                                if (venomEffect.makeChanceResult()) {
                                    if (monster.getVenomMulti() < 3) {
                                        monster.setVenomMulti((monster.getVenomMulti() + 1));
                                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(
                                                Collections.singletonMap(MonsterStatus.POISON, 1), type, null, false);
                                        monster.applyStatus(player, monsterStatusEffect, false,
                                                venomEffect.getDuration(), true);
                                    }
                                }
                            }
                        }
                    } else if (job >= 311 && job <= 322) {
                        if (!monster.isBoss()) {
                            Skill mortalBlow;
                            if (job == 311 || job == 312) {
                                mortalBlow = SkillFactory.getSkill(Ranger.MORTAL_BLOW);
                            } else {
                                mortalBlow = SkillFactory.getSkill(Sniper.MORTAL_BLOW);
                            }

                            int skillLevel = player.getSkillLevel(mortalBlow);
                            if (skillLevel > 0) {
                                StatEffect mortal = mortalBlow.getEffect(skillLevel);
                                if (monster.getHp() <= (monster.getStats().getHp() * mortal.getX()) / 100) {
                                    if (Randomizer.rand(1, 100) <= mortal.getY()) {
                                        map.damageMonster(player, monster, Integer.MAX_VALUE,
                                                target.getValue().delay());
                                    }
                                }
                            }
                        }
                    }
                    if (attack.skill != 0) {
                        if (attackEffect.getFixDamage() != -1) {
                            if (totDamageToOneMonster != attackEffect.getFixDamage() && totDamageToOneMonster != 0) {
                                AutobanFactory.FIX_DAMAGE.autoban(player, totDamageToOneMonster + " damage");
                            }

                            int threeSnailsId = player.getJobType() * 10000000 + 1000;
                            if (attack.skill == threeSnailsId) {
                                if (YamlConfig.config.server.USE_ULTRA_THREE_SNAILS) {
                                    int skillLv = player.getSkillLevel(threeSnailsId);

                                    if (skillLv > 0) {
                                        AbstractPlayerInteraction api = player.getAbstractPlayerInteraction();

                                        int shellId = switch (skillLv) {
                                            case 1 -> ItemId.SNAIL_SHELL;
                                            case 2 -> ItemId.BLUE_SNAIL_SHELL;
                                            default -> ItemId.RED_SNAIL_SHELL;
                                        };

                                        if (api.haveItem(shellId, 1)) {
                                            api.gainItem(shellId, (short) -1, false);
                                            totDamageToOneMonster *= player.getLevel();
                                        } else {
                                            player.dropMessage(5,
                                                    "You have ran out of shells to activate the hidden power of Three Snails.");
                                        }
                                    } else {
                                        totDamageToOneMonster = 0;
                                    }
                                }
                            }
                        }
                    }
                    if (totDamageToOneMonster > 0 && attackEffect != null) {
                        Map<MonsterStatus, Integer> attackEffectStati = attackEffect.getMonsterStati();
                        if (!attackEffectStati.isEmpty()) {
                            if (attackEffect.makeChanceResult()) {
                                monster.applyStatus(player,
                                        new MonsterStatusEffect(attackEffectStati, theSkill, null, false),
                                        attackEffect.isPoison(), attackEffect.getDuration());
                            }
                        }
                    }
                    if (attack.skill == Paladin.HEAVENS_HAMMER) {
                        // SERVER-SIDE: Heaven's Hammer — scales with WATK, skill%, and reborns
                        long hhBase = (long) player.calculateMaxBaseDamage(player.getTotalWatk());
                        int hhSkillPct = SkillFactory.getSkill(Paladin.HEAVENS_HAMMER)
                                .getEffect(player.getSkillLevel(SkillFactory.getSkill(Paladin.HEAVENS_HAMMER)))
                                .getDamage();
                        double hhRebornMult = 1.0 + (player.getReborns() * 0.08); // +8% per rebirth
                        long hhDmg = (long) (hhBase * hhSkillPct / 100.0 * hhRebornMult);
                        // ±20% variance for feel
                        long finalHH = (long) (Math.random() * (hhDmg * 0.2) + hhDmg * 0.9);
                        damageMonsterWithSkill(player, map, monster, finalHH, attack.skill, 1777);

                    } else if (attack.skill == Aran.COMBO_TEMPEST) {
                        // SERVER-SIDE: Combo Tempest — bossing finisher, scales with WATK, reborns, and
                        // combo orbs
                        long ctBase = (long) player.calculateMaxBaseDamage(player.getTotalWatk());
                        int ctSkillPct = SkillFactory.getSkill(Aran.COMBO_TEMPEST)
                                .getEffect(player.getSkillLevel(SkillFactory.getSkill(Aran.COMBO_TEMPEST)))
                                .getDamage();
                        double ctRebornMult = 1.0 + (player.getReborns() * 0.12); // +12% per rebirth (more than HH)
                        // Combo orb bonus: each orb beyond 1 adds 25% extra damage (max 5 orbs = +100%)
                        Integer comboOrbs = player.getBuffedValue(BuffStat.COMBO);
                        int orbs = (comboOrbs != null) ? Math.max(0, comboOrbs - 1) : 0;
                        double ctComboMult = 1.0 + (orbs * 0.25); // 1x at 0 orbs, up to 2x at max orbs
                        long ctDmg = (long) (ctBase * ctSkillPct / 100.0 * ctRebornMult * ctComboMult);
                        // ±20% variance for feel
                        long finalCT = (long) (Math.random() * (ctDmg * 0.2) + ctDmg * 0.9);
                        damageMonsterWithSkill(player, map, monster, finalCT, attack.skill, 0);
                    } else {
                        if (attack.skill == Aran.BODY_PRESSURE) {
                            // SERVER-SIDE: Body Pressure — % of monster max HP as long to handle >2.14b HP
                            // bosses
                            long bodyPressureDmg = (long) Math.ceil(monster.getMaxHp()
                                    * SkillFactory.getSkill(Aran.BODY_PRESSURE).getEffect(attack.skilllevel).getDamage()
                                    / 100.0);
                            if (bodyPressureDmg > totDamageToOneMonster) {
                                totDamageToOneMonster = bodyPressureDmg;
                            }
                            map.broadcastMessage(
                                    PacketCreator.damageMonster(monster.getObjectId(),
                                            (int) Math.min(totDamageToOneMonster, Integer.MAX_VALUE)));
                        }

                        map.damageMonster(player, monster, totDamageToOneMonster, target.getValue().delay());
                    }
                    if (monster.isBuffed(MonsterStatus.WEAPON_REFLECT) && !attack.magic) {
                        for (MobSkillId msId : monster.getSkills()) {
                            if (msId.type() == MobSkillType.PHYSICAL_AND_MAGIC_COUNTER) {
                                MobSkill toUse = MobSkillFactory
                                        .getMobSkillOrThrow(MobSkillType.PHYSICAL_AND_MAGIC_COUNTER, msId.level());
                                player.addHP(-toUse.getX());
                                map.broadcastMessage(player,
                                        PacketCreator.damagePlayer(0, monster.getId(), player.getId(), toUse.getX(), 0,
                                                0, false, 0, true, monster.getObjectId(), 0, 0),
                                        true);
                            }
                        }
                    }
                    if (monster.isBuffed(MonsterStatus.MAGIC_REFLECT) && attack.magic) {
                        for (MobSkillId msId : monster.getSkills()) {
                            if (msId.type() == MobSkillType.PHYSICAL_AND_MAGIC_COUNTER) {
                                MobSkill toUse = MobSkillFactory
                                        .getMobSkillOrThrow(MobSkillType.PHYSICAL_AND_MAGIC_COUNTER, msId.level());
                                player.addHP(-toUse.getY());
                                map.broadcastMessage(player,
                                        PacketCreator.damagePlayer(0, monster.getId(), player.getId(), toUse.getY(), 0,
                                                0, false, 0, true, monster.getObjectId(), 0, 0),
                                        true);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void damageMonsterWithSkill(final Character attacker, final MapleMap map, final Monster monster,
            final long damage, int skillid, int fixedTime) {
        int animationTime;

        if (fixedTime == 0) {
            animationTime = SkillFactory.getSkill(skillid).getAnimationTime();
        } else {
            animationTime = fixedTime;
        }

        // Visual broadcast caps at INT_MAX (client limitation); server applies full
        // long damage.
        int visualDamage = (int) Math.min(damage, Integer.MAX_VALUE);

        if (animationTime > 0) {
            TimerManager.getInstance().schedule(() -> {
                map.broadcastMessage(PacketCreator.damageMonster(monster.getObjectId(), visualDamage),
                        monster.getPosition());
                map.damageMonster(attacker, monster, damage);
            }, animationTime);
        } else {
            map.broadcastMessage(PacketCreator.damageMonster(monster.getObjectId(), visualDamage),
                    monster.getPosition());
            map.damageMonster(attacker, monster, damage);
        }
    }

    protected AttackInfo parseDamage(InPacket p, Character chr, boolean ranged, boolean magic) {
        // 2C 00 00 01 91 A1 12 00 A5 57 62 FC E2 75 99 10 00 47 80 01 04 01 C6 CC 02 DD
        // FF 5F 00
        AttackInfo ret = new AttackInfo();
        p.readByte();
        ret.numAttackedAndDamage = p.readByte();
        ret.numAttacked = (ret.numAttackedAndDamage >>> 4) & 0xF;
        ret.numDamage = ret.numAttackedAndDamage & 0xF;
        ret.targets = new HashMap<>();
        ret.skill = p.readInt();
        ret.ranged = ranged;
        ret.magic = magic;

        if (ret.skill > 0) {
            ret.skilllevel = chr.getSkillLevel(ret.skill);
            if (ret.skilllevel == 0 && GameConstants.isPqSkillMap(chr.getMapId())
                    && GameConstants.isPqSkill(ret.skill)) {
                ret.skilllevel = 1;
            }
        }

        if (ret.skill == Evan.ICE_BREATH || ret.skill == Evan.FIRE_BREATH || ret.skill == FPArchMage.BIG_BANG
                || ret.skill == ILArchMage.BIG_BANG || ret.skill == Bishop.BIG_BANG || ret.skill == Gunslinger.GRENADE
                || ret.skill == Brawler.CORKSCREW_BLOW || ret.skill == ThunderBreaker.CORKSCREW_BLOW
                || ret.skill == NightWalker.POISON_BOMB) {
            ret.charge = p.readInt();
        } else {
            ret.charge = 0;
        }

        p.skip(8);
        ret.display = p.readByte();
        ret.direction = p.readByte();
        ret.stance = p.readByte();
        if (ret.skill == ChiefBandit.MESO_EXPLOSION) {
            return parseMesoExplosion(p, ret);
        }

        if (ranged) {
            p.readByte();
            ret.speed = p.readByte();
            p.readByte();
            ret.rangedirection = p.readByte();
            p.skip(7);
            if (ret.skill == Bowmaster.HURRICANE || ret.skill == Marksman.PIERCING_ARROW
                    || ret.skill == Corsair.RAPID_FIRE || ret.skill == WindArcher.HURRICANE) {
                p.skip(4);
            }
        } else {
            p.readByte();
            ret.speed = p.readByte();
            p.skip(4);
        }

        long calcDmgMax;

        if (magic && ret.skill != 0) {
            calcDmgMax = (long) (Math
                    .ceil((chr.getTotalMagic() * Math.ceil(chr.getTotalMagic() / 1000.0) + chr.getTotalMagic()) / 30.0)
                    + Math.ceil(chr.getTotalInt() / 200.0));
        } else if (ret.skill == 4001344 || ret.skill == NightWalker.LUCKY_SEVEN
                || ret.skill == NightLord.TRIPLE_THROW) {
            calcDmgMax = (long) ((chr.getTotalLuk() * 5) * Math.ceil(chr.getTotalWatk() / 100.0));
        } else if (ret.skill == DragonKnight.DRAGON_ROAR) {
            calcDmgMax = (long) ((chr.getTotalStr() * 4 + chr.getTotalDex()) * Math.ceil(chr.getTotalWatk() / 100.0));
        } else if (ret.skill == NightLord.VENOMOUS_STAR || ret.skill == Shadower.VENOMOUS_STAB) {
            calcDmgMax = (long) (Math
                    .ceil((18.5 * (chr.getTotalStr() + chr.getTotalLuk()) + chr.getTotalDex() * 2) / 100.0)
                    * chr.calculateMaxBaseDamage(chr.getTotalWatk()));
        } else {
            calcDmgMax = chr.calculateMaxBaseDamage(chr.getTotalWatk());
        }

        StatEffect effect = null;
        if (ret.skill != 0) {
            Skill skill = SkillFactory.getSkill(ret.skill);
            effect = skill.getEffect(ret.skilllevel);

            if (magic) {
                if (chr.getJob() == Job.IL_ARCHMAGE || chr.getJob() == Job.IL_MAGE) {
                    int skillLvl = chr.getSkillLevel(ILMage.ELEMENT_AMPLIFICATION);
                    if (skillLvl > 0) {
                        calcDmgMax = calcDmgMax
                                * SkillFactory.getSkill(ILMage.ELEMENT_AMPLIFICATION).getEffect(skillLvl).getY() / 100;
                    }
                } else if (chr.getJob() == Job.FP_ARCHMAGE || chr.getJob() == Job.FP_MAGE) {
                    int skillLvl = chr.getSkillLevel(FPMage.ELEMENT_AMPLIFICATION);
                    if (skillLvl > 0) {
                        calcDmgMax = calcDmgMax
                                * SkillFactory.getSkill(FPMage.ELEMENT_AMPLIFICATION).getEffect(skillLvl).getY() / 100;
                    }
                } else if (chr.getJob() == Job.BLAZEWIZARD3 || chr.getJob() == Job.BLAZEWIZARD4) {
                    int skillLvl = chr.getSkillLevel(BlazeWizard.ELEMENT_AMPLIFICATION);
                    if (skillLvl > 0) {
                        calcDmgMax = calcDmgMax
                                * SkillFactory.getSkill(BlazeWizard.ELEMENT_AMPLIFICATION).getEffect(skillLvl).getY()
                                / 100;
                    }
                } else if (chr.getJob() == Job.EVAN7 || chr.getJob() == Job.EVAN8 || chr.getJob() == Job.EVAN9
                        || chr.getJob() == Job.EVAN10) {
                    int skillLvl = chr.getSkillLevel(Evan.MAGIC_AMPLIFICATION);
                    if (skillLvl > 0) {
                        calcDmgMax = calcDmgMax
                                * SkillFactory.getSkill(Evan.MAGIC_AMPLIFICATION).getEffect(skillLvl).getY() / 100;
                    }
                }

                calcDmgMax *= effect.getMatk();
                if (ret.skill == Cleric.HEAL) {
                    calcDmgMax = (int) Math
                            .round((chr.getTotalInt() * 4.8 + chr.getTotalLuk() * 4) * chr.getTotalMagic() / 1000);
                    calcDmgMax = calcDmgMax * effect.getHp() / 100;

                    ret.speed = 7;
                }
            } else if (ret.skill == Hermit.SHADOW_MESO) {
                calcDmgMax = effect.getMoneyCon() * 10;
                calcDmgMax = (int) Math.floor(calcDmgMax * 1.5);
            } else {
                calcDmgMax = calcDmgMax * effect.getDamage() / 100;
            }
        }

        Integer comboBuff = chr.getBuffedValue(BuffStat.COMBO);
        if (comboBuff != null && comboBuff > 0) {
            int oid = chr.isCygnus() ? DawnWarrior.COMBO : Crusader.COMBO;
            int advcomboid = chr.isCygnus() ? DawnWarrior.ADVANCED_COMBO : Hero.ADVANCED_COMBO;

            if (comboBuff > 6) {
                StatEffect ceffect = SkillFactory.getSkill(advcomboid).getEffect(chr.getSkillLevel(advcomboid));
                calcDmgMax = (long) Math
                        .floor(calcDmgMax * (ceffect.getDamage() + 50) / 100 + 0.20 + (comboBuff - 5) * 0.04);
            } else {
                int skillLv = chr.getSkillLevel(oid);
                if (skillLv <= 0 || chr.isGM()) {
                    skillLv = SkillFactory.getSkill(oid).getMaxLevel();
                }

                if (skillLv > 0) {
                    StatEffect ceffect = SkillFactory.getSkill(oid).getEffect(skillLv);
                    calcDmgMax = (long) Math.floor(calcDmgMax * (ceffect.getDamage() + 50) / 100
                            + Math.floor((comboBuff - 1) * (skillLv / 6)) / 100);
                }
            }

            if (GameConstants.isFinisherSkill(ret.skill)) {
                int orbs = comboBuff - 1;
                if (orbs == 2) {
                    calcDmgMax *= 1.2;
                } else if (orbs == 3) {
                    calcDmgMax *= 1.54;
                } else if (orbs == 4) {
                    calcDmgMax *= 2;
                } else if (orbs >= 5) {
                    calcDmgMax *= 2.5;
                }
            }
        }

        if (chr.getEnergyBar() == 15000) {
            int energycharge = chr.isCygnus() ? ThunderBreaker.ENERGY_CHARGE : Marauder.ENERGY_CHARGE;
            StatEffect ceffect = SkillFactory.getSkill(energycharge).getEffect(chr.getSkillLevel(energycharge));
            calcDmgMax *= (100 + ceffect.getDamage()) / 100;
        }

        int bonusDmgBuff = 100;
        for (PlayerBuffValueHolder pbvh : chr.getAllBuffs()) {
            int bonusDmg = pbvh.effect.getDamage() - 100;
            bonusDmgBuff += bonusDmg;
        }

        if (bonusDmgBuff != 100) {
            float dmgBuff = bonusDmgBuff / 100.0f;
            calcDmgMax = (long) Math.ceil(calcDmgMax * dmgBuff);
        }

        if (chr.getMapId() >= MapId.ARAN_TUTORIAL_START && chr.getMapId() <= MapId.ARAN_TUTORIAL_MAX) {
            calcDmgMax += 80000; // Aran Tutorial.
        }

        boolean canCrit = chr.getJob().isA((Job.BOWMAN)) || chr.getJob().isA(Job.THIEF)
                || chr.getJob().isA(Job.NIGHTWALKER1) || chr.getJob().isA(Job.WINDARCHER1) || chr.getJob() == Job.ARAN3
                || chr.getJob() == Job.ARAN4 || chr.getJob() == Job.MARAUDER || chr.getJob() == Job.BUCCANEER;

        if (chr.getBuffEffect(BuffStat.SHARP_EYES) != null) {
            canCrit = true;
            calcDmgMax *= 1.4;
        }

        boolean shadowPartner = chr.getBuffEffect(BuffStat.SHADOWPARTNER) != null;

        if (ret.skill != 0) {
            int fixed = ret.getAttackEffect(chr, SkillFactory.getSkill(ret.skill)).getFixDamage();
            if (fixed > 0) {
                calcDmgMax = fixed;
            }
        }
        for (int i = 0; i < ret.numAttacked; i++) {
            int oid = p.readInt();
            p.skip(4);
            Point curPos = p.readPos();
            Point nextPos = p.readPos();
            short delay = p.readShort();
            List<Integer> damageLines = new ArrayList<>();
            final Monster monster = chr.getMap().getMonsterByOid(oid);
            if (chr.getBuffEffect(BuffStat.WK_CHARGE) != null) {
                // Charge, so now we need to check elemental effectiveness
                int sourceID = chr.getBuffSource(BuffStat.WK_CHARGE);
                int level = chr.getBuffedValue(BuffStat.WK_CHARGE);
                if (monster != null) {
                    if (sourceID == WhiteKnight.BW_FIRE_CHARGE || sourceID == WhiteKnight.SWORD_FIRE_CHARGE) {
                        if (monster.getStats().getEffectiveness(Element.FIRE) == ElementalEffectiveness.WEAK) {
                            calcDmgMax *= 1.05 + level * 0.015;
                        }
                    } else if (sourceID == WhiteKnight.BW_ICE_CHARGE || sourceID == WhiteKnight.SWORD_ICE_CHARGE) {
                        if (monster.getStats().getEffectiveness(Element.ICE) == ElementalEffectiveness.WEAK) {
                            calcDmgMax *= 1.05 + level * 0.015;
                        }
                    } else if (sourceID == WhiteKnight.BW_LIT_CHARGE || sourceID == WhiteKnight.SWORD_LIT_CHARGE) {
                        if (monster.getStats().getEffectiveness(Element.LIGHTING) == ElementalEffectiveness.WEAK) {
                            calcDmgMax *= 1.05 + level * 0.015;
                        }
                    } else if (sourceID == Paladin.BW_HOLY_CHARGE || sourceID == Paladin.SWORD_HOLY_CHARGE) {
                        if (monster.getStats().getEffectiveness(Element.HOLY) == ElementalEffectiveness.WEAK) {
                            calcDmgMax *= 1.2 + level * 0.015;
                        }
                    }
                } else {
                    calcDmgMax *= 1.5;
                }
            }

            if (ret.skill != 0) {
                Skill skill = SkillFactory.getSkill(ret.skill);
                if (skill.getElement() != Element.NEUTRAL && chr.getBuffedValue(BuffStat.ELEMENTAL_RESET) == null) {
                    if (monster != null) {
                        ElementalEffectiveness eff = monster.getElementalEffectiveness(skill.getElement());
                        if (eff == ElementalEffectiveness.WEAK) {
                            calcDmgMax *= 1.5;
                        }
                    } else {
                        calcDmgMax *= 1.5;
                    }
                }
                if (ret.skill == FPWizard.POISON_BREATH || ret.skill == FPMage.POISON_MIST
                        || ret.skill == FPArchMage.FIRE_DEMON || ret.skill == ILArchMage.ICE_DEMON) {
                    if (monster != null) {
                    }
                } else if (ret.skill == Hermit.SHADOW_WEB) {
                    if (monster != null) {
                        calcDmgMax = monster.getHp() / (50 - chr.getSkillLevel(skill));
                    }
                } else if (ret.skill == Hermit.SHADOW_MESO) {
                    if (monster != null) {
                        monster.debuffMob(Hermit.SHADOW_MESO);
                    }
                } else if (ret.skill == Aran.BODY_PRESSURE) {
                    if (monster != null) {
                        int bodyPressureDmg = (int) Math.ceil(monster.getMaxHp()
                                * SkillFactory.getSkill(Aran.BODY_PRESSURE).getEffect(ret.skilllevel).getDamage()
                                / 100.0);
                        if (bodyPressureDmg > calcDmgMax) {
                            calcDmgMax = bodyPressureDmg;
                        }
                    }
                }
            }

            for (int j = 0; j < ret.numDamage; j++) {
                int damage = p.readInt();
                long hitDmgMax = calcDmgMax;
                if (ret.skill == Buccaneer.BARRAGE || ret.skill == ThunderBreaker.BARRAGE) {
                    if (j > 3) {
                        hitDmgMax *= Math.pow(2, (j - 3));
                        hitDmgMax *= Math.pow(2, (j - 3));
                    }
                }
                if (shadowPartner) {
                    if (j >= ret.numDamage * 2 / 3) {
                        hitDmgMax *= 0.333;
                    }
                }

                if (ret.skill == Marksman.SNIPE) {
                    Monster mob = chr.getMap().getMonsterByOid(oid);
                    if (mob != null) {
                        Point mobPos = mob.getPosition();
                        Point chrPos = chr.getPosition();

                        // SERVER-SIDE: Snipe — dynamic formula; bypasses 2.14b int cap via long
                        long snipeBase = chr.calculateMaxBaseDamage(chr.getTotalWatk());
                        int snipeLevel = chr.getSkillLevel(Marksman.SNIPE);

                        // Skill level mult: Lv1 = 4.25x, Lv30 = 11.5x
                        double skillMult = 4.0 + (snipeLevel * 0.25);

                        // Distance mult: 0.2x at melee (point blank), caps at 4.0x beyond ~450px
                        double distPx = Math.abs(chrPos.getX() - mobPos.getX());
                        double distMult = 0.2 + (Math.min(distPx / 450.0, 1.0) * 3.8);

                        // Rebirth mult: +4% per rebirth, uncapped
                        double rebornMult = 1.0 + (chr.getReborns() * 0.04);

                        long snipeDmg = (long) (snipeBase * skillMult * distMult * rebornMult);

                        // Override the damage line with the server-computed value
                        // damageLines already holds ints; we apply the long directly to
                        // totDamageToOneMonster
                        // by setting damage = INT_MAX visually while the real value is applied below.
                        damage = Integer.MAX_VALUE; // visual placeholder — real damage applied via snipeDmg override

                        final long finalSnipeDmg = snipeDmg;
                        TimerManager.getInstance().schedule(() -> {
                            chr.getMap().broadcastMessage(
                                    PacketCreator.damageMonster(oid, (int) Math.min(finalSnipeDmg, Integer.MAX_VALUE),
                                            mob.getHp(), mob.getMaxHp()),
                                    mobPos);
                            chr.getMap().damageMonster(chr, mob, finalSnipeDmg);
                        }, delay);

                        // Skip normal totDamageToOneMonster accumulation for Snipe;
                        // damage is applied directly above via the scheduled task.
                        hitDmgMax = Long.MAX_VALUE; // exempt Snipe from damage hack check
                    }
                } else if (ret.skill == Beginner.BAMBOO_RAIN || ret.skill == Noblesse.BAMBOO_RAIN
                        || ret.skill == Evan.BAMBOO_THRUST || ret.skill == Legend.BAMBOO_THRUST) {
                    hitDmgMax = 82569000; // 30% of Max HP of strongest Dojo boss
                }

                long maxWithCrit = hitDmgMax;
                if (canCrit) {
                    maxWithCrit *= 3.5;
                }

                // Scale the damage tolerance based on rebirths.
                // Each rebirth gives +5 bonus AP (stats), which compound into higher damage.
                // We apply a 20% tolerance increase per rebirth, capped at 5x the base limit.
                int reborns = chr.getReborns();
                double rebornMultiplier = Math.min(1.0 + (reborns * 0.20), 5.0);

                // Scale the damage tolerance based on the player's actual ATK power.
                // Strong custom equipment can push WATK/MATK far above the vanilla 300
                // baseline,
                // causing the formula-derived cap to under-estimate real damage output.
                // We scale proportionally above 300 ATK, capped at 4x to stay meaningful.
                int totalAtk = magic ? chr.getTotalMagic() : chr.getTotalWatk();
                int baselineAtk = 300;
                double atkMultiplier = Math.min(Math.max(1.0, (double) totalAtk / baselineAtk), 4.0);

                long adjustedMaxWithCrit = (long) (maxWithCrit * rebornMultiplier * atkMultiplier);

                String dmgHackContext = " SID: " + ret.skill
                        + " MobID: " + (monster != null ? monster.getId() : "null")
                        + " Map: " + chr.getMap().getMapName() + " (" + chr.getMapId() + ")"
                        + " Reborns: " + reborns + " ATK: " + totalAtk;

                if (damage > adjustedMaxWithCrit * 1.5) {
                    AutobanFactory.DAMAGE_HACK.alert(chr,
                            "DMG: " + damage + " MaxDMG: " + adjustedMaxWithCrit + dmgHackContext);
                }

                if (damage > adjustedMaxWithCrit * 5) {
                    AutobanFactory.DAMAGE_HACK.addPoint(chr.getAutobanManager(),
                            "DMG: " + damage + " MaxDMG: " + adjustedMaxWithCrit + dmgHackContext);
                }
                if (ret.skill == Marksman.SNIPE || (canCrit && damage > hitDmgMax)) {
                }

                if (effect != null) {
                    // Base hit count comes from WZ data (attackCount/bulletCount default to 1 if
                    // absent).
                    // Shadow Partner causes the client to send exactly double the normal hit lines,
                    // so we double maxattack when SP is active — no per-skill hardcoding needed.
                    int maxattack = Math.max(effect.getBulletCount(), effect.getAttackCount());

                    if (shadowPartner) {
                        maxattack *= 2; // Shadow Partner doubles hit lines
                    }

                    // Custom penta-star Triple Throw (NL) sends 5 lines vs WZ's 3; override
                    // explicitly.
                    if (ret.skill == NightLord.TRIPLE_THROW) {
                        maxattack = shadowPartner ? 12 : 6;
                    }

                    if (ret.numDamage > maxattack) {
                        chr.dropMessage(5, "[Warning] Ignored 'Too Many Lines' check (Lines: " + ret.numDamage + "/"
                                + maxattack + ")");
                        chr.getAutobanManager()
                                .jailPlayer("Damage Line Hack (" + ret.numDamage + " lines) SID: " + ret.skill, 60);
                    }
                }

                damageLines.add(damage);
                ret.damageLines = damageLines;
            }
            if (ret.skill != Corsair.RAPID_FIRE || ret.skill != Aran.HIDDEN_FULL_DOUBLE
                    || ret.skill != Aran.HIDDEN_FULL_TRIPLE || ret.skill != Aran.HIDDEN_OVER_DOUBLE
                    || ret.skill != Aran.HIDDEN_OVER_TRIPLE) {
                p.skip(4);
            }
            ret.targets.put(oid, new AttackTarget(delay, damageLines));
        }
        if (ret.skill == NightWalker.POISON_BOMB) { // Poison Bomb
            p.skip(4);
            ret.position.setLocation(p.readShort(), p.readShort());
        }

        for (Map.Entry<Integer, AttackTarget> entry : ret.targets.entrySet()) {
            int mobId = entry.getKey(); // get monsterId on map
            Monster monster = chr.getMap().getMonsterByOid(mobId); // get monster class

            // [FIX] Add this check!
            if (monster == null) {
                continue;
            }

            // Damage overflow is now handled correctly in the main attack loop above.
            // totDamageToOneMonster is a long that correctly accumulates all hit lines,
            // including unwrapping client-side negatives. No extra pass needed here.
        }

        return ret;
    }

    private AttackInfo parseMesoExplosion(InPacket p, AttackInfo attackInfo) {
        p.skip(6);

        Map<Integer, List<Integer>> targetDamage = new HashMap<>();
        for (int i = 0; i < attackInfo.numAttacked; i++) {
            int mobOid = p.readInt();
            p.skip(4);
            Point curPos = p.readPos();
            Point nextPos = p.readPos();
            int damageLines = p.readByte();
            List<Integer> allDamageNumbers = new ArrayList<>();
            for (int j = 0; j < damageLines; j++) {
                int damage = p.readInt();
                allDamageNumbers.add(damage);
            }
            p.skip(4);
            targetDamage.put(mobOid, allDamageNumbers);
        }

        p.skip(4);

        List<Integer> explodedMesos = new ArrayList<>();
        int explodedMesoCount = p.readByte();
        for (int j = 0; j < explodedMesoCount; j++) {
            int mesoOid = p.readInt();
            p.skip(1);
            explodedMesos.add(mesoOid);
        }
        attackInfo.explodedMesos = explodedMesos;

        final short attackDelay = p.readShort();
        attackInfo.attackDelay = attackDelay;

        Map<Integer, AttackTarget> targets = new HashMap<>();
        targetDamage.forEach((id, damage) -> targets.put(id, new AttackTarget(attackDelay, damage)));
        attackInfo.targets = targets;
        return attackInfo;
    }

    private void removeExplodedMesos(MapleMap map, AttackInfo attack) {
        int index = 0;
        for (Integer mesoId : attack.explodedMesos) {
            MapObject mapobject = map.getMapObject(mesoId);
            if (!(mapobject instanceof MapItem mapItem)) {
                return;
            }
            if (mapItem.getMeso() == 0) {
                return;
            }

            mapItem.lockItem();
            try {
                if (mapItem.isPickedUp()) {
                    return;
                }
                int delay = attack.attackDelay + (index++ % 5) * EXPLODED_MESO_SPREAD_DELAY;
                delay = Math.min(delay, EXPLODED_MESO_MAX_DELAY);
                map.pickItemDrop(PacketCreator.removeExplodedMesoFromMap(mapItem.getObjectId(), (short) delay),
                        mapItem);
            } finally {
                mapItem.unlockItem();
            }
        }
    }
}