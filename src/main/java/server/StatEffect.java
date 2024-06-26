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
package server;

import client.BuffStat;
import client.Character;
import client.Disease;
import client.Job;
import client.Mount;
import client.Skill;
import client.SkillFactory;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.manipulator.InventoryManipulator;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import config.YamlConfig;
import constants.id.ItemId;
import constants.id.MapId;
import constants.inventory.ItemConstants;
import constants.skills.Aran;
import constants.skills.Assassin;
import constants.skills.Bandit;
import constants.skills.Beginner;
import constants.skills.Bishop;
import constants.skills.BlazeWizard;
import constants.skills.Bowmaster;
import constants.skills.Brawler;
import constants.skills.Buccaneer;
import constants.skills.ChiefBandit;
import constants.skills.Cleric;
import constants.skills.Corsair;
import constants.skills.Crossbowman;
import constants.skills.Crusader;
import constants.skills.DarkKnight;
import constants.skills.DawnWarrior;
import constants.skills.DragonKnight;
import constants.skills.Evan;
import constants.skills.FPArchMage;
import constants.skills.FPMage;
import constants.skills.FPWizard;
import constants.skills.Fighter;
import constants.skills.GM;
import constants.skills.Gunslinger;
import constants.skills.Hermit;
import constants.skills.Hero;
import constants.skills.Hunter;
import constants.skills.ILArchMage;
import constants.skills.ILMage;
import constants.skills.ILWizard;
import constants.skills.Legend;
import constants.skills.Magician;
import constants.skills.Marauder;
import constants.skills.Marksman;
import constants.skills.NightLord;
import constants.skills.NightWalker;
import constants.skills.Noblesse;
import constants.skills.Outlaw;
import constants.skills.Page;
import constants.skills.Paladin;
import constants.skills.Pirate;
import constants.skills.Priest;
import constants.skills.Ranger;
import constants.skills.Rogue;
import constants.skills.Shadower;
import constants.skills.Sniper;
import constants.skills.Spearman;
import constants.skills.SuperGM;
import constants.skills.ThunderBreaker;
import constants.skills.WhiteKnight;
import constants.skills.WindArcher;
import net.packet.Packet;
import net.server.Server;
import net.server.world.Party;
import net.server.world.PartyCharacter;
import provider.Data;
import provider.DataTool;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.life.MobSkillType;
import server.life.Monster;
import server.maps.Door;
import server.maps.FieldLimit;
import server.maps.MapObject;
import server.maps.MapObjectType;
import server.maps.MapleMap;
import server.maps.Mist;
import server.maps.Portal;
import server.maps.Summon;
import server.maps.SummonMovementType;
import server.partyquest.CarnivalFactory;
import server.partyquest.CarnivalFactory.MCSkill;
import tools.PacketCreator;
import tools.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @author Matze
 * @author Frz
 * @author Ronan
 */
public class StatEffect {
    private short watk, matk, wdef, mdef, acc, avoid, speed, jump;
    private short hp, mp;
    private double hpR, mpR;
    private short mhpRRate, mmpRRate, mobSkill, mobSkillLevel;
    private byte mhpR, mmpR;
    private short mpCon, hpCon;
    private int duration, target, barrier, mob;
    private boolean overTime, repeatEffect;
    private int sourceid;
    private int moveTo;
    private int cp, nuffSkill;
    private List<Disease> cureDebuffs;
    private boolean skill;
    private List<Pair<BuffStat, Integer>> statups;
    private Map<MonsterStatus, Integer> monsterStatus;
    private int x, y, mobCount, moneyCon, cooldown, morphId = 0, ghost, fatigue, berserk, booster;
    private double prop;
    private int itemCon, itemConNo;
    private int damage, attackCount, fixdamage;
    private Point lt, rb;
    private short bulletCount, bulletConsume;
    private byte mapProtection;
    private CardItemupStats cardStats;

    private static class CardItemupStats {
        protected int itemCode, prob;
        protected boolean party;
        private final List<Pair<Integer, Integer>> areas;

        private CardItemupStats(int code, int prob, List<Pair<Integer, Integer>> areas, boolean inParty) {
            this.itemCode = code;
            this.prob = prob;
            this.areas = areas;
            this.party = inParty;
        }

        private boolean isInArea(int mapid) {
            if (this.areas == null) {
                return true;
            }

            for (Pair<Integer, Integer> a : this.areas) {
                if (mapid >= a.left && mapid <= a.right) {
                    return true;
                }
            }

            return false;
        }
    }

    private boolean isEffectActive(int mapid, boolean partyHunting) {
        if (cardStats == null) {
            return true;
        }

        if (!cardStats.isInArea(mapid)) {
            return false;
        }

        return !cardStats.party || partyHunting;
    }

    public boolean isActive(Character applyto) {
        return isEffectActive(applyto.getMapId(), applyto.getPartyMembersOnSameMap().size() > 1);
    }

    public int getCardRate(int mapid, int itemid) {
        if (cardStats != null) {
            if (cardStats.itemCode == Integer.MAX_VALUE) {
                return cardStats.prob;
            } else if (cardStats.itemCode < 1000) {
                if (itemid / 10000 == cardStats.itemCode) {
                    return cardStats.prob;
                }
            } else {
                if (itemid == cardStats.itemCode) {
                    return cardStats.prob;
                }
            }
        }

        return 0;
    }

    public static StatEffect loadSkillEffectFromData(Data source, int skillid, boolean overtime) {
        return loadFromData(source, skillid, true, overtime);
    }

    public static StatEffect loadItemEffectFromData(Data source, int itemid) {
        return loadFromData(source, itemid, false, false);
    }

    private static void addBuffStatPairToListIfNotZero(List<Pair<BuffStat, Integer>> list, BuffStat buffstat, Integer val) {
        if (val != 0) {
            list.add(new Pair<>(buffstat, val));
        }
    }

    private static byte mapProtection(int sourceid) {
        if (sourceid == ItemId.RED_BEAN_PORRIDGE || sourceid == ItemId.SOFT_WHITE_BUN) {
            return 1;   //elnath cold
        } else if (sourceid == ItemId.AIR_BUBBLE) {
            return 2;   //aqua road underwater
        } else {
            return 0;
        }
    }

