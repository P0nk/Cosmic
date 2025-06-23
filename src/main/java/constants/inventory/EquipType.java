/*
    This file is part of the HeavenMS MapleStory Server
    Copyleft (L) 2016 - 2019 RonanLana

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
package constants.inventory;

import client.inventory.ArmorType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author RonanLana
 */
public enum EquipType {
    UNDEFINED(-1),
    ACCESSORY(0),
    CAP(100),
    CAPE(110),
    COAT(104),
    FACE(2),
    GLOVES(108),
    HAIR(3),
    LONGCOAT(105),
    PANTS(106),
    PET_EQUIP(180),
    PET_EQUIP_FIELD(181),
    PET_EQUIP_LABEL(182),
    PET_EQUIP_QUOTE(183),
    RING(111),
    SHIELD(109),
    SHOES(107),
    TAMING(190),
    TAMING_SADDLE(191),
    SWORD(1302),
    AXE(1312),
    MACE(1322),
    DAGGER(1332),
    WAND(1372),
    STAFF(1382),
    SWORD_2H(1402),
    AXE_2H(1412),
    MACE_2H(1422),
    SPEAR(1432),
    POLEARM(1442),
    BOW(1452),
    CROSSBOW(1462),
    CLAW(1472),
    KNUCKLER(1482),
    PISTOL(1492);

    // Categories
    public static final int CATEGORY_HELMET = 100;
    public static final int CATEGORY_FACE_ACCESSORY = 101;
    public static final int CATEGORY_EYE_ACCESSORY = 102;
    public static final int CATEGORY_EARRING = 103;
    public static final int CATEGORY_TOPWEAR = 104;
    public static final int CATEGORY_OVERALL = 105;
    public static final int CATEGORY_BOTTOMWEAR = 106;
    public static final int CATEGORY_SHOES = 107;
    public static final int CATEGORY_GLOVES = 108;
    public static final int CATEGORY_SHIELD = 109;
    public static final int CATEGORY_CAPE = 110;
    public static final int CATEGORY_RING = 111;
    public static final int CATEGORY_PENDANT = 112;
    public static final int CATEGORY_BELT = 113;
    public static final int CATEGORY_PET_EQUIP = 180;

    private final int i;
    private static final Map<Integer, EquipType> map = new HashMap(34);
    private final Map<Integer, List<Integer>> scrollsByEquipType = new HashMap<>();

    EquipType(int val) {
        this.i = val;
    }

    public int getValue() {
        return i;
    }

    static {
        for (EquipType eqEnum : EquipType.values()) {
            map.put(eqEnum.i, eqEnum);
        }
    }

    public static EquipType getEquipTypeById(int itemid) {
        EquipType ret;
        int val = itemid / 100000;

        if (val == 13 || val == 14) {
            ret = map.get(itemid / 1000);
        } else {
            ret = map.get(itemid / 10000);
        }

        return (ret != null) ? ret : EquipType.UNDEFINED;
    }

