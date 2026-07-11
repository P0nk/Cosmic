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
package client;

public enum Job {
    BEGINNER(0),

    WARRIOR(100),
    FIGHTER(110), CRUSADER(111), HERO(112), HERALD(113),
    PAGE(120), WHITEKNIGHT(121), PALADIN(122), RETRIBUTE(123),
    SPEARMAN(130), DRAGONKNIGHT(131), DARKKNIGHT(132), DARKOMEN(133),

    MAGICIAN(200),
    FP_WIZARD(210), FP_MAGE(211), FP_ARCHMAGE(212), FIREGRANDMASTER(213),
    IL_WIZARD(220), IL_MAGE(221), IL_ARCHMAGE(222), ICEGRANDMASTER(223),
    CLERIC(230), PRIEST(231), BISHOP(232), HOLYGRANDMASTER(233),

    BOWMAN(300),
    HUNTER(310), RANGER(311), BOWMASTER(312), SHARPEYES(313),
    CROSSBOWMAN(320), SNIPER(321), MARKSMAN(322), CURSEDBOLT(323),

    THIEF(400),
    ASSASSIN(410), HERMIT(411), NIGHTLORD(412), VOID(413),
    BANDIT(420), CHIEFBANDIT(421), SHADOWER(422), INVERSE(423),

    PIRATE(500),
    BRAWLER(510), MARAUDER(511), BUCCANEER(512), CAPTAIN(513),
    GUNSLINGER(520), OUTLAW(521), CORSAIR(522), SHARPSHOT(523),

    SUPER_BEGINNER(700), SYNTH_MASTER(710),
    MAPLELEAF_BRIGADIER(800),
    GM(900), SUPERGM(910),

    NOBLESSE(1000),
    DAWNWARRIOR1(1100), DAWNWARRIOR2(1110), DAWNWARRIOR3(1111), DAWNWARRIOR4(1112), DAWNWARRIOR5(1113),
    BLAZEWIZARD1(1200), BLAZEWIZARD2(1210), BLAZEWIZARD3(1211), BLAZEWIZARD4(1212), BLAZEWIZARD5(1213),
    WINDARCHER1(1300), WINDARCHER2(1310), WINDARCHER3(1311), WINDARCHER4(1312), WINDARCHER5(1313),
    NIGHTWALKER1(1400), NIGHTWALKER2(1410), NIGHTWALKER3(1411), NIGHTWALKER4(1412), NIGHTWALKER5(1413),
    THUNDERBREAKER1(1500), THUNDERBREAKER2(1510), THUNDERBREAKER3(1511), THUNDERBREAKER4(1512), THUNDERBREAKER5(1513),

    LEGEND(2000), EVAN(2001),
    ARAN1(2100), ARAN2(2110), ARAN3(2111), ARAN4(2112), AEGIS(2113),

    EVAN1(9900), EVAN2(9910), EVAN3(9911), EVAN4(9912), EVAN5(9913), EVAN6(9914),
    EVAN7(9915), EVAN8(9916), EVAN9(9917), EVAN10(9918);

    final int jobid;
    final static int maxId = 22;    // maxId = (EVAN / 100);

    Job(int id) {
        jobid = id;
    }

    public static int getMax() {
        return maxId;
    }

    public int getId() {
        return jobid;
    }

    public static Job getById(int id) {
        for (Job l : Job.values()) {
            if (l.getId() == id) {
                return l;
            }
        }
        return null;
    }

    public static Job getBy5ByteEncoding(int encoded) {
        switch (encoded) {
            case 2:
                return WARRIOR;
            case 4:
                return MAGICIAN;
            case 8:
                return BOWMAN;
            case 16:
                return THIEF;
            case 32:
                return PIRATE;
            case 1024:
                return NOBLESSE;
            case 2048:
                return DAWNWARRIOR1;
            case 4096:
                return BLAZEWIZARD1;
            case 8192:
                return WINDARCHER1;
            case 16384:
                return NIGHTWALKER1;
            case 32768:
                return THUNDERBREAKER1;
            default:
                return BEGINNER;
        }
    }

    public boolean isA(Job basejob) {  // hanks Steve (kaito1410) for pointing out an improvement here
        int basebranch = basejob.getId() / 10;
        return (getId() / 10 == basebranch && getId() >= basejob.getId()) || (basebranch % 10 == 0 && getId() / 100 == basejob.getId() / 100);
    }

    public int getJobNiche() {
        return (jobid / 100) % 10;
        
        /*
        case 0: BEGINNER;
        case 1: WARRIOR;
        case 2: MAGICIAN;
        case 3: BOWMAN;  
        case 4: THIEF;
        case 5: PIRATE;
        */
    }
}