    private static StatEffect loadFromData(Data source, int sourceid, boolean skill, boolean overTime) {
        StatEffect ret = new StatEffect();
        ret.duration = DataTool.getIntConvert("time", source, -1);
        ret.hp = (short) DataTool.getInt("hp", source, 0);
        ret.hpR = DataTool.getInt("hpR", source, 0) / 100.0;
        ret.mp = (short) DataTool.getInt("mp", source, 0);
        ret.mpR = DataTool.getInt("mpR", source, 0) / 100.0;
        ret.mpCon = (short) DataTool.getInt("mpCon", source, 0);
        ret.hpCon = (short) DataTool.getInt("hpCon", source, 0);
        int iprop = DataTool.getInt("prop", source, 100);
        ret.prop = iprop / 100.0;

        ret.cp = DataTool.getInt("cp", source, 0);
        List<Disease> cure = new ArrayList<>(5);
        if (DataTool.getInt("poison", source, 0) > 0) {
            cure.add(Disease.POISON);
        }
        if (DataTool.getInt("seal", source, 0) > 0) {
            cure.add(Disease.SEAL);
        }
        if (DataTool.getInt("darkness", source, 0) > 0) {
            cure.add(Disease.DARKNESS);
        }
        if (DataTool.getInt("weakness", source, 0) > 0) {
            cure.add(Disease.WEAKEN);
            cure.add(Disease.SLOW);
        }
        if (DataTool.getInt("curse", source, 0) > 0) {
            cure.add(Disease.CURSE);
        }
        ret.cureDebuffs = cure;
        ret.nuffSkill = DataTool.getInt("nuffSkill", source, 0);

        ret.mobCount = DataTool.getInt("mobCount", source, 1);
        ret.cooldown = DataTool.getInt("cooltime", source, 0);
        ret.morphId = DataTool.getInt("morph", source, 0);
        ret.ghost = DataTool.getInt("ghost", source, 0);
        ret.fatigue = DataTool.getInt("incFatigue", source, 0);
        ret.repeatEffect = DataTool.getInt("repeatEffect", source, 0) > 0;

        Data mdd = source.getChildByPath("0");
        if (mdd != null && mdd.getChildren().size() > 0) {
            ret.mobSkill = (short) DataTool.getInt("mobSkill", mdd, 0);
            ret.mobSkillLevel = (short) DataTool.getInt("level", mdd, 0);
            ret.target = DataTool.getInt("target", mdd, 0);
        } else {
            ret.mobSkill = 0;
            ret.mobSkillLevel = 0;
            ret.target = 0;
        }

        Data mdds = source.getChildByPath("mob");
        if (mdds != null) {
            if (mdds.getChildren() != null && mdds.getChildren().size() > 0) {
                ret.mob = DataTool.getInt("mob", mdds, 0);
            }
        }
        ret.sourceid = sourceid;
        ret.skill = skill;
        if (!ret.skill && ret.duration > -1) {
            ret.overTime = true;
        } else {
            ret.duration *= 1000; // items have their times stored in ms, of course
            ret.overTime = overTime;
        }

        ArrayList<Pair<BuffStat, Integer>> statups = new ArrayList<>();
        ret.watk = (short) DataTool.getInt("pad", source, 0);
        ret.wdef = (short) DataTool.getInt("pdd", source, 0);
        ret.matk = (short) DataTool.getInt("mad", source, 0);
        ret.mdef = (short) DataTool.getInt("mdd", source, 0);
        ret.acc = (short) DataTool.getIntConvert("acc", source, 0);
        ret.avoid = (short) DataTool.getInt("eva", source, 0);

        ret.speed = (short) DataTool.getInt("speed", source, 0);
        ret.jump = (short) DataTool.getInt("jump", source, 0);

        ret.barrier = DataTool.getInt("barrier", source, 0);
        addBuffStatPairToListIfNotZero(statups, BuffStat.AURA, ret.barrier);

        ret.mapProtection = mapProtection(sourceid);
        addBuffStatPairToListIfNotZero(statups, BuffStat.MAP_PROTECTION, (int) ret.mapProtection);

        if (ret.overTime && ret.getSummonMovementType() == null) {
            if (!skill) {
                if (ItemId.isPyramidBuff(sourceid)) {
                    ret.berserk = DataTool.getInt("berserk", source, 0);
                    ret.booster = DataTool.getInt("booster", source, 0);

                    // TODO: For some reason adding the BERSERK buff stat makes the user attack VERY slowly. It also doesn't increase
                    //  the damage output like it should either..
                    addBuffStatPairToListIfNotZero(statups, BuffStat.BERSERK, ret.berserk);
                    addBuffStatPairToListIfNotZero(statups, BuffStat.BOOSTER, ret.booster);

                } else if (ItemId.isDojoBuff(sourceid) || isHpMpRecovery(sourceid)) {
                    ret.mhpR = (byte) DataTool.getInt("mhpR", source, 0);
                    ret.mhpRRate = (short) (DataTool.getInt("mhpRRate", source, 0) * 100);
                    ret.mmpR = (byte) DataTool.getInt("mmpR", source, 0);
                    ret.mmpRRate = (short) (DataTool.getInt("mmpRRate", source, 0) * 100);

                    addBuffStatPairToListIfNotZero(statups, BuffStat.HPREC, (int) ret.mhpR);
                    addBuffStatPairToListIfNotZero(statups, BuffStat.MPREC, (int) ret.mmpR);

                } else if (ItemId.isRateCoupon(sourceid)) {
                    switch (DataTool.getInt("expR", source, 0)) {
                        case 1:
                            addBuffStatPairToListIfNotZero(statups, BuffStat.COUPON_EXP1, 1);
                            break;

                        case 2:
                            addBuffStatPairToListIfNotZero(statups, BuffStat.COUPON_EXP2, 1);
                            break;

                        case 3:
                            addBuffStatPairToListIfNotZero(statups, BuffStat.COUPON_EXP3, 1);
                            break;

                        case 4:
                            addBuffStatPairToListIfNotZero(statups, BuffStat.COUPON_EXP4, 1);
                            break;
                    }

                    switch (DataTool.getInt("drpR", source, 0)) {
                        case 1:
                            addBuffStatPairToListIfNotZero(statups, BuffStat.COUPON_DRP1, 1);
                            break;

                        case 2:
                            addBuffStatPairToListIfNotZero(statups, BuffStat.COUPON_DRP2, 1);
                            break;

                        case 3:
                            addBuffStatPairToListIfNotZero(statups, BuffStat.COUPON_DRP3, 1);
                            break;
                    }
                } else if (ItemId.isMonsterCard(sourceid)) {
                    int prob = 0, itemupCode = Integer.MAX_VALUE;
                    List<Pair<Integer, Integer>> areas = null;
                    boolean inParty = false;

                    Data con = source.getChildByPath("con");
                    if (con != null) {
                        areas = new ArrayList<>(3);

                        for (Data conData : con.getChildren()) {
                            int type = DataTool.getInt("type", conData, -1);

                            if (type == 0) {
                                int startMap = DataTool.getInt("sMap", conData, 0);
                                int endMap = DataTool.getInt("eMap", conData, 0);

                                areas.add(new Pair<>(startMap, endMap));
                            } else if (type == 2) {
                                inParty = true;
                            }
                        }

                        if (areas.isEmpty()) {
                            areas = null;
                        }
                    }

                    if (DataTool.getInt("mesoupbyitem", source, 0) != 0) {
                        addBuffStatPairToListIfNotZero(statups, BuffStat.MESO_UP_BY_ITEM, 4);
                        prob = DataTool.getInt("prob", source, 1);
                    }

                    int itemupType = DataTool.getInt("itemupbyitem", source, 0);
                    if (itemupType != 0) {
                        addBuffStatPairToListIfNotZero(statups, BuffStat.ITEM_UP_BY_ITEM, 4);
                        prob = DataTool.getInt("prob", source, 1);

                        switch (itemupType) {
                            case 2:
                                itemupCode = DataTool.getInt("itemCode", source, 1);
                                break;

                            case 3:
                                itemupCode = DataTool.getInt("itemRange", source, 1);    // 3 digits
                                break;
                        }
                    }

                    if (DataTool.getInt("respectPimmune", source, 0) != 0) {
                        addBuffStatPairToListIfNotZero(statups, BuffStat.RESPECT_PIMMUNE, 4);
                    }

                    if (DataTool.getInt("respectMimmune", source, 0) != 0) {
                        addBuffStatPairToListIfNotZero(statups, BuffStat.RESPECT_MIMMUNE, 4);
                    }

                    if (DataTool.getString("defenseAtt", source, null) != null) {
                        addBuffStatPairToListIfNotZero(statups, BuffStat.DEFENSE_ATT, 4);
                    }

                    if (DataTool.getString("defenseState", source, null) != null) {
                        addBuffStatPairToListIfNotZero(statups, BuffStat.DEFENSE_STATE, 4);
                    }

                    int thaw = DataTool.getInt("thaw", source, 0);
                    if (thaw != 0) {
                        addBuffStatPairToListIfNotZero(statups, BuffStat.MAP_PROTECTION, thaw > 0 ? 1 : 2);
                    }

                    ret.cardStats = new CardItemupStats(itemupCode, prob, areas, inParty);
                } else if (ItemId.isExpIncrease(sourceid)) {
                    addBuffStatPairToListIfNotZero(statups, BuffStat.EXP_INCREASE, DataTool.getInt("expinc", source, 0));
                }
            } else {
                if (isMapChair(sourceid)) {
                    addBuffStatPairToListIfNotZero(statups, BuffStat.MAP_CHAIR, 1);
                } else if ((sourceid == Beginner.NIMBLE_FEET || sourceid == Noblesse.NIMBLE_FEET || sourceid == Evan.NIMBLE_FEET || sourceid == Legend.AGILE_BODY) && YamlConfig.config.server.USE_ULTRA_NIMBLE_FEET == true) {
                    ret.jump = (short) (ret.speed * 4);
                    ret.speed *= 15;
                }
            }

            addBuffStatPairToListIfNotZero(statups, BuffStat.WATK, (int) ret.watk);
            addBuffStatPairToListIfNotZero(statups, BuffStat.WDEF, (int) ret.wdef);
            addBuffStatPairToListIfNotZero(statups, BuffStat.MATK, (int) ret.matk);
            addBuffStatPairToListIfNotZero(statups, BuffStat.MDEF, (int) ret.mdef);
            addBuffStatPairToListIfNotZero(statups, BuffStat.ACC, (int) ret.acc);
            addBuffStatPairToListIfNotZero(statups, BuffStat.AVOID, (int) ret.avoid);
            addBuffStatPairToListIfNotZero(statups, BuffStat.SPEED, (int) ret.speed);
            addBuffStatPairToListIfNotZero(statups, BuffStat.JUMP, (int) ret.jump);
        }

        Data ltd = source.getChildByPath("lt");
        if (ltd != null) {
            ret.lt = (Point) ltd.getData();
            ret.rb = (Point) source.getChildByPath("rb").getData();

            if (YamlConfig.config.server.USE_MAXRANGE_ECHO_OF_HERO && (sourceid == Beginner.ECHO_OF_HERO || sourceid == Noblesse.ECHO_OF_HERO || sourceid == Legend.ECHO_OF_HERO || sourceid == Evan.ECHO_OF_HERO)) {
                ret.lt = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
                ret.rb = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
            }
        }

        int x = DataTool.getInt("x", source, 0);

        if ((sourceid == Beginner.RECOVERY || sourceid == Noblesse.RECOVERY || sourceid == Evan.RECOVERY || sourceid == Legend.RECOVERY) && YamlConfig.config.server.USE_ULTRA_RECOVERY == true) {
            x *= 10;
        }
        ret.x = x;
        ret.y = DataTool.getInt("y", source, 0);

        ret.damage = DataTool.getIntConvert("damage", source, 100);
        ret.fixdamage = DataTool.getIntConvert("fixdamage", source, -1);
        ret.attackCount = DataTool.getIntConvert("attackCount", source, 1);
        ret.bulletCount = (short) DataTool.getIntConvert("bulletCount", source, 1);
        ret.bulletConsume = (short) DataTool.getIntConvert("bulletConsume", source, 0);
        ret.moneyCon = DataTool.getIntConvert("moneyCon", source, 0);
        ret.itemCon = DataTool.getInt("itemCon", source, 0);
        ret.itemConNo = DataTool.getInt("itemConNo", source, 0);
        ret.moveTo = DataTool.getInt("moveTo", source, -1);
        Map<MonsterStatus, Integer> monsterStatus = new EnumMap<>(MonsterStatus.class);
        if (skill) {
            switch (sourceid) {
                // BEGINNER
                case Beginner.RECOVERY:
                case Noblesse.RECOVERY:
                case Legend.RECOVERY:
                case Evan.RECOVERY:
                    statups.add(new Pair<>(BuffStat.RECOVERY, x));
                    break;
                case Beginner.ECHO_OF_HERO:
                case Noblesse.ECHO_OF_HERO:
                case Legend.ECHO_OF_HERO:
                case Evan.ECHO_OF_HERO:
                    statups.add(new Pair<>(BuffStat.ECHO_OF_HERO, ret.x));
                    break;
                case Beginner.MONSTER_RIDER:
                case Noblesse.MONSTER_RIDER:
                case Legend.MONSTER_RIDER:
                case Corsair.BATTLE_SHIP:
                case Beginner.SPACESHIP:
                case Noblesse.SPACESHIP:
                case Beginner.YETI_MOUNT1:
                case Beginner.YETI_MOUNT2:
                case Noblesse.YETI_MOUNT1:
                case Noblesse.YETI_MOUNT2:
                case Legend.YETI_MOUNT1:
                case Legend.YETI_MOUNT2:
                case Beginner.WITCH_BROOMSTICK:
                case Noblesse.WITCH_BROOMSTICK:
                case Legend.WITCH_BROOMSTICK:
                case Beginner.BALROG_MOUNT:
                case Noblesse.BALROG_MOUNT:
                case Legend.BALROG_MOUNT:
                    statups.add(new Pair<>(BuffStat.MONSTER_RIDING, sourceid));
                    break;
                case Beginner.INVINCIBLE_BARRIER:
                case Noblesse.INVINCIBLE_BARRIER:
                case Legend.INVICIBLE_BARRIER:
                case Evan.INVINCIBLE_BARRIER:
                    statups.add(new Pair<>(BuffStat.DIVINE_BODY, 1));
                    break;
                case Fighter.POWER_GUARD:
                case Page.POWER_GUARD:
                    statups.add(new Pair<>(BuffStat.POWERGUARD, x));
                    break;
                case Spearman.HYPER_BODY:
                case GM.HYPER_BODY:
                case SuperGM.HYPER_BODY:
                    statups.add(new Pair<>(BuffStat.HYPERBODYHP, x));
                    statups.add(new Pair<>(BuffStat.HYPERBODYMP, ret.y));
                    break;
                case Crusader.COMBO:
                case DawnWarrior.COMBO:
                    statups.add(new Pair<>(BuffStat.COMBO, 1));
                    break;
                case WhiteKnight.BW_FIRE_CHARGE:
                case WhiteKnight.BW_ICE_CHARGE:
                case WhiteKnight.BW_LIT_CHARGE:
                case WhiteKnight.SWORD_FIRE_CHARGE:
                case WhiteKnight.SWORD_ICE_CHARGE:
                case WhiteKnight.SWORD_LIT_CHARGE:
                case Paladin.BW_HOLY_CHARGE:
                case Paladin.SWORD_HOLY_CHARGE:
                case DawnWarrior.SOUL_CHARGE:
                case ThunderBreaker.LIGHTNING_CHARGE:
                    statups.add(new Pair<>(BuffStat.WK_CHARGE, x));
                    break;
                case DragonKnight.DRAGON_BLOOD:
                    statups.add(new Pair<>(BuffStat.DRAGONBLOOD, ret.x));
                    break;
                case Hero.STANCE:
                case Paladin.STANCE:
                case DarkKnight.STANCE:
                case Aran.FREEZE_STANDING:
                    statups.add(new Pair<>(BuffStat.STANCE, iprop));
                    break;
                case DawnWarrior.FINAL_ATTACK:
                case WindArcher.FINAL_ATTACK:
                    statups.add(new Pair<>(BuffStat.FINALATTACK, x));
                    break;
                // MAGICIAN
                case Magician.MAGIC_GUARD:
                case BlazeWizard.MAGIC_GUARD:
                case Evan.MAGIC_GUARD:
                    statups.add(new Pair<>(BuffStat.MAGIC_GUARD, x));
                    break;
                case Cleric.INVINCIBLE:
                    statups.add(new Pair<>(BuffStat.INVINCIBLE, x));
                    break;
                case Priest.HOLY_SYMBOL:
                case SuperGM.HOLY_SYMBOL:
                    statups.add(new Pair<>(BuffStat.HOLY_SYMBOL, x));
                    break;
                case FPArchMage.INFINITY:
                case ILArchMage.INFINITY:
                case Bishop.INFINITY:
                    statups.add(new Pair<>(BuffStat.INFINITY, x));
                    break;
                case FPArchMage.MANA_REFLECTION:
                case ILArchMage.MANA_REFLECTION:
                case Bishop.MANA_REFLECTION:
                    statups.add(new Pair<>(BuffStat.MANA_REFLECTION, 1));
                    break;
                case Bishop.HOLY_SHIELD:
                    statups.add(new Pair<>(BuffStat.HOLY_SHIELD, x));
                    break;
                case BlazeWizard.ELEMENTAL_RESET:
                case Evan.ELEMENTAL_RESET:
                    statups.add(new Pair<>(BuffStat.ELEMENTAL_RESET, x));
                    break;
                case Evan.MAGIC_SHIELD:
                    statups.add(new Pair<>(BuffStat.MAGIC_SHIELD, x));
                    break;
                case Evan.MAGIC_RESISTANCE:
                    statups.add(new Pair<>(BuffStat.MAGIC_RESISTANCE, x));
                    break;
                case Evan.SLOW:
                    statups.add(new Pair<>(BuffStat.SLOW, x));
                    // BOWMAN
                case Priest.MYSTIC_DOOR:
                case Hunter.SOUL_ARROW:
                case Crossbowman.SOUL_ARROW:
                case WindArcher.SOUL_ARROW:
                    statups.add(new Pair<>(BuffStat.SOULARROW, x));
                    break;
                case Ranger.PUPPET:
                case Sniper.PUPPET:
                case WindArcher.PUPPET:
                case Outlaw.OCTOPUS:
                case Corsair.WRATH_OF_THE_OCTOPI:
                    statups.add(new Pair<>(BuffStat.PUPPET, 1));
                    break;
                case Bowmaster.CONCENTRATE:
                    statups.add(new Pair<>(BuffStat.CONCENTRATE, x));
                    break;
                case Bowmaster.HAMSTRING:
                    statups.add(new Pair<>(BuffStat.HAMSTRING, x));
                    monsterStatus.put(MonsterStatus.SPEED, x);
                    break;
                case Marksman.BLIND:
                    statups.add(new Pair<>(BuffStat.BLIND, x));
                    monsterStatus.put(MonsterStatus.ACC, x);
                    break;
                case Bowmaster.SHARP_EYES:
                case Marksman.SHARP_EYES:
                    statups.add(new Pair<>(BuffStat.SHARP_EYES, ret.x << 8 | ret.y));
                    break;
                case WindArcher.WIND_WALK:
                    statups.add(new Pair<>(BuffStat.WIND_WALK, x));
                    //break;    thanks Vcoc for noticing WW not showing for other players when changing maps
                case Rogue.DARK_SIGHT:
                case NightWalker.DARK_SIGHT:
                    statups.add(new Pair<>(BuffStat.DARKSIGHT, x));
                    break;
                case Hermit.MESO_UP:
                    statups.add(new Pair<>(BuffStat.MESOUP, x));
                    break;
                case Hermit.SHADOW_PARTNER:
                case NightWalker.SHADOW_PARTNER:
                    statups.add(new Pair<>(BuffStat.SHADOWPARTNER, x));
                    break;
                case ChiefBandit.MESO_GUARD:
                    statups.add(new Pair<>(BuffStat.MESOGUARD, x));
                    break;
                case ChiefBandit.PICKPOCKET:
                    statups.add(new Pair<>(BuffStat.PICKPOCKET, x));
                    break;
                case NightLord.SHADOW_STARS:
                    statups.add(new Pair<>(BuffStat.SHADOW_CLAW, 0));
                    break;
                // PIRATE
                case Pirate.DASH:
                case ThunderBreaker.DASH:
                case Beginner.SPACE_DASH:
                case Noblesse.SPACE_DASH:
                    statups.add(new Pair<>(BuffStat.DASH2, ret.x));
                    statups.add(new Pair<>(BuffStat.DASH, ret.y));
                    break;
                case Corsair.SPEED_INFUSION:
                case Buccaneer.SPEED_INFUSION:
                case ThunderBreaker.SPEED_INFUSION:
                    statups.add(new Pair<>(BuffStat.SPEED_INFUSION, x));
                    break;
                case Outlaw.HOMING_BEACON:
                case Corsair.BULLSEYE:
                    statups.add(new Pair<>(BuffStat.HOMING_BEACON, x));
                    break;
                case ThunderBreaker.SPARK:
                    statups.add(new Pair<>(BuffStat.SPARK, x));
                    break;
                // MULTIPLE
                case Aran.POLEARM_BOOSTER:
                case Fighter.AXE_BOOSTER:
                case Fighter.SWORD_BOOSTER:
                case Page.BW_BOOSTER:
                case Page.SWORD_BOOSTER:
                case Spearman.POLEARM_BOOSTER:
                case Spearman.SPEAR_BOOSTER:
                case Hunter.BOW_BOOSTER:
                case Crossbowman.CROSSBOW_BOOSTER:
                case Assassin.CLAW_BOOSTER:
                case Bandit.DAGGER_BOOSTER:
                case FPMage.SPELL_BOOSTER:
                case ILMage.SPELL_BOOSTER:
                case Brawler.KNUCKLER_BOOSTER:
                case Gunslinger.GUN_BOOSTER:
                case DawnWarrior.SWORD_BOOSTER:
                case BlazeWizard.SPELL_BOOSTER:
                case WindArcher.BOW_BOOSTER:
                case NightWalker.CLAW_BOOSTER:
                case ThunderBreaker.KNUCKLER_BOOSTER:
                case Evan.MAGIC_BOOSTER:
                case Beginner.POWER_EXPLOSION:
                case Noblesse.POWER_EXPLOSION:
                case Legend.POWER_EXPLOSION:
                    statups.add(new Pair<>(BuffStat.BOOSTER, x));
                    break;
                case Hero.MAPLE_WARRIOR:
                case Paladin.MAPLE_WARRIOR:
                case DarkKnight.MAPLE_WARRIOR:
                case FPArchMage.MAPLE_WARRIOR:
                case ILArchMage.MAPLE_WARRIOR:
                case Bishop.MAPLE_WARRIOR:
                case Bowmaster.MAPLE_WARRIOR:
                case Marksman.MAPLE_WARRIOR:
                case NightLord.MAPLE_WARRIOR:
                case Shadower.MAPLE_WARRIOR:
                case Corsair.MAPLE_WARRIOR:
                case Buccaneer.MAPLE_WARRIOR:
                case Aran.MAPLE_WARRIOR:
                case Evan.MAPLE_WARRIOR:
                    statups.add(new Pair<>(BuffStat.MAPLE_WARRIOR, ret.x));
                    break;
                // SUMMON
                case Ranger.SILVER_HAWK:
                case Sniper.GOLDEN_EAGLE:
                    statups.add(new Pair<>(BuffStat.SUMMON, 1));
                    monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case FPArchMage.ELQUINES:
                case Marksman.FROST_PREY:
                    statups.add(new Pair<>(BuffStat.SUMMON, 1));
                    monsterStatus.put(MonsterStatus.FREEZE, 1);
                    break;
                case Priest.SUMMON_DRAGON:
                case Bowmaster.PHOENIX:
                case ILArchMage.IFRIT:
                case Bishop.BAHAMUT:
                case DarkKnight.BEHOLDER:
                case Outlaw.GAVIOTA:
                case DawnWarrior.SOUL:
                case BlazeWizard.FLAME:
                case WindArcher.STORM:
                case NightWalker.DARKNESS:
                case ThunderBreaker.LIGHTNING:
                case BlazeWizard.IFRIT:
                    statups.add(new Pair<>(BuffStat.SUMMON, 1));
                    break;
                // ----------------------------- MONSTER STATUS ---------------------------------- //
                case Crusader.ARMOR_CRASH:
                case DragonKnight.POWER_CRASH:
                case WhiteKnight.MAGIC_CRASH:
                    monsterStatus.put(MonsterStatus.SEAL_SKILL, 1);
                    break;
                case Rogue.DISORDER:
                    monsterStatus.put(MonsterStatus.WATK, ret.x);
                    monsterStatus.put(MonsterStatus.WDEF, ret.y);
                    break;
                case Corsair.HYPNOTIZE:
                    monsterStatus.put(MonsterStatus.INERTMOB, 1);
                    break;
                case NightLord.NINJA_AMBUSH:
                case Shadower.NINJA_AMBUSH:
                    monsterStatus.put(MonsterStatus.NINJA_AMBUSH, ret.damage);
                    break;
                case Page.THREATEN:
                    monsterStatus.put(MonsterStatus.WATK, ret.x);
                    monsterStatus.put(MonsterStatus.WDEF, ret.y);
                    break;
                case DragonKnight.DRAGON_ROAR:
                    ret.hpR = -x / 100.0;
                    monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case Crusader.AXE_COMA:
                case Crusader.SWORD_COMA:
                case Crusader.SHOUT:
                case WhiteKnight.CHARGE_BLOW:
                case Hunter.ARROW_BOMB:
                case ChiefBandit.ASSAULTER:
                case Shadower.BOOMERANG_STEP:
                case Brawler.BACK_SPIN_BLOW:
                case Brawler.DOUBLE_UPPERCUT:
                case Buccaneer.DEMOLITION:
                case Buccaneer.SNATCH:
                case Buccaneer.BARRAGE:
                case Gunslinger.BLANK_SHOT:
                case DawnWarrior.COMA:
                case ThunderBreaker.BARRAGE:
                case Aran.ROLLING_SPIN:
                case Evan.FIRE_BREATH:
                case Evan.BLAZE:
                    monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case NightLord.TAUNT:
                case Shadower.TAUNT:
                    monsterStatus.put(MonsterStatus.SHOWDOWN, ret.x);
                    monsterStatus.put(MonsterStatus.MDEF, ret.x);
                    monsterStatus.put(MonsterStatus.WDEF, ret.x);
                    break;
                case ILWizard.COLD_BEAM:
                case ILMage.ICE_STRIKE:
                case ILArchMage.BLIZZARD:
                case ILMage.ELEMENT_COMPOSITION:
                case Sniper.BLIZZARD:
                case Outlaw.ICE_SPLITTER:
                case FPArchMage.PARALYZE:
                case Aran.COMBO_TEMPEST:
                case Evan.ICE_BREATH:
                    monsterStatus.put(MonsterStatus.FREEZE, 1);
                    ret.duration *= 2; // freezing skills are a little strange
                    break;
                case FPWizard.SLOW:
                case ILWizard.SLOW:
                case BlazeWizard.SLOW:
                    monsterStatus.put(MonsterStatus.SPEED, ret.x);
                    break;
                case FPWizard.POISON_BREATH:
                case FPMage.ELEMENT_COMPOSITION:
                    monsterStatus.put(MonsterStatus.POISON, 1);
                    break;
                case Priest.DOOM:
                    monsterStatus.put(MonsterStatus.DOOM, 1);
                    break;
                case ILMage.SEAL:
                case FPMage.SEAL:
                case BlazeWizard.SEAL:
                    monsterStatus.put(MonsterStatus.SEAL, 1);
                    break;
                case Hermit.SHADOW_WEB: // shadow web
                case NightWalker.SHADOW_WEB:
                    monsterStatus.put(MonsterStatus.SHADOW_WEB, 1);
                    break;
                case FPArchMage.FIRE_DEMON:
                case ILArchMage.ICE_DEMON:
                    monsterStatus.put(MonsterStatus.POISON, 1);
                    monsterStatus.put(MonsterStatus.FREEZE, 1);
                    break;
                case Evan.PHANTOM_IMPRINT:
                    monsterStatus.put(MonsterStatus.PHANTOM_IMPRINT, x);
                    //ARAN
                case Aran.COMBO_ABILITY:
                    statups.add(new Pair<>(BuffStat.ARAN_COMBO, 100));
                    break;
                case Aran.COMBO_BARRIER:
                    statups.add(new Pair<>(BuffStat.COMBO_BARRIER, ret.x));
                    break;
                case Aran.COMBO_DRAIN:
                    statups.add(new Pair<>(BuffStat.COMBO_DRAIN, ret.x));
                    break;
                case Aran.SMART_KNOCKBACK:
                    statups.add(new Pair<>(BuffStat.SMART_KNOCKBACK, ret.x));
                    break;
                case Aran.BODY_PRESSURE:
                    statups.add(new Pair<>(BuffStat.BODY_PRESSURE, ret.x));
                    break;
                case Aran.SNOW_CHARGE:
                    statups.add(new Pair<>(BuffStat.WK_CHARGE, ret.duration));
                    break;
                default:
                    break;
            }
        }
        if (ret.isMorph()) {
            statups.add(new Pair<>(BuffStat.MORPH, ret.getMorph()));
        }
        if (ret.ghost > 0 && !skill) {
            statups.add(new Pair<>(BuffStat.GHOST_MORPH, ret.ghost));
        }
        ret.monsterStatus = monsterStatus;
        statups.trimToSize();
        ret.statups = statups;
        return ret;
    }