    public List<Integer> getScrollsByItemId(int itemId) {

        int equipCategory = (itemId / 10000);

        // Check cache first
        if (scrollsByEquipType.containsKey(equipCategory)) {
            return scrollsByEquipType.get(equipCategory);
        }

        List<Integer> ret = new ArrayList<>();
        switch (equipCategory) {
            case CATEGORY_HELMET -> {
                ret.add(2040000); // Scroll for Helmet for DEF - Improves the helmet's weapon def.\nSuccess rate:100%, weapon def. +1
                ret.add(2040001); // Scroll for Helmet for DEF - Improves helmet def.\nSuccess rate:60%, weapon def.+2, magic def., +2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040002); // Scroll for Helmet for DEF - Improves helmet def.\nSuccess Rate:10%, weapon def.+5, magic def.+3, accuracy+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040003); // Scroll for Helmet for HP - Improves MaxHP on hats.\nSuccess rate:100%, MaxHP+5
                ret.add(2040004); // Scroll for Helmet for HP - Improves MaxHP on hats.\nSuccess rate:60%, MaxHP+10. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040005); // Scroll for Helmet for HP - Improves MaxHP on hats.\nSuccess rate:10%, MaxHP+30. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040006); // Scroll for Helmet for DEF - Improves helmet def.\nSuccess rate:100%, weapon def.+5, magic def.+3, accuracy+1
                ret.add(2040007); // Scroll for Helmet for HP - Improves MaxHP on hats.\nSuccess rate:100%, MaxHP+30
                ret.add(2040016); // Scroll for Helmet for Accuracy - Improves the helmet's accuracy option.\nSuccess Rate 10%, Dex+2, Accuracy +4. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040017); // Scroll for Helmet for Accuracy - Improves the helmet's accuracy option.\nSuccess Rate 60%, Dex+1, Accuracy +2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040018); // Scroll for Helmet for Accuracy - Improves the helmet's accuracy option.\nSuccess Rate 100%, Accuracy +1
                ret.add(2040019); // Scroll for Helmet for DEF - Improves Weapon Defense on a Helmet.\nSuccess rate: 65%, Weapon Def. +2, Magic Def. +1
                ret.add(2040020); // Scroll for Helmet for DEF - Improves Weapon Defense on a Helmet.\nSuccess rate: 15%, Weapon Def.+5, Magic Def.+3, Accuracy+1
                ret.add(2040021); // Scroll for Helmet for MaxHP - Improves MaxHP on a Helmet.\nSuccess rate: 65%, MaxHP +10
                ret.add(2040022); // Scroll for Helmet for MaxHP - Improves MaxHP on a Helmet.\nSuccess rate: 15%, MaxHP +30
                ret.add(2040023); // Scroll for Rudolph's Horn 60% - Increases the weapon attack and magic attack of Rudolph's Horn.\nSuccess rate:60%, attack +1, magic att. +1
                ret.add(2040024); // Scroll for Helmet for INT 100% - Improves INT on headwear..Success rate 100%, INT+1
                ret.add(2040025); // Scroll for Helmet for INT 60% - Improves INT on headwear.\nSuccess rate 60%, INT+2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040026); // Scroll for Helmet for INT 10% - Improves INT on headwear.\nSuccess rate 10%, INT+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040027); // Scroll for Helmet for DEX 100% - Improves DEX on headwear..Success rate 100%, DEX+1
                ret.add(2040029); // Scroll for Helmet for DEX 60% - Improves DEX on headwear.\nSuccess rate 60%, DEX+2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040031); // Scroll for Helmet for DEX 10% - Improves DEX on headwear.\nSuccess rate 10%, DEX+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040041); // Scroll for Helmet for DEF 100% - Improves helmet def.\nSuccess rate:100%, weapon def.+2, magic def., +3
                ret.add(2040042); // Scroll for Helmet for HP 100% - Improves MaxHP on hats.\nSuccess rate:100%, MaxHP+15
                ret.add(2040043); // Scroll for Helmet for DEX 65% - Improves DEX on Helmets.\nSuccess Rate 65%, DEX+2
                ret.add(2040044); // Scroll for Helmet for DEX 15% - Improves DEX on Helmets.\nSuccess Rate 15%, DEX+3
                ret.add(2040045); // Scroll for Helmet for DEF 50% - Improves helmet def.\nSuccess Rate:50%, weapon def.+5, magic def.+4
                ret.add(2040046); // Scroll for Helmet for HP 50% - Improves MaxHP on hats.\nSuccess rate:50%, MaxHP+35
            }
            case CATEGORY_FACE_ACCESSORY -> {
                ret.add(2040100); // Scroll for Face Accessory for HP - Improves MaxHP on face accessories.\nSuccess rate:10%, MaxHP +30. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040101); // Scroll for Face Accessory for HP - Improves MaxHP on face accessories.\nSuccess rate:60%, MaxHP +15. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040102); // Scroll for Face Accessory for HP - Improves MaxHP on face accessories.\nSuccess rate:100%, MaxHP +5
                ret.add(2040105); // Scroll for Face Accessory for Avoidability - Improves avoidability on face accessories.\nSuccess rate:10%, Avoidability +2, DEX +2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040106); // Scroll for Face Accessory for Avoidability - Improves avoidability on face accessories.\nSuccess rate:60%, Avoidability +1, DEX +1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040107); // Scroll for Face Accessory for Avoidability - Improves avoidability on face accessories.\nSuccess rate:100%, Avoidability +1

            }
            case CATEGORY_EYE_ACCESSORY -> {
                ret.add(2040200); // Scroll for Eye Accessory for Accuracy - Improves accuracy on eye accessories.\nSuccess rate:10%, Accuracy +3, DEX +1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040201); // Scroll for Eye Accessory for Accuracy - Improves accuracy on eye accessories.\nSuccess rate:60%, Accuracy +2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040202); // Scroll for Eye Accessory for Accuracy - Improves accuracy on eye accessories.\nSuccess rate:100%, Accuracy +1
                ret.add(2040205); // Scroll for Eye Accessory for INT - Improves INT on eye accessories.\nSuccess rate:10%, INT +3, Magic Def. +2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040206); // Scroll for Eye Accessory for INT - Improves INT on eye accessories.\nSuccess rate:60%, INT +1, Magic Def. +1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040207); // Scroll for Eye Accessory for INT - Improves INT on eye accessories.\nSuccess rate:100%, INT +1
            }
            case CATEGORY_EARRING -> {
                ret.add(2040300); // Scroll for Earring for INT - Improves INT on ear accessory.\nSuccess rate:100%, magic attack+1
                ret.add(2040301); // Scroll for Earring for INT - Improves INT on ear accessory.\nSuccess rate:60%, magic attack +2, INT+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040302); // Scroll for Earring for INT - Improves INT on ear accessory.\nSuccess rate:10%, magic attack +5, INT+3, magic def. +1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040303); // Scroll for Earring for INT - Improves INT on ear accessory.\nSuccess rate:30%, magic attack +5, INT+3, magic def. +1
                ret.add(2040310); // Scroll for Earring for DEF - Improves DEF on earrings.\nSuccess Rate 10%, weapon defense+3, magic defense+3, Accuracy+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040311); // Scroll for Earring for DEF - Improves DEF on earrings.\nSuccess Rate 60%, weapon defense+1, magic defense+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040312); // Scroll for Earring for DEF - Improves DEF on earringsnSuccess Rate 100%, weapon defense+1
                ret.add(2040313); // Scroll for Earring for INT - Improves INT on Earrings.\nSuccess rate: 65%, Magic Attack +2, INT+1
                ret.add(2040314); // Scroll for Earring for INT - Improves INT on Earrings.\nSuccess rate:15%, Magic Attack +5, INT +3, Magic Def. +1
                ret.add(2040316); // Scroll for Earring for DEX 100% - Improves DEX on earrings..\nSuccess rate:100%, DEX+1
                ret.add(2040317); // Scroll for Earring for DEX 60% - Improves DEX on earrings.\nSuccess rate:60%, DEX+2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040318); // Scroll for Earring for DEX 10% - Improves DEX on earrings.\nSuccess rate:10%, DEX+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040319); // Scroll for Earring for LUK 100% - Improves LUK on earrings..\nSuccess rate:100%, LUK+1
                ret.add(2040321); // Scroll for Earring for LUK 60% - Improves LUK on earrings.\nSuccess rate:60%, LUK+2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040323); // Scroll for Earring for LUK 10% - Improves LUK on earrings.\nSuccess rate:10%, LUK+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040324); // Scroll for Earring for HP 100% - Improves HP on earrings..\nSuccess rate:100%, MaxHP+5
                ret.add(2040326); // Scroll for Earring for HP 60% - Improves HP on earrings.\nSuccess rate:60%, MaxHP+15. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040328); // Scroll for Earring for HP 10% - Improves HP on earrings.\nSuccess rate:10%, MaxHP+30. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040329); // Scroll for Earring for DEX 10% - Improves DEX on earrings. nSuccess rate: 10%, Dex +3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040330); // Scroll for Earring for INT 10% - Improves INT on earrings. nSuccess rate: 10%, Magic ATT +5, INT +3, Magic Defense +1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040331); // Scroll for Earring for LUK 10% - Improves LUK on earrings. nSuccess rate: 10%, LUK +3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040333); // Scroll for Earring for INT 50% - Improves INT on ear accessory.\nSuccess rate:50%, magic attack +5, INT+3, magic def. +2
                ret.add(2040334); // Scroll for Earring for INT 100% - Improves INT on ear accessory.\nSuccess rate:100%, magic attack +2, INT+2
                ret.add(2040335); // Scroll for Earring for DEX 65% - Improves DEX on Earrings.\nSuccess Rate: 65%, DEX+2
                ret.add(2040336); // Scroll for Earring for DEX 15% - Improves DEX on Earrings.\nSuccess Rate: 15%, DEX+3
                ret.add(2040337); // Scroll for Earring for LUK 65% - Improves LUK on Earrings.\nSuccess Rate: 65%, LUK+2
                ret.add(2040338); // Scroll for Earring for LUK 15% - Improves LUK on Earrings.\nSuccess Rate: 15%, LUK+3
                ret.add(2040339); // Scroll for Earring for HP 65% - Improves HP on Earrings.\nSuccess Rate: 65%, MaxHP+15
                ret.add(2040340); // Scroll for Earring for HP 15% - Improves HP on Earrings.\nSuccess Rate: 15%, MaxHP+30
            }
            case CATEGORY_TOPWEAR -> {
                ret.add(2040400); // Scroll for Topwear for DEF - Improves weapon def. on topwear.\nSuccess rate:100%, weapon def.+1
                ret.add(2040401); // Scroll for Topwear for DEF - Improves weapon def. on topwear.\nSuccess rate:60%, weapon def.+2, magic def.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040402); // Scroll for Topwear for DEF - Improves weapon def. on topwear.\nSuccess rate:10%, weapon def. +5, magic def. +3, MaxHP+10. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040403); // Scroll for Topwear for DEF - Improves weapon def. on topwear.\nSuccess rate:100%, weapon def. +5, magic def. +3, MaxHP+10
                ret.add(2040412); // Scroll for Topwear for LUK - Improves LUK on the topwear.\nSuccess Rate 10%, LUK+3, avoidability+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040413); // Scroll for Topwear for LUK - Improves LUK on the topwear.\nSuccess Rate 60%, LUK+2, avoidability+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040414); // Scroll for Topwear for LUK - Improves LUK on the topwear.\nSuccess Rate 100%, LUK+1
                ret.add(2040415); // Scroll for Topwear for DEF - Improves Weapon Def. on Topwear.\nSuccess rate: 65%, Weapon Def. +2, Magic Def. +1
                ret.add(2040416); // Scroll for Topwear for DEF - Improves Weapon Def. on Topwear.\nSuccess rate: 15%, Weapon Def. +5, Magic Def. +3, MaxHP +10
                ret.add(2040417); // Scroll for Topwear for STR 100% - Improves strength on topwear..Success rate 100%, STR+1
                ret.add(2040418); // Scroll for Topwear for STR 60% - Improves strength on topwear.\nSuccess rate 60%, STR+2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040419); // Scroll for Topwear for STR 10% - Improves strength on topwear.\nSuccess rate 10%, STR+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040420); // Scroll for Topwear for HP 100% - Improves HP on topwear..Success rate 100%, MaxHP + 5
                ret.add(2040421); // Scroll for Topwear for HP 60% - Improves HP on topwear.\nSuccess rate 60%, MaxHP + 15. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040422); // Scroll for Topwear for HP 10% - Improves HP on topwear.\nSuccess rate 10%, MaxHP + 30. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040423); // Scroll for Topwear for LUK 100% - Improves luck on topwear..\nSuccess rate:100%, LUK+1
                ret.add(2040425); // Scroll for Topwear for LUK 60% - Improves luck on topwear.\nSuccess rate:60%, LUK+2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040427); // Scroll for Topwear for LUK 10% - Improves luck on topwear.\nSuccess rate:10%, LUK+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040429); // Scroll for Topwear for DEF 50% - Improves weapon def. on topwear.\nSuccess rate:50%, weapon def. +5, magic def. +4
                ret.add(2040430); // Scroll for Topwear for DEF 100% - Improves weapon def. on topwear.\nSuccess rate:100%, weapon def.+2, magic def.+3
                ret.add(2040431); // Scroll for Topwear for STR 65% - Improves STR on Topwear.\nSuccess Rate 65%, STR+2
                ret.add(2040432); // Scroll for Topwear for STR 15% - Improves STR on Topwear.\nSuccess Rate 15%, STR+3
                ret.add(2040433); // Scroll for Topwear for HP 65% - Improves HP on Topwear.\nSuccess Rate 65%, MaxHP + 15
                ret.add(2040434); // Scroll for Topwear for HP 15% - Improves HP on Topwear.\nSuccess Rate 15%, MaxHP + 30
                ret.add(2040435); // Scroll for Topwear for LUK 65% - Improves LUK on Topwear.\nSuccess Rate: 65%, LUK+2
                ret.add(2040436); // Scroll for Topwear for LUK 15% - Improves LUK on Topwear.\nSuccess Rate: 15%, LUK+3
            }
            case CATEGORY_OVERALL -> {
                ret.add(2040500); // Scroll for Overall Armor for DEX - Improves dexterity on the overall armor.\nSuccess rate:100%, DEX+1
                ret.add(2040501); // Scroll for Overall Armor for DEX - Improves dexterity on the overall armor.\nSuccess rate:60%, DEX+2, accuracy+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040502); // Scroll for Overall Armor for DEX - Improves dexterity on the overall armor.\nSuccess rate:10%, DEX+5, accuracy+3, speed+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040503); // Scroll for Overall Armor for DEF - Improves weapon def. on the overall armor.\nSuccess rate:100%, weapon def.+1
                ret.add(2040504); // Scroll for Overall Armor for DEF - Improves def. on the overall armor.\nSuccess rate:60%, weapon def.+2, magic def.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040505); // Scroll for Overall Armor for DEF - Improves def. on the overall armor.\nSuccess rate:10%, wepon def. +5, magic def. +3, MaxHP+10. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040506); // Scroll for Overall Armor for DEX - Improves dexterity on the overall armor.\nSuccess rate:100%, DEX+5, accuracy+3, speed+1
                ret.add(2040507); // Scroll for Overall Armor for DEF - Improves weapon def. on the overall armor.\nSuccess rate:30%, weapon def.+5, magic def.+3, MaxHP+10
                ret.add(2040512); // Scroll for Overall Armor for INT - Improves INT on the overall armor.\nSuccess rate: 100%, INT + 1
                ret.add(2040513); // Scroll for Overall Armor for INT - Improves INT on the overall armor.\nSuccess rate: 60%, INT + 2, magic def. +1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040514); // Scroll for Overall Armor for INT - Improves INT on the overall armor.\nSuccess rate: 10%, INT + 5, magic def. + 3, MaxMP + 10. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040515); // Scroll for Overall Armor for LUK - Improves LUK on the overall armor.\nSuccess rate: 100%, LUK + 1
                ret.add(2040516); // Scroll for Overall Armor for LUK - Improves LUK on the overall armor.\nSuccess rate: 60%, LUK + 2, avoidability + 1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040517); // Scroll for Overall Armor for LUK - Improves LUK on the overall armor.\nSuccess rate: 10%, LUK + 5, avoidability + 3, accuracy + 1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040522); // Scroll for Overall Armor for DEX - Improves DEX on Overall Armor.\nSuccess rate: 65%, DEX +2, Accuracy +1
                ret.add(2040523); // Scroll for Overall Armor for DEX - Improves DEX on Overall Armor.\nSuccess rate: 15%, DEX +5, Accuracy+3, Speed +1
                ret.add(2040524); // Overall Armor Scroll for DEF - Improves Weapon Def. on Overall Armor.\nSuccess rate: 65%, Weapon Def. +2, Magic Def. +1
                ret.add(2040525); // Overall Armor Scroll for DEF - Improves Weapon Def. on Overall Armor.\nSuccess rate: 15%, Weapon Def. +5, Magic Def. +3, MaxHP +10
                ret.add(2040526); // Scroll for Overall Armor for INT - Improves INT on the Overall Armor.\nSuccess rate: 65%, INT +2, Magic Def. +1
                ret.add(2040527); // Scroll for Overall Armor for INT - Improves INT on the overall armor.\nSuccess rate:15%, INT+5, magic def.+3, MaxMP+10
                ret.add(2040528); // Scroll for Overall Armor for LUK - Improves LUK on the overall armor.\nSuccess rate:65%, LUK+2, avoidability+1
                ret.add(2040529); // Scroll for Overall Armor for LUK - Improves LUK on the overall armor.\nSuccess rate:15%, LUK+5, avoidability+3, accuracy+1
                ret.add(2040530); // Scroll for Overall for STR 100% - Improves strength on overalls..\nSuccess rate:100%, STR+1
                ret.add(2040532); // Scroll for Overall for STR 60% - Improves strength on overalls.\nSuccess rate:60%, STR+2, weapon def.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040534); // Scroll for Overall for STR 10% - Improves strength on overalls.\nSuccess rate:10%, STR+5, weapon def.+3, MaxHP+5. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040538); // Scroll for Overall Armor for DEX 100% - Improves dexterity on the overall armor.\nSuccess rate:100%, DEX+2, accuracy+3
                ret.add(2040539); // Scroll for Overall Armor for DEF 100% - Improves def. on the overall armor.\nSuccess rate:100%, weapon def.+2, magic def.+3
                ret.add(2040540); // Scroll for Overall Armor for STR 65% - Improves STR on Overall Armor.\nSuccess Rate: 65%, STR+2, Weapon Attack+1
                ret.add(2040541); // Scroll for Overall Armor for STR 15% - Improves STR on Overall Armor.\nSuccess Rate: 15%, STR+5, Weapon Attack+3, MaxHP+5
                ret.add(2040542); // Scroll for Overall Armor for DEX 50% - Improves dexterity on the overall armor.\nSuccess rate:50%, DEX+5, avoidability+1, speed+1
                ret.add(2040543); // Scroll for Overall Armor for DEF 50% - Improves def. on the overall armor.\nSuccess rate:50%, wepon def. +5, magic def. +4
            }
            case CATEGORY_BOTTOMWEAR -> {
                ret.add(2040600); // Scroll for Bottomwear for DEF - Improves weapon def. on the bottomwear. nSuccess rate:100%, weapon def. +1
                ret.add(2040601); // Scroll for Bottomwear for DEF - Improves weapon def. on the bottomwear.\nSuccess rate:60%, weapon def. +2, magic def. +1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040602); // Scroll for Bottomwear for DEF - Improves weapon def. on the bottomwear.\nSuccess rate:10%, weapon def.+5, magic def.+3, MaxHP+10. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040603); // Scroll for Bottomwear for DEF - Improves weapon def. on the bottomwear.\nSuccess rate:100%, weapon def.+5, magic def.+3, MaxHP+10
                ret.add(2040612); // Scroll for Bottomwear for DEX - Improves dexterity on the bottomwear.\nSuccess Rate 10%, DEX+3, speed+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040613); // Scroll for Bottomwear for DEX - Improves dexterity on the bottomwear.\nSuccess Rate 60%, DEX+2, speed+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040614); // Scroll for Bottomwear for DEX - Improves dexterity on the bottomwear.\nSuccess Rate 100%, DEX+1
                ret.add(2040615); // Scroll for Bottomwear for DEF - Improves weapon def. on bottomwear.\nSuccess rate:65%, weapon def.+2, magic def.+1
                ret.add(2040616); // Scroll for Bottomwear for DEF - Improves weapon def. on bottomwear.\nSuccess rate:15%, weapon def.+5, magic def.+3, MaxHP+10
                ret.add(2040617); // Scroll for Bottomwear for Jump 100% - Improves jumping abilities on bottomwears..\nSuccess rate:100%, jump+1
                ret.add(2040618); // Scroll for Bottomwear for Jump 60% - Improves jumping abilities on bottomwears.\nSuccess rate:60%, jump+2, avoidability+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040619); // Scroll for Bottomwear for Jump 10% - Improves jumping abilities on bottomwears..\nSuccess rate:10%, jump+4, avoidability+2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040620); // Scroll for Bottomwear for HP 100% - Improves HP on bottomwears..\nSuccess rate:100%, MaxHP+5
                ret.add(2040621); // Scroll for Bottomwear for HP 60% - Improves HP on bottomwears.\nSuccess rate:60%, MaxHP+15. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040622); // Scroll for Bottomwear for HP 10% - Improves HP on bottomwears.\nSuccess rate:10%, MaxHP+30. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040623); // Scroll for Bottomwear for DEX 100% - Improves dexterity on bottomwears..\nSuccess rate:100%, DEX+1
                ret.add(2040625); // Scroll for Bottomwear for DEX 60% - Improves dexterity on bottomwears.\nSuccess rate:60%, DEX+2, accuracy+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040627); // Scroll for Bottomwear for DEX 10% - Improves dexterity on bottomwears.\nSuccess rate:10%, DEX+3, accuracy+2, speed+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040629); // Scroll for Bottomwear for DEF 50% - Improves weapon def. on the bottomwear.\nSuccess rate:50%, weapon def.+5, magic def.+4
                ret.add(2040630); // Scroll for Bottomwear for DEF 100% - Improves weapon def. on the bottomwear.\nSuccess rate:100%, weapon def. +2, magic def. +3
                ret.add(2040631); // Scroll for Bottomwear for Jump 65% - Improves Jump on Bottomwear.\nSuccess Rate: 65%, Jump+2, Avoidability+1
                ret.add(2040632); // Scroll for Bottomwear for Jump 15% - Improves Jump on Bottomwear.\nSuccess Rate: 15%, Jump+4, Avoidability+2
                ret.add(2040633); // Scroll for Bottomwear for HP 65% - Improves HP on Bottomwear.\nSuccess Rate: 65%, MaxHP+15
                ret.add(2040634); // Scroll for Bottomwear for HP 15% - Improves HP on Bottomwear.\nSuccess Rate: 15%, MaxHP+30
                ret.add(2040635); // Scroll for Bottomwear for DEX 65% - Improves DEX on Bottomwear.\nSuccess Rate: 65%, DEX+2, Accuracy+1
                ret.add(2040636); // Scroll for Bottomwear for DEX 15% - Improves DEX on Bottomwear.\nSuccess Rate: 15%, DEX+3, Accuracy+2, Speed+1
            }
            case CATEGORY_SHOES -> {
                ret.add(2040700); // Scroll for Shoes for DEX - Improves dexterity on shoes.\nSuccess rate:100%, Avoidability+1
                ret.add(2040701); // Scroll for Shoes for DEX - Improves dexterity on shoes.\nSuccess rate:60%, Avoidability +2, Accuracy+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040702); // Scroll for Shoes for DEX - Improves dexterity on shoes.\nSuccess rate:10%, Avoidability +5, accuracy +3, speed+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040703); // Scroll for Shoes for Jump - Improves jump on shoes.\nSuccess rate:100%, jump +1
                ret.add(2040704); // Scroll for Shoes for Jump - Improves jump on shoes.\nSuccess rate: 60%, jump +2, DEX+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040705); // Scroll for Shoes for Jump - Improves jump on shoes.\nSuccess rate:10%, jump+5, DEX+3, speed+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040706); // Scroll for Shoes for Speed - Improves speed on shoes.\nSuccess rate:100%, speed+1
                ret.add(2040707); // Scroll for Shoes for Speed - Improves speed on shoes.\nSuccess rate:60%, speed+2
                ret.add(2040708); // Scroll for Shoes for Speed - Improves speed on shoes.\nSuccess rate:10%, speed+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040709); // Scroll for Shoes for DEX - Improves DEX on shoes.\nSuccess rate:100%, avoidability+5, accuracy+3, speed+1
                ret.add(2040710); // Scroll for Shoes for Jump - Improves jump on shoes.\nSuccess rate:100%, jump+5, DEX+3, speed+1
                ret.add(2040711); // Scroll for Shoes for Speed - Improves speed on shoes.\nSuccess rate:100%, speed+3
                ret.add(2040718); // Scroll for Shoes for DEX - Improves dexterity on shoes.\nSuccess rate:65%, avoidability+2, accuracy+1
                ret.add(2040719); // Scroll for Shoes for DEX - Improves dexterity on shoes.\nSuccess rate:15%, avoidability+5, accuracy+3, speed+1
                ret.add(2040720); // Scroll for Shoes for Jump - Improves jump on shoes.\nSuccess rate:65%, jump+2, DEX+1
                ret.add(2040721); // Scroll for Shoes for Jump - Improves jump on shoes.\nSuccess rate:15%, jump+5, DEX+3, speed+1
                ret.add(2040722); // Scroll for Shoes for Speed - Improves speed on shoes.\nSuccess rate:65%, speed+2
                ret.add(2040723); // Scroll for Shoes for Speed - Improves speed on shoes.\nSuccess rate:15%, speed+3
                ret.add(2040727); // Scroll for Spikes on Shoes 10% - Adds traction to the shoes, which prevents the shoes from slipping on slippery surface.\nSuccess rate:10%, Does not affect the number of upgrades available. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040740); // Scroll for Shoes for DEX 100% - Improves dexterity on shoes.\nSuccess rate:100%, Avoidability +2, Accuracy+3
                ret.add(2040741); // Scroll for Shoes for Jump 100% - Improves jump on shoes.\nSuccess rate: 100%, jump +2, DEX+2
                ret.add(2040742); // Scroll for Shoes for Speed 100% - Improves speed on shoes.\nSuccess rate:100%, speed+2
                ret.add(2040755); // Scroll for Shoes for DEX 50% - Improves dexterity on shoes.\nSuccess rate:50%, Avoidability +3, accuracy +3, speed+2
                ret.add(2040756); // Scroll for Shoes for Jump 50% - Improves jump on shoes.\nSuccess rate:50%, jump+6, speed+1
                ret.add(2040757); // Scroll for Shoes for Speed 50% - Improves speed on shoes.\nSuccess rate:50%, speed+3, jump+1
                ret.add(2040758); // Scroll for Shoes for ATT - Improves attack on shoes.\nSuccess rate: 100%. Weapon Attack +1
                ret.add(2040759); // Scroll for Shoes for ATT - Improves attack on shoes.\nSuccess rate: 60%. Weapon Attack +2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040760); // Scroll for Shoes for ATT - Improves attack on shoes.\nSuccess rate: 10%, Weapon Attack +3. The success rate of this scroll can be enhanced by Vega's Spell.
            }
            case CATEGORY_GLOVES -> {
                ret.add(2040800); // Scroll for Gloves for DEX - Improves dexterity on gloves.\nSuccess rate:100%, accurcacy +1
                ret.add(2040801); // Scroll for Gloves for DEX - Improves dexterity on gloves.\nSuccess rate: 60%, accuracy+2, DEX+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040802); // Scroll for Gloves for DEX - Improves dexterity on gloves.\nSuccess rate:10%, accuracy+5, DEX+3, avoidability+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040803); // Scroll for Gloves for ATT - Improves attack on gloves.\nSuccess rate:100%, weapon att. +1
                ret.add(2040804); // Scroll for Gloves for ATT - Improves attack on gloves.\nSuccess rate 60%, weapon att. +2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040805); // Scroll for Gloves for ATT - Improves attack on gloves.\nSuccess rate:10%, weapon att.+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040806); // Scroll for Gloves for DEX - Improves DEX on the glove.\nSuccess rate:100%, accuracy+5, DEX+3, avoidability+1
                ret.add(2040807); // Scroll for Gloves for ATT - Improves weapon att. on the glove.\nSuccess rate:100%, weapon att.+3
                ret.add(2040816); // Scroll for Gloves for Magic Att. - Improves magic attack on the glove.\nSuccess Rate 10%, magic defense+1, magic attack+3, INT+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040817); // Scroll for Gloves for Magic Att. - Improves magic attack on the glove.\nSuccess Rate 60%, magic attack+1, INT+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040818); // Scroll for Gloves for Magic Att. - Improves magic attack on the glove.\nSuccess Rate 100%, magic attack+1
                ret.add(2040819); // Scroll for Gloves for DEX - Improves dexterity on gloves.\nSuccess rate:65%, accuracy+2, DEX+1
                ret.add(2040820); // Scroll for Gloves for DEX - Improves dexterity on gloves.\nSuccess rate:15%, accuracy+5, DEX+3, avoidability+1
                ret.add(2040821); // Scroll for Gloves for ATT - Improves attack on gloves.\nSuccess rate:65%, weapon attack+2
                ret.add(2040822); // Scroll for Gloves for ATT - Improves attack on gloves.\nSuccess rate:15%, weapon attack+3
                ret.add(2040823); // Scroll for Gloves for HP 100% - Improves HP on gloves..\nSuccess rate:100%, MaxHP+5
                ret.add(2040824); // Scroll for Gloves for HP 60% - Improves HP on gloves.\nSuccess rate:60%, MaxHP+15. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040825); // Scroll for Gloves for HP 10% - Improves HP on gloves.\nSuccess rate:10%, MaxHP+30. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040826); // Scroll for Gloves for ATT 60% - Improves ATT on Gloves.\nSuccess rate: 60%, Weapons ATT +2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040829); // Scroll for Gloves for DEX 100% - Improves dexterity on gloves.\nSuccess rate: 100%, accuracy+2, DEX+2
                ret.add(2040830); // Scroll for Gloves for ATT 100% - Improves attack on gloves.\nSuccess rate 100%, weapon att. +2
                ret.add(2040831); // Scroll for Gloves for HP 65% - Improves HP on Gloves.\nSuccess Rate: 65%, MaxHP+15
                ret.add(2040832); // Scroll for Gloves for HP 15% - Improves HP on Gloves.\nSuccess Rate: 15%, MaxHP+30
                ret.add(2040833); // Scroll for Gloves for DEX 50% - Improves dexterity on gloves.\nSuccess rate:50%, accuracy+3, DEX+3, avoidability+2
                ret.add(2040834); // Scroll for Gloves for ATT 50% - Improves attack on gloves.\nSuccess rate:50%, weapon att.+3
            }
            case CATEGORY_SHIELD -> {
                ret.add(2040900); // Scroll for Shield for DEF - Improves weapon def. on the shield.\nSuccess rate:100%, weapon def. +1
                ret.add(2040901); // Scroll for Shield for DEF - Improves weapon def. on the shield.\nSuccess rate:60%, weapon def.+2, magic def.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040902); // Scroll for Shield for DEF - Improves weapon def. on the shield.\nSuccess rate 10%, weapon def.+5, magic def.+3, MaxHP+10. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040903); // Scroll for Shield for DEF - Improves weapon def. on the shield.\nSuccess rate 100%, weapon def.+5, magic def.+3, MaxHP+10
                ret.add(2040910); // Scroll for Shield for DEF - Improves weapon defense on the shield.\nSuccess rate:65%, weapon def.+2, magic def.+1
                ret.add(2040911); // Scroll for Shield for DEF - Improves weapon defense on the shield.\nSuccess rate:15%, weapon def.+5, magic def.+3, MaxHP+10
                ret.add(2040914); // Scroll for Shield for Weapon Att. - Improves weapon attack on the shield.\nSuccess Rate 60%, W. attack+2, STR+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040915); // Scroll for Shield for Weapon Att. - Improves weapon attack on the shield.\nSuccess Rate 10%, W. attack+3, STR+2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040918); // Scroll for Shield for Magic Att. - Improves magic attack on the shield.\nSuccess Rate 100%, magic attack+1
                ret.add(2040919); // Scroll for Shield for Magic Att. - Improves magic attack on the shield.\nSuccess Rate 60%, magic attack+2, INT+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040920); // Scroll for Shield for Magic Att. - Improves magic attack on the shield.\nSuccess Rate 10%, magic attack+3, INT+2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040923); // Scroll for Shield for LUK 100% - Improves LUK on shields..\nSuccess rate:100%, LUK+1
                ret.add(2040924); // Scroll for Shield for LUK 60% - Improves LUK on shields.\nSuccess rate:60%, LUK+2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040925); // Scroll for Shield for LUK 10% - Improves LUK on shields.\nSuccess rate:10%, LUK+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040926); // Scroll for Shield for HP 100% - Improves HP on shields..\nSuccess rate:100%, MaxHP+5
                ret.add(2040927); // Scroll for Shield for HP 60% - Improves HP on shields.\nSuccess rate:60%, MaxHP+15. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040928); // Scroll for Shield for HP 10% - Improves HP on shields.\nSuccess rate:10%, MaxHP+30. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040929); // Scroll for Shield for STR 100% - Improves strength on shields..\nSuccess rate:100%, STR+1
                ret.add(2040931); // Scroll for Shield for STR 60% - Improves strength on shields.\nSuccess rate:60%, STR+2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040933); // Scroll for Shield for STR 10% - Improves strength on shields.\nSuccess rate:10%, STR+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2040936); // Scroll for Shield for DEF 100% - Improves weapon def. on the shield.\nSuccess rate:100%, weapon def.+2, magic def.+3
                ret.add(2040937); // Scroll for Shield for LUK 65% - Improves LUK on Shields.\nSuccess Rate: 65%, LUK+2
                ret.add(2040938); // Scroll for Shield for LUK 15% - Improves LUK on Shields.\nSuccess Rate: 15%, LUK+3
                ret.add(2040939); // Scroll for Shield for HP 65% - Improves HP on Shields.\nSuccess Rate: 65%, MaxHP+15
                ret.add(2040940); // Scroll for Shield for HP 15% - Improves HP on Shields.\nSuccess Rate: 15%, MaxHP+30
                ret.add(2040941); // Scroll for Shield for STR 65% - Improves STR on Shields.\nSuccess Rate: 65%, STR+2
                ret.add(2040942); // Scroll for Shield for STR 15% - Improves STR on Shields.\nSuccess Rate: 15%, STR+3
                ret.add(2040943); // Scroll for Shield for DEF 50% - Improves weapon def. on the shield.\nSuccess rate 50%, weapon def.+5, magic def.+4
            }
            case CATEGORY_CAPE -> {
                ret.add(2041000); // Scroll for Cape for Magic Def. - Improves magic def. on the cape.\nSuccess rate:100%, magic def. +1
                ret.add(2041001); // Scroll for Cape for Magic Def. - Improves magic def. on the cape.\nSuccess rate:60%, magic def.+3, weapon def.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041002); // Scroll for Cape for Magic Def. - Improves magic def. on the cape.\nSuccess rate:10%, magic def. +5, weapon def. +3, MaxMP+10. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041003); // Scroll for Cape for Weapon Def. - Improves weapon def. on the cape.\nSuccess rate:100%, weapon def.+1
                ret.add(2041004); // Scroll for Cape for Weapon Def. - Improves weapon def. on the cape.\nSuccess rate:60%, weapon def.+3, magic def. +1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041005); // Scroll for Cape for Weapon Def. - Improves weapon def. on the cape.\nSuccess rate:10%, weapon def. +5, magic def.+3, MaxHP+10. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041006); // Scroll for Cape for HP - Improves MaxHP on the cape.\nSuccess rate:100%, MaxHP+5
                ret.add(2041007); // Scroll for Cape for HP - Improves MaxHP on the cape.\nSuccess rate:60%, MaxHP+10. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041008); // Scroll for Cape for HP - Improves MaxHP on the cape.\nSuccess rate:10%, MaxHP+20. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041009); // Scroll for Cape for MP - Improves MaxMP on the cape.\nSuccess rate:100%, MaxMP+5
                ret.add(2041010); // Scroll for Cape for MP - Improves MaxMP on the cape.\nSuccess rate:60%, MaxMP+10. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041011); // Scroll for Cape for MP - Improves MaxMP on the cape.\nSuccess rate:10%, MaxMP+20. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041012); // Scroll for Cape for STR - Improves STR on the cape.\nSuccess rate:100%, STR+1
                ret.add(2041013); // Scroll for Cape for STR - Improves STR on the cape.\nSuccess rate:60%, STR+2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041014); // Scroll for Cape for STR - Improves STR on the cape.\nSuccess rate:10%, STR+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041015); // Scroll for Cape for INT - Improves INT on the cape.\nSuccess rate:100%, INT+1
                ret.add(2041016); // Scroll for Cape for INT - Improves INT on the cape.\nSuccess rate:60%, INT+2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041017); // Scroll for Cape for INT - Improves INT on the cape.\nSuccess rate:10%, INT+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041018); // Scroll for Cape for DEX - Improves DEX on the cape.\nSuccess rate:100%, DEX+1
                ret.add(2041019); // Scroll for Cape for DEX - Improves DEX on the cape.\nSuccess rate:60%, DEX+2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041020); // Scroll for Cape for DEX - Improves DEX on the cape.\nSuccess rate:10%, DEX+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041021); // Scroll for Cape for LUK - Improves LUK on the cape.\nSuccess rate:100%, LUK+1
                ret.add(2041022); // Scroll for Cape for LUK - Improves LUK on the cape.\nSuccess rate:60%, LUK+2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041023); // Scroll for Cape for LUK - Improves LUK on the cape.\nSuccess rate:10%, LUK+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041024); // Scroll for Cape for Magic Def. - Improves magic def. on the cape.\nSuccess rate:100%, magic def.+5, weapon def.+3, MaxMP+10
                ret.add(2041025); // Scroll for Cape for Weapon Def. - Improves weapon def. on the cape.\nSuccess rate:100%, weapon def.+5, magic def.+3, MaxHP+10
                ret.add(2041042); // Scroll for Cape for Magic DEF - Improves magic defense on the cape.\nSuccess rate:65%, magic def.+3, weapon def.+1
                ret.add(2041043); // Scroll for Cape for Magic DEF - Improves magic defense on the cape.\nSuccess rate:15%, magic def.+5, weapon def.+3, MaxMP+10
                ret.add(2041044); // Scroll for Cape for Weapon DEF - Improves weapon defense on the cape.\nSuccess rate:65%, weapon def.+3, magic def.+1
                ret.add(2041045); // Scroll for Cape for Weapon DEF - Improves weapon defense on the cape.\nSuccess rate:15%, weapon def.+5, magic def.+3, MaxHP+10
                ret.add(2041046); // Scroll for Cape for MaxHP - Improves MaxHP on the cape.\nSuccess rate:65%, MaxHP+10
                ret.add(2041047); // Scroll for Cape for MaxHP - Improves MaxHP on the cape.\nSuccess rate:15%, MaxHP+20
                ret.add(2041048); // Scroll for Cape for MP - Improves MaxMP on the cape.\nSuccess rate:65%, MaxMP+10
                ret.add(2041049); // Scroll for Cape for MP - Improves MaxMP on the cape.\nSuccess rate:15%, MaxMP+20
                ret.add(2041050); // Scroll for Cape for STR - Improves STR on the cape.\nSuccess rate:65%, STR+2
                ret.add(2041051); // Scroll for Cape for STR - Improves STR on the cape.\nSuccess rate:15%, STR+3
                ret.add(2041052); // Scroll for Cape for INT - Improves INT on the cape.\nSuccess rate:65%, INT+2
                ret.add(2041053); // Scroll for Cape for INT - Improves INT on the cape.\nSuccess rate:15%, INT+3
                ret.add(2041054); // Scroll for Cape for DEX - Improves dexterity on the cape.\nSuccess rate:65%, DEX+2
                ret.add(2041055); // Scroll for Cape for DEX - Improves dexterity on the cape.\nSuccess rate:15%, DEX+3
                ret.add(2041056); // Scroll for Cape for LUK - Improves LUK on the cape.\nSuccess rate:65%, LUK+2
                ret.add(2041057); // Scroll for Cape for LUK - Improves LUK on the cape.\nSuccess rate:15%, LUK+3
                ret.add(2041058); // Scroll for Cape for Cold Protection 10% - Includes the effect of protection from cold weather on the cape.\nSuccess rate: 10%. Does not affect the number of upgrades available. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041066); // Scroll for Cape for Magic DEF 100% - Improves magic def. on the cape.\nSuccess rate:100%, magic def.+3, weapon def.+2
                ret.add(2041067); // Scroll for Cape for Weapon DEF 100% - Improves weapon def. on the cape.\nSuccess rate:100%, weapon def.+3, magic def. +2
                ret.add(2041068); // Scroll for Cape for Magic Def. 50% - Improves magic def. on the cape.\nSuccess rate:50%, magic def. +5, weapon def. +4
                ret.add(2041069); // Scroll for Cape for Weapon Def. 50% - Improves weapon def. on the cape.\nSuccess rate:50%, weapon def. +5, magic def.+4
            }
            case CATEGORY_RING -> {
                ret.add(2041100); // Scroll for Ring for STR 100% - Improves STR on Rings. nSuccess rate: 100%, STR +1
                ret.add(2041101); // Scroll for Rings for STR 60% - Improves STR on Rings.\nSuccess rate: 60%, STR +2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041102); // Scroll for Rings for STR 10% - Improves STR on Rings.\nSuccess rate: 10%, STR +3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041103); // Scroll for Rings for INT 100% - Improves INT on Rings. nSuccess rate: 100%, INT +1
                ret.add(2041104); // Scroll for Rings for INT 60% - Improves INT on Rings.\nSuccess rate: 60%, INT +2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041105); // Scroll for Rings for INT 10% - Improves INT on Rings.\nSuccess rate: 10%, INT +2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041106); // Scroll for Rings for DEX 100% - Improves DEX on Rings. nSuccess rate: 100%, DEX +1
                ret.add(2041107); // Scroll for Rings for DEX 60% - Improves DEX on Rings.\nSuccess rate: 100%, DEX +2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041108); // Scroll for Rings for DEX 10% - Improves DEX on Rings.\nSuccess rate: 10%, DEX +3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041109); // Scroll for Rings for LUK 100% - Improves LUK on Rings. nSuccess rate: 100%, LUK +1
                ret.add(2041110); // Scroll for Rings for LUK 60% - Improves LUK on Rings.\nSuccess rate: 60%, LUK+2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041111); // Scroll for Rings for LUK 10% - Improves LUK on Rings.\nSuccess rate: 10%, LUK +3. The success rate of this scroll can be enhanced by Vega's Spell.
            }
            case CATEGORY_PENDANT -> {

            }
            case CATEGORY_BELT -> {
                ret.add(2041300); // Scroll for Belts for STR 100% - Improves STR on Belts. nSuccess rate: 100%, STR +1
                ret.add(2041301); // Scroll for Belts for STR 60% - Improves STR on Belts.\nSuccess rate: 60%, STR +2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041302); // Scroll for Belts for STR 10% - Improves STR on Belts.\nSuccess rate: 10%, STR +3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041303); // Scroll for Belts for INT 100% - Improves INT on Belts. nSuccess rate: 100%, INT +1
                ret.add(2041304); // Scroll for Belts for INT 60% - Improves INT on Belts.\nSuccess rate: 60%, INT +2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041305); // Scroll for Belts for INT 10% - Improves INT on Belts.\nSuccess rate: 10%, INT +3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041306); // Scroll for Belts for DEX 100% - Improves DEX on Belts. nSuccess rate: 100%, DEX +1
                ret.add(2041307); // Scroll for Belts for DEX 60% - Improves DEX on Belts.\nSuccess rate: 60%, DEX +2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041308); // Scroll for Belts for DEX 10% - Improves DEX on Belts.\nSuccess rate: 10%, DEX +2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041309); // Scroll for Belts for LUK 100% - Improves LUK on Belts. nSuccess rate: 100%, LUK +1
                ret.add(2041310); // Scroll for Belts for LUK 60% - Improves LUK on Belts.\nSuccess rate: 60%, LUK +2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2041311); // Scroll for Belts for LUK 10% - Improves LUK on Belts.\nSuccess rate: 10%, LUK +3. The success rate of this scroll can be enhanced by Vega's Spell.
            }
            case CATEGORY_PET_EQUIP -> {
                ret.add(2048000); // Scroll for Pet Equip. for Speed - Improves speed on pet equip.\nSuccess rate:100%, speed+1
                ret.add(2048001); // Scroll for Pet Equip. for Speed - Improves speed on pet equip.\nSuccess rate:60%, moving speed+2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2048002); // Scroll for Pet Equip. for Speed - Improves speed on pet equip.\nSuccess rate:10%, moving speed+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2048003); // Scroll for Pet Equip. for Jump - Improves jump on pet equip.\nSuccess rate:100%, jump+1
                ret.add(2048004); // Scroll for Pet Equip. for Jump - Improves jump on pet equip.\nSuccess rate:60%, jump+2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2048005); // Scroll for Pet Equip. for Jump - Improves jump on pet equip.\nSuccess rate:10%, jump+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2048006); // Scroll for Pet Equip. for Speed - Improves speed on Pet Equip. nSuccess rate:65%, speed+2
                ret.add(2048007); // Scroll for Pet Equip. for Speed - Improves speed on Pet Equip. nSuccess rate:15%, speed+3
                ret.add(2048008); // Scroll for Pet Equip. for Jump - Improves jump on Pet equip. nSuccess rate:65%, jump+2
                ret.add(2048009); // Scroll for Pet Equip. for Jump - Improves jump on Pet equip. nSuccess rate:15%, jump+3
                ret.add(2048010); // Scroll for Pet Equip. for STR 60% - Improves strength on pet equipments.\nSuccess rate:60%, STR+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2048011); // Scroll for Pet Equip. for INT 60% - Improves intelligence on pet equipments.\nSuccess rate:60%, INT+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2048012); // Scroll for Pet Equip. for DEX 60% - Improves dexterity on pet equipments.\nSuccess rate:60%, DEX+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2048013); // Scroll for Pet Equip. for LUK 60% - Improves luck on pet equipments.\nSuccess rate:60%, LUK+1. The success rate of this scroll can be enhanced by Vega's Spell.
            }
        }

        // Add to cache
        scrollsByEquipType.put(equipCategory, ret);

        return scrollsByEquipType.get(equipCategory);
    }
}