    /**
     * @param applyto
     * @param obj
     * @param attack  damage done by the skill
     */
    public void applyPassive(Character applyto, MapObject obj, int attack) {
        if (makeChanceResult()) {
            switch (sourceid) { // MP eater
                case FPWizard.MP_EATER:
                case ILWizard.MP_EATER:
                case Cleric.MP_EATER:
                    if (obj == null || obj.getType() != MapObjectType.MONSTER) {
                        return;
                    }
                    Monster mob = (Monster) obj; // x is absorb percentage
                    if (!mob.isBoss()) {
                        int absorbMp = Math.min((int) (mob.getMaxMp() * (getX() / 100.0)), mob.getMp());
                        if (absorbMp > 0) {
                            mob.setMp(mob.getMp() - absorbMp);
                            applyto.addMP(absorbMp);
                            applyto.sendPacket(PacketCreator.showOwnBuffEffect(sourceid, 1));
                            applyto.getMap().broadcastMessage(applyto, PacketCreator.showBuffEffect(applyto.getId(), sourceid, 1), false);
                        }
                    }
                    break;
            }
        }
    }

    public boolean applyEchoOfHero(Character applyfrom) {
        Map<Integer, Character> mapPlayers = applyfrom.getMap().getMapPlayers();
        mapPlayers.remove(applyfrom.getId());

        boolean hwResult = applyTo(applyfrom);
        for (Character chr : mapPlayers.values()) {    // Echo of Hero not buffing players in the map detected thanks to Masterrulax
            applyTo(applyfrom, chr, false, null, false, 1);
        }

        return hwResult;
    }

    public boolean applyTo(Character chr) {
        return applyTo(chr, chr, true, null, false, 1);
    }

    public boolean applyTo(Character chr, boolean useMaxRange) {
        return applyTo(chr, chr, true, null, useMaxRange, 1);
    }

    public boolean applyTo(Character chr, Point pos) {
        return applyTo(chr, chr, true, pos, false, 1);
    }

    // primary: the player caster of the buff
    private boolean applyTo(Character applyfrom, Character applyto, boolean primary, Point pos, boolean useMaxRange, int affectedPlayers) {
        if (skill && (sourceid == GM.HIDE || sourceid == SuperGM.HIDE)) {
            applyto.toggleHide(false);
            return true;
        }

        if (primary && isHeal()) {
            affectedPlayers = applyBuff(applyfrom, useMaxRange);
        }

        int hpchange = calcHPChange(applyfrom, primary, affectedPlayers);
        int mpchange = calcMPChange(applyfrom, primary);
        if (primary) {
            if (itemConNo != 0) {
                if (!applyto.getAbstractPlayerInteraction().hasItem(itemCon, itemConNo)) {
                    applyto.sendPacket(PacketCreator.enableActions());
                    return false;
                }
                InventoryManipulator.removeById(applyto.getClient(), ItemConstants.getInventoryType(itemCon), itemCon, itemConNo, false, true);
            }
        } else {
            if (isResurrection()) {
                hpchange = applyto.getCurrentMaxHp();
                applyto.broadcastStance(applyto.isFacingLeft() ? 5 : 4);
            }
        }

        if (isDispel() && makeChanceResult()) {
            applyto.dispelDebuffs();
        } else if (isCureAllAbnormalStatus()) {
            applyto.purgeDebuffs();
        } else if (isComboReset()) {
            applyto.setCombo((short) 0);
        }
        /*if (applyfrom.getMp() < getMpCon()) {
         AutobanFactory.MPCON.addPoint(applyfrom.getAutobanManager(), "mpCon hack for skill:" + sourceid + "; Player MP: " + applyto.getMp() + " MP Needed: " + getMpCon());
         } */

        if (!applyto.applyHpMpChange(hpCon, hpchange, mpchange)) {
            applyto.sendPacket(PacketCreator.enableActions());
            return false;
        }

        if (moveTo != -1) {
            if (moveTo != applyto.getMapId()) {
                MapleMap target;
                Portal pt;

                if (moveTo == MapId.NONE) {
                    if (sourceid != ItemId.ANTI_BANISH_SCROLL) {
                        target = applyto.getMap().getReturnMap();
                        pt = target.getRandomPlayerSpawnpoint();
                    } else {
                        if (!applyto.canRecoverLastBanish()) {
                            return false;
                        }

                        Pair<Integer, Integer> lastBanishInfo = applyto.getLastBanishData();
                        target = applyto.getWarpMap(lastBanishInfo.getLeft());
                        pt = target.getPortal(lastBanishInfo.getRight());
                    }
                } else {
                    target = applyto.getClient().getWorldServer().getChannel(applyto.getClient().getChannel()).getMapFactory().getMap(moveTo);
                    int targetid = target.getId() / 10000000;
                    if (targetid != 60 && applyto.getMapId() / 10000000 != 61 && targetid != applyto.getMapId() / 10000000 && targetid != 21 && targetid != 20 && targetid != 12 && (applyto.getMapId() / 10000000 != 10 && applyto.getMapId() / 10000000 != 12)) {
                        return false;
                    }

                    pt = target.getRandomPlayerSpawnpoint();
                }

                applyto.changeMap(target, pt);
            } else {
                return false;
            }
        }
        if (isShadowClaw()) {
            short projectileConsume = this.getBulletConsume();  // noticed by shavit

            Inventory use = applyto.getInventory(InventoryType.USE);
            use.lockInventory();
            try {
                Item projectile = null;
                for (int i = 1; i <= use.getSlotLimit(); i++) { // impose order...
                    Item item = use.getItem((short) i);
                    if (item != null) {
                        if (ItemConstants.isThrowingStar(item.getItemId()) && item.getQuantity() >= projectileConsume) {
                            projectile = item;
                            break;
                        }
                    }
                }
                if (projectile == null) {
                    return false;
                } else {
                    InventoryManipulator.removeFromSlot(applyto.getClient(), InventoryType.USE, projectile.getPosition(), projectileConsume, false, true);
                }
            } finally {
                use.unlockInventory();
            }
        }
        SummonMovementType summonMovementType = getSummonMovementType();
        if (overTime || isCygnusFA() || summonMovementType != null) {
            if (summonMovementType != null && pos != null) {
                if (summonMovementType.getValue() == SummonMovementType.STATIONARY.getValue()) {
                    applyto.cancelBuffStats(BuffStat.PUPPET);
                } else {
                    applyto.cancelBuffStats(BuffStat.SUMMON);
                }

                applyto.sendPacket(PacketCreator.enableActions());
            }

            applyBuffEffect(applyfrom, applyto, primary);
        }

        if (primary) {
            if (overTime) {
                applyBuff(applyfrom, useMaxRange);
            }

            if (isMonsterBuff()) {
                applyMonsterBuff(applyfrom);
            }
        }

        if (this.getFatigue() != 0) {
            applyto.getMount().setTiredness(applyto.getMount().getTiredness() + this.getFatigue());
        }

        if (summonMovementType != null && pos != null) {
            final Summon tosummon = new Summon(applyfrom, sourceid, pos, summonMovementType);
            applyfrom.getMap().spawnSummon(tosummon);
            applyfrom.addSummon(sourceid, tosummon);
            tosummon.addHP(x);
            if (isBeholder()) {
                tosummon.addHP(1);
            }
        }
        if (isMagicDoor() && !FieldLimit.DOOR.check(applyto.getMap().getFieldLimit())) { // Magic Door
            int y = applyto.getFh();
            if (y == 0) {
                y = applyto.getMap().getGroundBelow(applyto.getPosition()).y;    // thanks Lame for pointing out unusual cases of doors sending players on ground below
            }
            Point doorPosition = new Point(applyto.getPosition().x, y);
            Door door = new Door(applyto, doorPosition);

            if (door.getOwnerId() >= 0) {
                applyto.applyPartyDoor(door, false);

                door.getTarget().spawnDoor(door.getAreaDoor());
                door.getTown().spawnDoor(door.getTownDoor());
            } else {
                InventoryManipulator.addFromDrop(applyto.getClient(), new Item(ItemId.MAGIC_ROCK, (short) 0, (short) 1), false);

                if (door.getOwnerId() == -3) {
                    applyto.dropMessage(5, "Mystic Door cannot be cast far from a spawn point. Nearest one is at " + door.getDoorStatus().getRight() + "pts " + door.getDoorStatus().getLeft());
                } else if (door.getOwnerId() == -2) {
                    applyto.dropMessage(5, "Mystic Door cannot be cast on a slope, try elsewhere.");
                } else {
                    applyto.dropMessage(5, "There are no door portals available for the town at this moment. Try again later.");
                }

                applyto.cancelBuffStats(BuffStat.SOULARROW);  // cancel door buff
            }
        } else if (isMist()) {
            Rectangle bounds = calculateBoundingBox(sourceid == NightWalker.POISON_BOMB ? pos : applyfrom.getPosition(), applyfrom.isFacingLeft());
            Mist mist = new Mist(bounds, applyfrom, this);
            applyfrom.getMap().spawnMist(mist, getDuration(), mist.isPoisonMist(), false, mist.isRecoveryMist());
        } else if (isTimeLeap()) {
            applyto.removeAllCooldownsExcept(Buccaneer.TIME_LEAP, true);
        } else if (cp != 0 && applyto.getMonsterCarnival() != null) {
            applyto.gainCP(cp);
        } else if (nuffSkill != 0 && applyto.getParty() != null && applyto.getMap().isCPQMap()) { // added by Drago (Dragohe4rt)
            final MCSkill skill = CarnivalFactory.getInstance().getSkill(nuffSkill);
            if (skill != null) {
                final Disease dis = skill.getDisease();
                Party opposition = applyfrom.getParty().getEnemy();
                if (skill.targetsAll()) {
                    for (PartyCharacter enemyChrs : opposition.getPartyMembers()) {
                        Character chrApp = enemyChrs.getPlayer();
                        if (chrApp != null && chrApp.getMap().isCPQMap()) {
                            if (dis == null) {
                                chrApp.dispel();
                            } else {
                                MobSkill mobSkill = MobSkillFactory.getMobSkillOrThrow(dis.getMobSkillType(), skill.level());
                                chrApp.giveDebuff(dis, mobSkill);
                            }
                        }
                    }
                } else {
                    int amount = opposition.getMembers().size();
                    int randd = (int) Math.floor(Math.random() * amount);
                    Character chrApp = applyfrom.getMap().getCharacterById(opposition.getMemberByPos(randd).getId());
                    if (chrApp != null && chrApp.getMap().isCPQMap()) {
                        if (dis == null) {
                            chrApp.dispel();
                        } else {
                            MobSkill mobSkill = MobSkillFactory.getMobSkillOrThrow(dis.getMobSkillType(), skill.level());
                            chrApp.giveDebuff(dis, mobSkill);
                        }
                    }
                }
            }
        } else if (cureDebuffs.size() > 0) { // added by Drago (Dragohe4rt)
            for (final Disease debuff : cureDebuffs) {
                applyfrom.dispelDebuff(debuff);
            }
        } else if (mobSkill > 0 && mobSkillLevel > 0) {
            MobSkillType mobSkillType = MobSkillType.from(mobSkill).orElseThrow();
            MobSkill ms = MobSkillFactory.getMobSkillOrThrow(mobSkillType, mobSkillLevel);
            Disease dis = Disease.getBySkill(mobSkillType);

            if (target > 0) {
                for (Character chr : applyto.getMap().getAllPlayers()) {
                    if (chr.getId() != applyto.getId()) {
                        chr.giveDebuff(dis, ms);
                    }
                }
            } else {
                applyto.giveDebuff(dis, ms);
            }
        }
        return true;
    }

    private int applyBuff(Character applyfrom, boolean useMaxRange) {
        int affectedc = 1;

        if (isPartyBuff() && (applyfrom.getParty() != null || isGmBuff())) {
            Rectangle bounds = (!useMaxRange) ? calculateBoundingBox(applyfrom.getPosition(), applyfrom.isFacingLeft()) : new Rectangle(Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2, Integer.MAX_VALUE, Integer.MAX_VALUE);
            List<MapObject> affecteds = applyfrom.getMap().getMapObjectsInRect(bounds, Arrays.asList(MapObjectType.PLAYER));
            List<Character> affectedp = new ArrayList<>(affecteds.size());
            for (MapObject affectedmo : affecteds) {
                Character affected = (Character) affectedmo;
                if (affected != applyfrom && (isGmBuff() || applyfrom.getParty().equals(affected.getParty()))) {
                    if (!isResurrection()) {
                        if (affected.isAlive()) {
                            affectedp.add(affected);
                        }
                    } else {
                        if (!affected.isAlive()) {
                            affectedp.add(affected);
                        }
                    }
                }
            }

            affectedc += affectedp.size();   // used for heal
            for (Character affected : affectedp) {
                applyTo(applyfrom, affected, false, null, useMaxRange, affectedc);
                affected.sendPacket(PacketCreator.showOwnBuffEffect(sourceid, 2));
                affected.getMap().broadcastMessage(affected, PacketCreator.showBuffEffect(affected.getId(), sourceid, 2), false);
            }
        }

        return affectedc;
    }

    private void applyMonsterBuff(Character applyfrom) {
        Rectangle bounds = calculateBoundingBox(applyfrom.getPosition(), applyfrom.isFacingLeft());
        List<MapObject> affected = applyfrom.getMap().getMapObjectsInRect(bounds, Arrays.asList(MapObjectType.MONSTER));
        Skill skill_ = SkillFactory.getSkill(sourceid);
        int i = 0;
        for (MapObject mo : affected) {
            Monster monster = (Monster) mo;
            if (isDispel()) {
                monster.debuffMob(skill_.getId());
            } else if (isSeal() && monster.isBoss()) {  // thanks IxianMace for noticing seal working on bosses
                // do nothing
            } else {
                if (makeChanceResult()) {
                    monster.applyStatus(applyfrom, new MonsterStatusEffect(getMonsterStati(), skill_, null, false), isPoison(), getDuration());
                    if (isCrash()) {
                        monster.debuffMob(skill_.getId());
                    }
                }
            }
            i++;
            if (i >= mobCount) {
                break;
            }
        }
    }

    private Rectangle calculateBoundingBox(Point posFrom, boolean facingLeft) {
        Point mylt;
        Point myrb;
        if (facingLeft) {
            mylt = new Point(lt.x + posFrom.x, lt.y + posFrom.y);
            myrb = new Point(rb.x + posFrom.x, rb.y + posFrom.y);
        } else {
            myrb = new Point(-lt.x + posFrom.x, rb.y + posFrom.y);  // thanks Conrad, April for noticing a disturbance in AoE skill behavior after a hitched refactor here
            mylt = new Point(-rb.x + posFrom.x, lt.y + posFrom.y);
        }
        Rectangle bounds = new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
        return bounds;
    }

    public int getBuffLocalDuration() {
        return !YamlConfig.config.server.USE_BUFF_EVERLASTING ? duration : Integer.MAX_VALUE;
    }

    public void silentApplyBuff(Character chr, long localStartTime) {
        int localDuration = getBuffLocalDuration();
        localDuration = alchemistModifyVal(chr, localDuration, false);
        //CancelEffectAction cancelAction = new CancelEffectAction(chr, this, starttime);
        //ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, ((starttime + localDuration) - Server.getInstance().getCurrentTime()));

        chr.registerEffect(this, localStartTime, localStartTime + localDuration, true);
        SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType != null) {
            final Summon tosummon = new Summon(chr, sourceid, chr.getPosition(), summonMovementType);
            if (!tosummon.isStationary()) {
                chr.addSummon(sourceid, tosummon);
                tosummon.addHP(x);
            }
        }
        if (sourceid == Corsair.BATTLE_SHIP) {
            chr.announceBattleshipHp();
        }
    }

    public final void applyComboBuff(final Character applyto, int combo) {
        final List<Pair<BuffStat, Integer>> stat = Collections.singletonList(new Pair<>(BuffStat.ARAN_COMBO, combo));
        applyto.sendPacket(PacketCreator.giveBuff(sourceid, 99999, stat));

        final long starttime = Server.getInstance().getCurrentTime();
//	final CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime);
//	final ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, ((starttime + 99999) - Server.getInstance().getCurrentTime()));
        applyto.registerEffect(this, starttime, Long.MAX_VALUE, false);
    }

    public final void applyBeaconBuff(final Character applyto, int objectid) { // thanks Thora & Hyun for reporting an issue with homing beacon autoflagging mobs when changing maps
        final List<Pair<BuffStat, Integer>> stat = Collections.singletonList(new Pair<>(BuffStat.HOMING_BEACON, objectid));
        applyto.sendPacket(PacketCreator.giveBuff(1, sourceid, stat));

        final long starttime = Server.getInstance().getCurrentTime();
        applyto.registerEffect(this, starttime, Long.MAX_VALUE, false);
    }

    public void updateBuffEffect(Character target, List<Pair<BuffStat, Integer>> activeStats, long starttime) {
        int localDuration = getBuffLocalDuration();
        localDuration = alchemistModifyVal(target, localDuration, false);

        long leftDuration = (starttime + localDuration) - Server.getInstance().getCurrentTime();
        if (leftDuration > 0) {
            if (isDash() || isInfusion()) {
                target.sendPacket(PacketCreator.givePirateBuff(activeStats, (skill ? sourceid : -sourceid), (int) leftDuration));
            } else {
                target.sendPacket(PacketCreator.giveBuff((skill ? sourceid : -sourceid), (int) leftDuration, activeStats));
            }
        }
    }

    private void applyBuffEffect(Character applyfrom, Character applyto, boolean primary) {
        if (!isMonsterRiding() && !isCouponBuff() && !isMysticDoor() && !isHyperBody() && !isCombo()) {     // last mystic door already dispelled if it has been used before.
            applyto.cancelEffect(this, true, -1);
        }

        List<Pair<BuffStat, Integer>> localstatups = statups;
        int localDuration = getBuffLocalDuration();
        int localsourceid = sourceid;
        int seconds = localDuration / 1000;
        Mount givemount = null;
        if (isMonsterRiding()) {
            int ridingMountId = 0;
            Item mount = applyfrom.getInventory(InventoryType.EQUIPPED).getItem((short) -18);
            if (mount != null) {
                ridingMountId = mount.getItemId();
            }

            if (sourceid == Corsair.BATTLE_SHIP) {
                ridingMountId = ItemId.BATTLESHIP;
            } else if (sourceid == Beginner.SPACESHIP || sourceid == Noblesse.SPACESHIP) {
                ridingMountId = 1932000 + applyto.getSkillLevel(sourceid);
            } else if (sourceid == Beginner.YETI_MOUNT1 || sourceid == Noblesse.YETI_MOUNT1 || sourceid == Legend.YETI_MOUNT1) {
                ridingMountId = 1932003;
            } else if (sourceid == Beginner.YETI_MOUNT2 || sourceid == Noblesse.YETI_MOUNT2 || sourceid == Legend.YETI_MOUNT2) {
                ridingMountId = 1932004;
            } else if (sourceid == Beginner.WITCH_BROOMSTICK || sourceid == Noblesse.WITCH_BROOMSTICK || sourceid == Legend.WITCH_BROOMSTICK) {
                ridingMountId = 1932005;
            } else if (sourceid == Beginner.BALROG_MOUNT || sourceid == Noblesse.BALROG_MOUNT || sourceid == Legend.BALROG_MOUNT) {
                ridingMountId = 1932010;
            }

            // thanks inhyuk for noticing some skill mounts not acting properly for other players when changing maps
            givemount = applyto.mount(ridingMountId, sourceid);
            applyto.getClient().getWorldServer().registerMountHunger(applyto);

            localDuration = sourceid;
            localsourceid = ridingMountId;
            localstatups = Collections.singletonList(new Pair<>(BuffStat.MONSTER_RIDING, 0));
        } else if (isSkillMorph()) {
            for (int i = 0; i < localstatups.size(); i++) {
                if (localstatups.get(i).getLeft().equals(BuffStat.MORPH)) {
                    localstatups.set(i, new Pair<>(BuffStat.MORPH, getMorph(applyto)));
                    break;
                }
            }
        }
        if (primary) {
            localDuration = alchemistModifyVal(applyfrom, localDuration, false);
            applyto.getMap().broadcastMessage(applyto, PacketCreator.showBuffEffect(applyto.getId(), sourceid, 1, (byte) 3), false);
        }
        if (localstatups.size() > 0) {
            Packet buff = null;
            Packet mbuff = null;
            if (this.isActive(applyto)) {
                buff = PacketCreator.giveBuff((skill ? sourceid : -sourceid), localDuration, localstatups);
            }
            if (isDash()) {
                buff = PacketCreator.givePirateBuff(statups, sourceid, seconds);
                mbuff = PacketCreator.giveForeignPirateBuff(applyto.getId(), sourceid, seconds, localstatups);
            } else if (isWkCharge()) {
                mbuff = PacketCreator.giveForeignWKChargeEffect(applyto.getId(), sourceid, localstatups);
            } else if (isInfusion()) {
                buff = PacketCreator.givePirateBuff(localstatups, sourceid, seconds);
                mbuff = PacketCreator.giveForeignPirateBuff(applyto.getId(), sourceid, seconds, localstatups);
            } else if (isDs()) {
                List<Pair<BuffStat, Integer>> dsstat = Collections.singletonList(new Pair<>(BuffStat.DARKSIGHT, 0));
                mbuff = PacketCreator.giveForeignBuff(applyto.getId(), dsstat);
            } else if (isWw()) {
                List<Pair<BuffStat, Integer>> dsstat = Collections.singletonList(new Pair<>(BuffStat.WIND_WALK, 0));
                mbuff = PacketCreator.giveForeignBuff(applyto.getId(), dsstat);
            } else if (isCombo()) {
                Integer comboCount = applyto.getBuffedValue(BuffStat.COMBO);
                if (comboCount == null) {
                    comboCount = 0;
                }

                List<Pair<BuffStat, Integer>> cbstat = Collections.singletonList(new Pair<>(BuffStat.COMBO, comboCount));
                buff = PacketCreator.giveBuff((skill ? sourceid : -sourceid), localDuration, cbstat);
                mbuff = PacketCreator.giveForeignBuff(applyto.getId(), cbstat);
            } else if (isMonsterRiding()) {
                if (sourceid == Corsair.BATTLE_SHIP) {//hp
                    if (applyto.getBattleshipHp() <= 0) {
                        applyto.resetBattleshipHp();
                    }

                    localstatups = statups;
                }
                buff = PacketCreator.giveBuff(localsourceid, localDuration, localstatups);
                mbuff = PacketCreator.showMonsterRiding(applyto.getId(), givemount);
                localDuration = duration;
            } else if (isShadowPartner()) {
                List<Pair<BuffStat, Integer>> stat = Collections.singletonList(new Pair<>(BuffStat.SHADOWPARTNER, 0));
                mbuff = PacketCreator.giveForeignBuff(applyto.getId(), stat);
            } else if (isSoulArrow()) {
                List<Pair<BuffStat, Integer>> stat = Collections.singletonList(new Pair<>(BuffStat.SOULARROW, 0));
                mbuff = PacketCreator.giveForeignBuff(applyto.getId(), stat);
            } else if (isEnrage()) {
                applyto.handleOrbconsume();
            } else if (isMorph()) {
                List<Pair<BuffStat, Integer>> stat = Collections.singletonList(new Pair<>(BuffStat.MORPH, getMorph(applyto)));
                mbuff = PacketCreator.giveForeignBuff(applyto.getId(), stat);
            } else if (isAriantShield()) {
                List<Pair<BuffStat, Integer>> stat = Collections.singletonList(new Pair<>(BuffStat.AURA, 1));
                mbuff = PacketCreator.giveForeignBuff(applyto.getId(), stat);
            }

            if (buff != null) {
                //Thanks flav for such a simple release! :)
                //Thanks Conrad, Atoot for noticing summons not using buff icon

                applyto.sendPacket(buff);
            }

            long starttime = Server.getInstance().getCurrentTime();
            //CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime);
            //ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, localDuration);
            applyto.registerEffect(this, starttime, starttime + localDuration, false);
            if (mbuff != null) {
                applyto.getMap().broadcastMessage(applyto, mbuff, false);
            }
            if (sourceid == Corsair.BATTLE_SHIP) {
                applyto.announceBattleshipHp();
            }
        }
    }

    private int calcHPChange(Character applyfrom, boolean primary, int affectedPlayers) {
        int hpchange = 0;
        if (hp != 0) {
            if (!skill) {
                if (primary) {
                    hpchange += alchemistModifyVal(applyfrom, hp, true);
                } else {
                    hpchange += hp;
                }
                if (applyfrom.hasDisease(Disease.ZOMBIFY)) {
                    hpchange /= 2;
                }
            } else { // assumption: this is heal
                float hpHeal = (applyfrom.getCurrentMaxHp() * (float) hp / (100.0f * affectedPlayers));
                hpchange += hpHeal;
                if (applyfrom.hasDisease(Disease.ZOMBIFY)) {
                    hpchange = -hpchange;
                    hpCon = 0;
                }
            }
        }
        if (hpR != 0) {
            hpchange += (int) (applyfrom.getCurrentMaxHp() * hpR) / (applyfrom.hasDisease(Disease.ZOMBIFY) ? 2 : 1);
        }
        if (primary) {
            if (hpCon != 0) {
                hpchange -= hpCon;
            }
        }
        if (isChakra()) {
            hpchange += makeHealHP(getY() / 100.0, applyfrom.getTotalLuk(), 2.3, 3.5);
        } else if (sourceid == SuperGM.HEAL_PLUS_DISPEL) {
            hpchange += applyfrom.getCurrentMaxHp();
        }

        return hpchange;
    }

    private int makeHealHP(double rate, double stat, double lowerfactor, double upperfactor) {
        return (int) ((Math.random() * ((int) (stat * upperfactor * rate) - (int) (stat * lowerfactor * rate) + 1)) + (int) (stat * lowerfactor * rate));
    }

    private int calcMPChange(Character applyfrom, boolean primary) {
        int mpchange = 0;
        if (mp != 0) {
            if (primary) {
                mpchange += alchemistModifyVal(applyfrom, mp, true);
            } else {
                mpchange += mp;
            }
        }
        if (mpR != 0) {
            mpchange += (int) (applyfrom.getCurrentMaxMp() * mpR);
        }
        if (primary) {
            if (mpCon != 0) {
                double mod = 1.0;
                boolean isAFpMage = applyfrom.getJob().isA(Job.FP_MAGE);
                boolean isCygnus = applyfrom.getJob().isA(Job.BLAZEWIZARD2);
                boolean isEvan = applyfrom.getJob().isA(Job.EVAN7);
                if (isAFpMage || isCygnus || isEvan || applyfrom.getJob().isA(Job.IL_MAGE)) {
                    Skill amp = isAFpMage ? SkillFactory.getSkill(FPMage.ELEMENT_AMPLIFICATION) : (isCygnus ? SkillFactory.getSkill(BlazeWizard.ELEMENT_AMPLIFICATION) : (isEvan ? SkillFactory.getSkill(Evan.MAGIC_AMPLIFICATION) : SkillFactory.getSkill(ILMage.ELEMENT_AMPLIFICATION)));
                    int ampLevel = applyfrom.getSkillLevel(amp);
                    if (ampLevel > 0) {
                        mod = amp.getEffect(ampLevel).getX() / 100.0;
                    }
                }
                mpchange -= mpCon * mod;
                if (applyfrom.getBuffedValue(BuffStat.INFINITY) != null) {
                    mpchange = 0;
                } else if (applyfrom.getBuffedValue(BuffStat.CONCENTRATE) != null) {
                    mpchange -= (int) (mpchange * (applyfrom.getBuffedValue(BuffStat.CONCENTRATE).doubleValue() / 100));
                }
            }
        }
        if (sourceid == SuperGM.HEAL_PLUS_DISPEL) {
            mpchange += applyfrom.getCurrentMaxMp();
        }

        return mpchange;
    }

    private int alchemistModifyVal(Character chr, int val, boolean withX) {
        if (!skill && (chr.getJob().isA(Job.HERMIT) || chr.getJob().isA(Job.NIGHTWALKER3))) {
            StatEffect alchemistEffect = getAlchemistEffect(chr);
            if (alchemistEffect != null) {
                return (int) (val * ((withX ? alchemistEffect.getX() : alchemistEffect.getY()) / 100.0));
            }
        }
        return val;
    }

    private StatEffect getAlchemistEffect(Character chr) {
        int id = Hermit.ALCHEMIST;
        if (chr.isCygnus()) {
            id = NightWalker.ALCHEMIST;
        }
        int alchemistLevel = chr.getSkillLevel(SkillFactory.getSkill(id));
        return alchemistLevel == 0 ? null : SkillFactory.getSkill(id).getEffect(alchemistLevel);
    }

    private boolean isGmBuff() {
        switch (sourceid) {
            case Beginner.ECHO_OF_HERO:
            case Noblesse.ECHO_OF_HERO:
            case Legend.ECHO_OF_HERO:
            case Evan.ECHO_OF_HERO:
            case SuperGM.HEAL_PLUS_DISPEL:
            case SuperGM.HASTE:
            case SuperGM.HOLY_SYMBOL:
            case SuperGM.BLESS:
            case SuperGM.RESURRECTION:
            case SuperGM.HYPER_BODY:
                return true;
            default:
                return false;
        }
    }

    private boolean isMonsterBuff() {
        if (!skill) {
            return false;
        }
        switch (sourceid) {
            case Page.THREATEN:
            case FPWizard.SLOW:
            case ILWizard.SLOW:
            case FPMage.SEAL:
            case ILMage.SEAL:
            case Priest.DOOM:
            case Hermit.SHADOW_WEB:
            case NightLord.NINJA_AMBUSH:
            case Shadower.NINJA_AMBUSH:
            case BlazeWizard.SLOW:
            case BlazeWizard.SEAL:
            case NightWalker.SHADOW_WEB:
            case Crusader.ARMOR_CRASH:
            case DragonKnight.POWER_CRASH:
            case WhiteKnight.MAGIC_CRASH:
            case Priest.DISPEL:
            case SuperGM.HEAL_PLUS_DISPEL:
                return true;
        }
        return false;
    }

    private boolean isPartyBuff() {
        if (lt == null || rb == null) {
            return false;
        }
        // wk charges have lt and rb set but are neither player nor monster buffs
        return (sourceid < 1211003 || sourceid > 1211008) && sourceid != Paladin.SWORD_HOLY_CHARGE && sourceid != Paladin.BW_HOLY_CHARGE && sourceid != DawnWarrior.SOUL_CHARGE;
    }

    private boolean isHeal() {
        return sourceid == Cleric.HEAL || sourceid == SuperGM.HEAL_PLUS_DISPEL;
    }

    private boolean isResurrection() {
        return sourceid == Bishop.RESURRECTION || sourceid == GM.RESURRECTION || sourceid == SuperGM.RESURRECTION;
    }

    private boolean isTimeLeap() {
        return sourceid == Buccaneer.TIME_LEAP;
    }

    public boolean isDragonBlood() {
        return skill && sourceid == DragonKnight.DRAGON_BLOOD;
    }

    public boolean isBerserk() {
        return skill && sourceid == DarkKnight.BERSERK;
    }

    public boolean isRecovery() {
        return sourceid == Beginner.RECOVERY || sourceid == Noblesse.RECOVERY || sourceid == Legend.RECOVERY || sourceid == Evan.RECOVERY;
    }

    public boolean isMapChair() {
        return sourceid == Beginner.MAP_CHAIR || sourceid == Noblesse.MAP_CHAIR || sourceid == Legend.MAP_CHAIR;
    }

    public static boolean isMapChair(int sourceid) {
        return sourceid == Beginner.MAP_CHAIR || sourceid == Noblesse.MAP_CHAIR || sourceid == Legend.MAP_CHAIR;
    }

    public static boolean isHpMpRecovery(int sourceid) {
        return sourceid == ItemId.RUSSELLONS_PILLS || sourceid == ItemId.SORCERERS_POTION;
    }

    public static boolean isAriantShield(int sourceid) {
        return sourceid == ItemId.ARPQ_SHIELD;
    }

    private boolean isDs() {
        return skill && (sourceid == Rogue.DARK_SIGHT || sourceid == NightWalker.DARK_SIGHT);
    }

    private boolean isWw() {
        return skill && (sourceid == WindArcher.WIND_WALK);
    }

    private boolean isCombo() {
        return skill && (sourceid == Crusader.COMBO || sourceid == DawnWarrior.COMBO);
    }

    private boolean isEnrage() {
        return skill && sourceid == Hero.ENRAGE;
    }

    public boolean isBeholder() {
        return skill && sourceid == DarkKnight.BEHOLDER;
    }

    private boolean isShadowPartner() {
        return skill && (sourceid == Hermit.SHADOW_PARTNER || sourceid == NightWalker.SHADOW_PARTNER);
    }

    private boolean isChakra() {
        return skill && sourceid == ChiefBandit.CHAKRA;
    }

    private boolean isCouponBuff() {
        return ItemId.isRateCoupon(sourceid);
    }

    private boolean isAriantShield() {
        int itemid = sourceid;
        return isAriantShield(itemid);
    }

    private boolean isMysticDoor() {
        return skill && sourceid == Priest.MYSTIC_DOOR;
    }

    public boolean isMonsterRiding() {
        return skill && (sourceid % 10000000 == 1004 || sourceid == Corsair.BATTLE_SHIP || sourceid == Beginner.SPACESHIP || sourceid == Noblesse.SPACESHIP
                || sourceid == Beginner.YETI_MOUNT1 || sourceid == Beginner.YETI_MOUNT2 || sourceid == Beginner.WITCH_BROOMSTICK || sourceid == Beginner.BALROG_MOUNT
                || sourceid == Noblesse.YETI_MOUNT1 || sourceid == Noblesse.YETI_MOUNT2 || sourceid == Noblesse.WITCH_BROOMSTICK || sourceid == Noblesse.BALROG_MOUNT
                || sourceid == Legend.YETI_MOUNT1 || sourceid == Legend.YETI_MOUNT2 || sourceid == Legend.WITCH_BROOMSTICK || sourceid == Legend.BALROG_MOUNT);
    }

    public boolean isMagicDoor() {
        return skill && sourceid == Priest.MYSTIC_DOOR;
    }

    public boolean isPoison() {
        return skill && (sourceid == FPMage.POISON_MIST || sourceid == FPWizard.POISON_BREATH || sourceid == FPMage.ELEMENT_COMPOSITION || sourceid == NightWalker.POISON_BOMB || sourceid == BlazeWizard.FLAME_GEAR);
    }

    public boolean isMorph() {
        return morphId > 0;
    }

    public boolean isMorphWithoutAttack() {
        return morphId > 0 && morphId < 100; // Every morph item I have found has been under 100, pirate skill transforms start at 1000.
    }

    private boolean isMist() {
        return skill && (sourceid == FPMage.POISON_MIST || sourceid == Shadower.SMOKE_SCREEN || sourceid == BlazeWizard.FLAME_GEAR || sourceid == NightWalker.POISON_BOMB || sourceid == Evan.RECOVERY_AURA);
    }

    private boolean isSoulArrow() {
        return skill && (sourceid == Hunter.SOUL_ARROW || sourceid == Crossbowman.SOUL_ARROW || sourceid == WindArcher.SOUL_ARROW);
    }

    private boolean isShadowClaw() {
        return skill && sourceid == NightLord.SHADOW_STARS;
    }

    private boolean isCrash() {
        return skill && (sourceid == DragonKnight.POWER_CRASH || sourceid == Crusader.ARMOR_CRASH || sourceid == WhiteKnight.MAGIC_CRASH);
    }

    private boolean isSeal() {
        return skill && (sourceid == ILMage.SEAL || sourceid == FPMage.SEAL || sourceid == BlazeWizard.SEAL);
    }

    private boolean isDispel() {
        return skill && (sourceid == Priest.DISPEL || sourceid == SuperGM.HEAL_PLUS_DISPEL);
    }

    private boolean isCureAllAbnormalStatus() {
        if (skill) {
            return isHerosWill(sourceid);
        } else {
            return sourceid == ItemId.WHITE_ELIXIR;
        }
    }

    public static boolean isHerosWill(int skillid) {
        switch (skillid) {
            case Hero.HEROS_WILL:
            case Paladin.HEROS_WILL:
            case DarkKnight.HEROS_WILL:
            case FPArchMage.HEROS_WILL:
            case ILArchMage.HEROS_WILL:
            case Bishop.HEROS_WILL:
            case Bowmaster.HEROS_WILL:
            case Marksman.HEROS_WILL:
            case NightLord.HEROS_WILL:
            case Shadower.HEROS_WILL:
            case Buccaneer.PIRATES_RAGE:
            case Aran.HEROS_WILL:
                return true;

            default:
                return false;
        }
    }

    private boolean isWkCharge() {
        if (!skill) {
            return false;
        }

        for (Pair<BuffStat, Integer> p : statups) {
            if (p.getLeft().equals(BuffStat.WK_CHARGE)) {
                return true;
            }
        }

        return false;
    }

    private boolean isDash() {
        return skill && (sourceid == Pirate.DASH || sourceid == ThunderBreaker.DASH || sourceid == Beginner.SPACE_DASH || sourceid == Noblesse.SPACE_DASH);
    }

    private boolean isSkillMorph() {
        return skill && (sourceid == Buccaneer.SUPER_TRANSFORMATION || sourceid == Marauder.TRANSFORMATION || sourceid == WindArcher.EAGLE_EYE || sourceid == ThunderBreaker.TRANSFORMATION);
    }

    private boolean isInfusion() {
        return skill && (sourceid == Buccaneer.SPEED_INFUSION || sourceid == Corsair.SPEED_INFUSION || sourceid == ThunderBreaker.SPEED_INFUSION);
    }

    private boolean isCygnusFA() {
        return skill && (sourceid == DawnWarrior.FINAL_ATTACK || sourceid == WindArcher.FINAL_ATTACK);
    }

    private boolean isHyperBody() {
        return skill && (sourceid == Spearman.HYPER_BODY || sourceid == GM.HYPER_BODY || sourceid == SuperGM.HYPER_BODY);
    }

    private boolean isComboReset() {
        return sourceid == Aran.COMBO_BARRIER || sourceid == Aran.COMBO_DRAIN;
    }

    private int getFatigue() {
        return fatigue;
    }

    private int getMorph() {
        return morphId;
    }

    private int getMorph(Character chr) {
        if (morphId == 1000 || morphId == 1001 || morphId == 1003) { // morph skill
            return chr.getGender() == 0 ? morphId : morphId + 100;
        }
        return morphId;
    }

    private SummonMovementType getSummonMovementType() {
        if (!skill) {
            return null;
        }
        switch (sourceid) {
            case Ranger.PUPPET:
            case Sniper.PUPPET:
            case WindArcher.PUPPET:
            case Outlaw.OCTOPUS:
            case Corsair.WRATH_OF_THE_OCTOPI:
                return SummonMovementType.STATIONARY;
            case Ranger.SILVER_HAWK:
            case Sniper.GOLDEN_EAGLE:
            case Priest.SUMMON_DRAGON:
            case Marksman.FROST_PREY:
            case Bowmaster.PHOENIX:
            case Outlaw.GAVIOTA:
                return SummonMovementType.CIRCLE_FOLLOW;
            case DarkKnight.BEHOLDER:
            case FPArchMage.ELQUINES:
            case ILArchMage.IFRIT:
            case Bishop.BAHAMUT:
            case DawnWarrior.SOUL:
            case BlazeWizard.FLAME:
            case BlazeWizard.IFRIT:
            case WindArcher.STORM:
            case NightWalker.DARKNESS:
            case ThunderBreaker.LIGHTNING:
                return SummonMovementType.FOLLOW;
        }
        return null;
    }

    public boolean isSkill() {
        return skill;
    }

    public int getSourceId() {
        return sourceid;
    }

    public int getBuffSourceId() {
        return skill ? sourceid : -sourceid;
    }

    public boolean makeChanceResult() {
        return prop == 1.0 || Math.random() < prop;
    }

    /*
     private static class CancelEffectAction implements Runnable {

     private StatEffect effect;
     private WeakReference<Character> target;
     private long startTime;

     public CancelEffectAction(Character target, StatEffect effect, long startTime) {
     this.effect = effect;
     this.target = new WeakReference<>(target);
     this.startTime = startTime;
     }

     @Override
     public void run() {
     Character realTarget = target.get();
     if (realTarget != null) {
     realTarget.cancelEffect(effect, false, startTime);
     }
     }
     }
     */
    public short getHp() {
        return hp;
    }

    public short getMp() {
        return mp;
    }

    public double getHpRate() {
        return hpR;
    }

    public double getMpRate() {
        return mpR;
    }

    public byte getHpR() {
        return mhpR;
    }

    public byte getMpR() {
        return mmpR;
    }

    public short getHpRRate() {
        return mhpRRate;
    }

    public short getMpRRate() {
        return mmpRRate;
    }

    public short getHpCon() {
        return hpCon;
    }

    public short getMpCon() {
        return mpCon;
    }

    public short getMatk() {
        return matk;
    }

    public short getWatk() {
        return watk;
    }

    public int getDuration() {
        return duration;
    }

    public List<Pair<BuffStat, Integer>> getStatups() {
        return statups;
    }

    public boolean sameSource(StatEffect effect) {
        return this.sourceid == effect.sourceid && this.skill == effect.skill;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getDamage() {
        return damage;
    }

    public int getAttackCount() {
        return attackCount;
    }

    public int getMobCount() {
        return mobCount;
    }

    public int getFixDamage() {
        return fixdamage;
    }

    public short getBulletCount() {
        return bulletCount;
    }

    public short getBulletConsume() {
        return bulletConsume;
    }

    public int getMoneyCon() {
        return moneyCon;
    }

    public int getCooldown() {
        return cooldown;
    }

    public Map<MonsterStatus, Integer> getMonsterStati() {
        return monsterStatus;
    }
}
