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

import client.Character;
import client.Client;
import client.Job;
import client.Skill;
import client.SkillFactory;
import client.autoban.AutobanFactory;
import client.inventory.Equip;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.WeaponType;
import config.YamlConfig;
import constants.id.ItemId;
import constants.inventory.EquipSlot;
import constants.inventory.ItemConstants;
import constants.skills.Assassin;
import constants.skills.Gunslinger;
import constants.skills.NightWalker;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import provider.Data;
import provider.DataDirectoryEntry;
import provider.DataFileEntry;
import provider.DataProvider;
import provider.DataProviderFactory;
import provider.DataTool;
import provider.wz.WZFiles;
import server.MakerItemFactory.MakerItemCreateEntry;
import server.life.LifeFactory;
import server.life.MonsterInformationProvider;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.Pair;
import tools.Randomizer;
import tools.StringUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Matze
 */
public class ItemInformationProvider {
    private static final Logger log = LoggerFactory.getLogger(ItemInformationProvider.class);
    private final static ItemInformationProvider instance = new ItemInformationProvider();

    public static ItemInformationProvider getInstance() {
        return instance;
    }

    protected DataProvider itemData;
    protected DataProvider equipData;
    protected DataProvider stringData;
    protected DataProvider etcData;
    protected Data cashStringData;
    protected Data consumeStringData;
    protected Data eqpStringData;
    protected Data etcStringData;
    protected Data insStringData;
    protected Data petStringData;
    protected Map<Integer, Short> slotMaxCache = new HashMap<>();
    protected Map<Integer, StatEffect> itemEffects = new HashMap<>();
    protected Map<Integer, Map<String, Integer>> equipStatsCache = new HashMap<>();
    protected Map<Integer, Equip> equipCache = new HashMap<>();
    protected Map<Integer, Data> equipLevelInfoCache = new HashMap<>();
    protected Map<Integer, Integer> equipLevelReqCache = new HashMap<>();
    protected Map<Integer, Integer> equipMaxLevelCache = new HashMap<>();
    protected Map<Integer, List<Integer>> scrollReqsCache = new HashMap<>();
    protected Map<Integer, Integer> wholePriceCache = new HashMap<>();
    protected Map<Integer, Double> unitPriceCache = new HashMap<>();
    protected Map<Integer, Integer> projectileWatkCache = new HashMap<>();
    protected Map<Integer, String> nameCache = new HashMap<>();
    protected Map<Integer, String> descCache = new HashMap<>();
    protected Map<Integer, String> msgCache = new HashMap<>();
    protected Map<Integer, Boolean> accountItemRestrictionCache = new HashMap<>();
    protected Map<Integer, Boolean> dropRestrictionCache = new HashMap<>();
    protected Map<Integer, Boolean> pickupRestrictionCache = new HashMap<>();
    protected Map<Integer, Integer> getMesoCache = new HashMap<>();
    protected Map<Integer, Integer> monsterBookID = new HashMap<>();
    protected Map<Integer, Boolean> untradeableCache = new HashMap<>();
    protected Map<Integer, Boolean> onEquipUntradeableCache = new HashMap<>();
    protected Map<Integer, ScriptedItem> scriptedItemCache = new HashMap<>();
    protected Map<Integer, Boolean> karmaCache = new HashMap<>();
    protected Map<Integer, Integer> triggerItemCache = new HashMap<>();
    protected Map<Integer, Integer> expCache = new HashMap<>();
    protected Map<Integer, Integer> createItem = new HashMap<>();
    protected Map<Integer, Integer> mobItem = new HashMap<>();
    protected Map<Integer, Integer> useDelay = new HashMap<>();
    protected Map<Integer, Integer> mobHP = new HashMap<>();
    protected Map<Integer, Integer> levelCache = new HashMap<>();
    protected Map<Integer, Pair<Integer, List<RewardItem>>> rewardCache = new HashMap<>();
    protected List<Pair<Integer, String>> itemNameCache = new ArrayList<>();
    protected Map<Integer, Boolean> consumeOnPickupCache = new HashMap<>();
    protected Map<Integer, Boolean> isQuestItemCache = new HashMap<>();
    protected Map<Integer, Boolean> isPartyQuestItemCache = new HashMap<>();
    protected Map<Integer, Pair<Integer, String>> replaceOnExpireCache = new HashMap<>();
    protected Map<Integer, String> equipmentSlotCache = new HashMap<>();
    protected Map<Integer, Boolean> noCancelMouseCache = new HashMap<>();
    protected Map<Integer, Integer> mobCrystalMakerCache = new HashMap<>();
    protected Map<Integer, Pair<String, Integer>> statUpgradeMakerCache = new HashMap<>();
    protected Map<Integer, MakerItemFactory.MakerItemCreateEntry> makerItemCache = new HashMap<>();
    protected Map<Integer, Integer> makerCatalystCache = new HashMap<>();
    protected Map<Integer, Map<String, Integer>> skillUpgradeCache = new HashMap<>();
    protected Map<Integer, Data> skillUpgradeInfoCache = new HashMap<>();
    protected Map<Integer, Pair<Integer, Set<Integer>>> cashPetFoodCache = new HashMap<>();
    protected Map<Integer, QuestConsItem> questItemConsCache = new HashMap<>();

    private ItemInformationProvider() {
        loadCardIdData();
        itemData = DataProviderFactory.getDataProvider(WZFiles.ITEM);
        equipData = DataProviderFactory.getDataProvider(WZFiles.CHARACTER);
        stringData = DataProviderFactory.getDataProvider(WZFiles.STRING);
        etcData = DataProviderFactory.getDataProvider(WZFiles.ETC);
        cashStringData = stringData.getData("Cash.img");
        consumeStringData = stringData.getData("Consume.img");
        eqpStringData = stringData.getData("Eqp.img");
        etcStringData = stringData.getData("Etc.img");
        insStringData = stringData.getData("Ins.img");
        petStringData = stringData.getData("Pet.img");

        isQuestItemCache.put(0, false);
        isPartyQuestItemCache.put(0, false);
    }


    public List<Pair<Integer, String>> getAllItems() {
        if (!itemNameCache.isEmpty()) {
            return itemNameCache;
        }
        List<Pair<Integer, String>> itemPairs = new ArrayList<>();
        Data itemsData;
        itemsData = stringData.getData("Cash.img");
        for (Data itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), DataTool.getString("name", itemFolder, "NO-NAME")));
        }
        itemsData = stringData.getData("Consume.img");
        for (Data itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), DataTool.getString("name", itemFolder, "NO-NAME")));
        }
        itemsData = stringData.getData("Eqp.img").getChildByPath("Eqp");
        for (Data eqpType : itemsData.getChildren()) {
            for (Data itemFolder : eqpType.getChildren()) {
                itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), DataTool.getString("name", itemFolder, "NO-NAME")));
            }
        }
        itemsData = stringData.getData("Etc.img").getChildByPath("Etc");
        for (Data itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), DataTool.getString("name", itemFolder, "NO-NAME")));
        }
        itemsData = stringData.getData("Ins.img");
        for (Data itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), DataTool.getString("name", itemFolder, "NO-NAME")));
        }
        itemsData = stringData.getData("Pet.img");
        for (Data itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), DataTool.getString("name", itemFolder, "NO-NAME")));
        }
        return itemPairs;
    }

    public List<Pair<Integer, String>> getAllEtcItems() {
        if (!itemNameCache.isEmpty()) {
            return itemNameCache;
        }

        List<Pair<Integer, String>> itemPairs = new ArrayList<>();
        Data itemsData;

        itemsData = stringData.getData("Etc.img").getChildByPath("Etc");
        for (Data itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), DataTool.getString("name", itemFolder, "NO-NAME")));
        }
        return itemPairs;
    }

    private Data getStringData(int itemId) {
        String cat = "null";
        Data theData;
        if (itemId >= 5010000) {
            theData = cashStringData;
        } else if (itemId >= 2000000 && itemId < 3000000) {
            theData = consumeStringData;
        } else if ((itemId >= 1010000 && itemId < 1040000) || (itemId >= 1122000 && itemId < 1123000) || (itemId >= 1132000 && itemId < 1133000) || (itemId >= 1142000 && itemId < 1143000)) {
            theData = eqpStringData;
            cat = "Eqp/Accessory";
        } else if (itemId >= 1000000 && itemId < 1010000) {
            theData = eqpStringData;
            cat = "Eqp/Cap";
        } else if (itemId >= 1102000 && itemId < 1103000) {
            theData = eqpStringData;
            cat = "Eqp/Cape";
        } else if (itemId >= 1040000 && itemId < 1050000) {
            theData = eqpStringData;
            cat = "Eqp/Coat";
        } else if (ItemConstants.isFace(itemId)) {
            theData = eqpStringData;
            cat = "Eqp/Face";
        } else if (itemId >= 1080000 && itemId < 1090000) {
            theData = eqpStringData;
            cat = "Eqp/Glove";
        } else if (ItemConstants.isHair(itemId)) {
            theData = eqpStringData;
            cat = "Eqp/Hair";
        } else if (itemId >= 1050000 && itemId < 1060000) {
            theData = eqpStringData;
            cat = "Eqp/Longcoat";
        } else if (itemId >= 1060000 && itemId < 1070000) {
            theData = eqpStringData;
            cat = "Eqp/Pants";
        } else if (itemId >= 1802000 && itemId < 1842000) {
            theData = eqpStringData;
            cat = "Eqp/PetEquip";
        } else if (itemId >= 1112000 && itemId < 1120000) {
            theData = eqpStringData;
            cat = "Eqp/Ring";
        } else if (itemId >= 1092000 && itemId < 1100000) {
            theData = eqpStringData;
            cat = "Eqp/Shield";
        } else if (itemId >= 1070000 && itemId < 1080000) {
            theData = eqpStringData;
            cat = "Eqp/Shoes";
        } else if (itemId >= 1900000 && itemId < 2000000) {
            theData = eqpStringData;
            cat = "Eqp/Taming";
        } else if (itemId >= 1300000 && itemId < 1800000) {
            theData = eqpStringData;
            cat = "Eqp/Weapon";
        } else if (itemId >= 4000000 && itemId < 5000000) {
            theData = etcStringData;
            cat = "Etc";
        } else if (itemId >= 3000000 && itemId < 4000000) {
            theData = insStringData;
        } else if (ItemConstants.isPet(itemId)) {
            theData = petStringData;
        } else {
            return null;
        }
        if (cat.equalsIgnoreCase("null")) {
            return theData.getChildByPath(String.valueOf(itemId));
        } else {
            return theData.getChildByPath(cat + "/" + itemId);
        }
    }

    public boolean noCancelMouse(int itemId) {
        if (noCancelMouseCache.containsKey(itemId)) {
            return noCancelMouseCache.get(itemId);
        }

        Data item = getItemData(itemId);
        if (item == null) {
            noCancelMouseCache.put(itemId, false);
            return false;
        }

        boolean blockMouse = DataTool.getIntConvert("info/noCancelMouse", item, 0) == 1;
        noCancelMouseCache.put(itemId, blockMouse);
        return blockMouse;
    }

    private Data getItemData(int itemId) {
        Data ret = null;
        String idStr = "0" + itemId;
        DataDirectoryEntry root = itemData.getRoot();
        for (DataDirectoryEntry topDir : root.getSubdirectories()) {
            for (DataFileEntry iFile : topDir.getFiles()) {
                if (iFile.getName().equals(idStr.substring(0, 4) + ".img")) {
                    ret = itemData.getData(topDir.getName() + "/" + iFile.getName());
                    if (ret == null) {
                        return null;
                    }
                    ret = ret.getChildByPath(idStr);
                    return ret;
                } else if (iFile.getName().equals(idStr.substring(1) + ".img")) {
                    return itemData.getData(topDir.getName() + "/" + iFile.getName());
                }
            }
        }
        root = equipData.getRoot();
        for (DataDirectoryEntry topDir : root.getSubdirectories()) {
            for (DataFileEntry iFile : topDir.getFiles()) {
                if (iFile.getName().equals(idStr + ".img")) {
                    return equipData.getData(topDir.getName() + "/" + iFile.getName());
                }
            }
        }
        return ret;
    }

    public List<Integer> getItemIdsInRange(int minId, int maxId, boolean ignoreCashItem) {
        List<Integer> list = new ArrayList<>();

        if (ignoreCashItem) {
            for (int i = minId; i <= maxId; i++) {
                if (getItemData(i) != null && !isCash(i)) {
                    list.add(i);
                }
            }
        } else {
            for (int i = minId; i <= maxId; i++) {
                if (getItemData(i) != null) {
                    list.add(i);
                }
            }
        }


        return list;
    }

    private static short getExtraSlotMaxFromPlayer(Client c, int itemId) {
        short ret = 0;

        // thanks GMChuck for detecting player sensitive data being cached into getSlotMax
        if (ItemConstants.isThrowingStar(itemId)) {
            if (c.getPlayer().getJob().isA(Job.NIGHTWALKER1)) {
                //               ret += c.getPlayer().getSkillLevel(SkillFactory.getSkill(NightWalker.CLAW_MASTERY)) * 10;
                ret += c.getPlayer().getSkillLevel(SkillFactory.getSkill(NightWalker.CLAW_MASTERY)) * 100; //Merogie
            } else {
                //               ret += c.getPlayer().getSkillLevel(SkillFactory.getSkill(Assassin.CLAW_MASTERY)) * 10;
                ret += c.getPlayer().getSkillLevel(SkillFactory.getSkill(Assassin.CLAW_MASTERY)) * 100; // Merogie
            }
        } else if (ItemConstants.isBullet(itemId)) {
            //          ret += c.getPlayer().getSkillLevel(SkillFactory.getSkill(Gunslinger.GUN_MASTERY)) * 10;
            ret += c.getPlayer().getSkillLevel(SkillFactory.getSkill(Gunslinger.GUN_MASTERY)) * 200; // Merogie
        }

        return ret;
    }

    public short getSlotMax(Client c, int itemId) {
        Short slotMax = slotMaxCache.get(itemId);
        if (slotMax != null) {
            return (short) (slotMax + getExtraSlotMaxFromPlayer(c, itemId));
        }
        short ret = 0;
        Data item = getItemData(itemId);
        if (item != null) {
            Data smEntry = item.getChildByPath("info/slotMax");
            if (smEntry == null) {
                if (ItemConstants.getInventoryType(itemId).getType() == InventoryType.EQUIP.getType()) {
                    ret = 1;
                } else ret = 32000; // originally was default = 100
            } else if (ItemConstants.isBullet(itemId) || ItemConstants.isThrowingStar(itemId)) {
                ret = (short) (DataTool.getInt(smEntry));  // limit the throwing stars to default, but allow item stacking for other items up to 32k
            } else {
                ret = 32000;
                // ret =(short) (DataTool.getInt(smEntry));
            }
        }

        slotMaxCache.put(itemId, ret);
        return (short) (ret + getExtraSlotMaxFromPlayer(c, itemId));
    }

    public int getMeso(int itemId) {
        if (getMesoCache.containsKey(itemId)) {
            return getMesoCache.get(itemId);
        }
        Data item = getItemData(itemId);
        if (item == null) {
            return -1;
        }
        int pEntry;
        Data pData = item.getChildByPath("info/meso");
        if (pData == null) {
            return -1;
        }
        pEntry = DataTool.getInt(pData);
        getMesoCache.put(itemId, pEntry);
        return pEntry;
    }

    private static double getRoundedUnitPrice(double unitPrice, int max) {
        double intPart = Math.floor(unitPrice);
        double fractPart = unitPrice - intPart;
        if (fractPart == 0.0) {
            return intPart;
        }

        double fractMask = 0.0;
        double lastFract, curFract = 1.0;
        int i = 1;

        do {
            lastFract = curFract;
            curFract /= 2;

            if (fractPart == curFract) {
                break;
            } else if (fractPart > curFract) {
                fractMask += curFract;
                fractPart -= curFract;
            }

            i++;
        } while (i <= max);

        if (i > max) {
            lastFract = curFract;
            curFract = 0.0;
        }

        if (Math.abs(fractPart - curFract) < Math.abs(fractPart - lastFract)) {
            return intPart + fractMask + curFract;
        } else {
            return intPart + fractMask + lastFract;
        }
    }

    private Pair<Integer, Double> getItemPriceData(int itemId) {
        Data item = getItemData(itemId);
        if (item == null) {
            wholePriceCache.put(itemId, -1);
            unitPriceCache.put(itemId, 0.0);
            return new Pair<>(-1, 0.0);
        }

        int pEntry = -1;
        Data pData = item.getChildByPath("info/price");
        if (pData != null) {
            pEntry = DataTool.getInt(pData);
        }

        double fEntry = 0.0f;
        pData = item.getChildByPath("info/unitPrice");
        if (pData != null) {
            try {
                fEntry = getRoundedUnitPrice(DataTool.getDouble(pData), 5);
            } catch (Exception e) {
                fEntry = DataTool.getInt(pData);
            }
        }

        wholePriceCache.put(itemId, pEntry);
        unitPriceCache.put(itemId, fEntry);
        return new Pair<>(pEntry, fEntry);
    }

    public int getWholePrice(int itemId) {
        if (wholePriceCache.containsKey(itemId)) {
            return wholePriceCache.get(itemId);
        }

        return getItemPriceData(itemId).getLeft();
    }

    public double getUnitPrice(int itemId) {
        if (unitPriceCache.containsKey(itemId)) {
            return unitPriceCache.get(itemId);
        }

        return getItemPriceData(itemId).getRight();
    }

    public int getPrice(int itemId, int quantity) {
        int retPrice = getWholePrice(itemId);
        if (retPrice == -1) {
            return -1;
        }

        if (!ItemConstants.isRechargeable(itemId)) {
            retPrice *= quantity;
        } else {
            retPrice += Math.ceil(quantity * getUnitPrice(itemId));
        }

        return retPrice;
    }

    public Pair<Integer, String> getReplaceOnExpire(int itemId) {   // thanks to GabrielSin
        if (replaceOnExpireCache.containsKey(itemId)) {
            return replaceOnExpireCache.get(itemId);
        }

        Data data = getItemData(itemId);
        int itemReplacement = DataTool.getInt("info/replace/itemid", data, 0);
        String msg = DataTool.getString("info/replace/msg", data, "");

        Pair<Integer, String> ret = new Pair<>(itemReplacement, msg);
        replaceOnExpireCache.put(itemId, ret);

        return ret;
    }

    protected String getEquipmentSlot(int itemId) {
        if (equipmentSlotCache.containsKey(itemId)) {
            return equipmentSlotCache.get(itemId);
        }

        String ret = "";

        Data item = getItemData(itemId);

        if (item == null) {
            return null;
        }

        Data info = item.getChildByPath("info");

        if (info == null) {
            return null;
        }

        ret = DataTool.getString("islot", info, "");

        equipmentSlotCache.put(itemId, ret);

        return ret;
    }

    public Map<String, Integer> getEquipStats(int itemId) {
        if (equipStatsCache.containsKey(itemId)) {
            return equipStatsCache.get(itemId);
        }
        Map<String, Integer> ret = new LinkedHashMap<>();
        Data item = getItemData(itemId);
        if (item == null) {
            return null;
        }
        Data info = item.getChildByPath("info");
        if (info == null) {
            return null;
        }
        for (Data data : info.getChildren()) {
            if (data.getName().startsWith("inc")) {
                ret.put(data.getName().substring(3), DataTool.getIntConvert(data));
            }
            /*else if (data.getName().startsWith("req"))
             ret.put(data.getName(), DataTool.getInt(data.getName(), info, 0));*/
        }
        ret.put("reqJob", DataTool.getInt("reqJob", info, 0));
        ret.put("reqLevel", DataTool.getInt("reqLevel", info, 0));
        ret.put("reqDEX", DataTool.getInt("reqDEX", info, 0));
        ret.put("reqSTR", DataTool.getInt("reqSTR", info, 0));
        ret.put("reqINT", DataTool.getInt("reqINT", info, 0));
        ret.put("reqLUK", DataTool.getInt("reqLUK", info, 0));
        ret.put("reqPOP", DataTool.getInt("reqPOP", info, 0));
        ret.put("cash", DataTool.getInt("cash", info, 0));
        ret.put("tuc", DataTool.getInt("tuc", info, 0));
        ret.put("cursed", DataTool.getInt("cursed", info, 0));
        ret.put("success", DataTool.getInt("success", info, 0));
        ret.put("fs", DataTool.getInt("fs", info, 0));
        equipStatsCache.put(itemId, ret);
        return ret;
    }

    public Integer getEquipLevelReq(int itemId) {
        if (equipLevelReqCache.containsKey(itemId)) {
            return equipLevelReqCache.get(itemId);
        }

        int ret = 0;
        Data item = getItemData(itemId);
        if (item != null) {
            Data info = item.getChildByPath("info");
            if (info != null) {
                ret = DataTool.getInt("reqLevel", info, 0);
            }
        }

        equipLevelReqCache.put(itemId, ret);
        return ret;
    }

    public List<Integer> getScrollReqs(int itemId) {
        if (scrollReqsCache.containsKey(itemId)) {
            return scrollReqsCache.get(itemId);
        }

        List<Integer> ret = new ArrayList<>();
        Data data = getItemData(itemId);
        data = data.getChildByPath("req");
        if (data != null) {
            for (Data req : data.getChildren()) {
                ret.add(DataTool.getInt(req));
            }
        }

        scrollReqsCache.put(itemId, ret);
        return ret;
    }

    public WeaponType getWeaponType(int itemId) {
        int cat = (itemId / 10000) % 100;
        WeaponType[] type = {WeaponType.SWORD1H, WeaponType.GENERAL1H_SWING, WeaponType.GENERAL1H_SWING, WeaponType.DAGGER_OTHER, WeaponType.NOT_A_WEAPON, WeaponType.NOT_A_WEAPON, WeaponType.NOT_A_WEAPON, WeaponType.WAND, WeaponType.STAFF, WeaponType.NOT_A_WEAPON, WeaponType.SWORD2H, WeaponType.GENERAL2H_SWING, WeaponType.GENERAL2H_SWING, WeaponType.SPEAR_STAB, WeaponType.POLE_ARM_SWING, WeaponType.BOW, WeaponType.CROSSBOW, WeaponType.CLAW, WeaponType.KNUCKLE, WeaponType.GUN};
        if (cat < 30 || cat > 49) {
            return WeaponType.NOT_A_WEAPON;
        }
        return type[cat - 30];
    }


    private static double testYourLuck(double prop, int dices) {   // revamped testYourLuck author: David A.
        return Math.pow(1.0 - prop, dices);
    }

    public static boolean rollSuccessChance(double propPercent) {
        return Math.random() >= testYourLuck(propPercent / 100.0, YamlConfig.config.server.SCROLL_CHANCE_ROLLS);
    }

    private static short getMaximumShortMaxIfOverflow(int value1, int value2) {
        return (short) Math.min(Short.MAX_VALUE, Math.max(value1, value2));
    }

    private static short getShortMaxIfOverflow(int value) {
        return (short) Math.min(Short.MAX_VALUE, value);
    }

    private static short chscrollRandomizedStat(int range, boolean goodness) {
        if (goodness) {
            return (short) Randomizer.rand(0, range);
        }
        return (short) Randomizer.rand(-range, range);
    }

    private static short chscrollGoodnessRandomizedStat(int range) {
        return (short) Randomizer.rand(0, range);
    }

    public void scrollOptionEquipWithChaos(Equip nEquip, int range, boolean option, boolean goodness) {
        // option: watk, matk, wdef, mdef, spd, jump, hp, mp
        //   stat: dex, luk, str, int, avoid, acc

        if (!option) {
            if (nEquip.getStr() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setStr(getMaximumShortMaxIfOverflow(nEquip.getStr(), (nEquip.getStr() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setStr(getMaximumShortMaxIfOverflow(0, (nEquip.getStr() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getDex() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setDex(getMaximumShortMaxIfOverflow(nEquip.getDex(), (nEquip.getDex() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setDex(getMaximumShortMaxIfOverflow(0, (nEquip.getDex() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getInt() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setInt(getMaximumShortMaxIfOverflow(nEquip.getInt(), (nEquip.getInt() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setInt(getMaximumShortMaxIfOverflow(0, (nEquip.getInt() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getLuk() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setLuk(getMaximumShortMaxIfOverflow(nEquip.getLuk(), (nEquip.getLuk() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setLuk(getMaximumShortMaxIfOverflow(0, (nEquip.getLuk() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getAcc() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setAcc(getMaximumShortMaxIfOverflow(nEquip.getAcc(), (nEquip.getAcc() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setAcc(getMaximumShortMaxIfOverflow(0, (nEquip.getAcc() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getAvoid() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setAvoid(getMaximumShortMaxIfOverflow(nEquip.getAvoid(), (nEquip.getAvoid() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setAvoid(getMaximumShortMaxIfOverflow(0, (nEquip.getAvoid() + chscrollRandomizedStat(range, goodness))));
                }
            }
        } else {
            if (nEquip.getWatk() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setWatk(getMaximumShortMaxIfOverflow(nEquip.getWatk(), (nEquip.getWatk() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setWatk(getMaximumShortMaxIfOverflow(0, (nEquip.getWatk() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getWdef() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setWdef(getMaximumShortMaxIfOverflow(nEquip.getWdef(), (nEquip.getWdef() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setWdef(getMaximumShortMaxIfOverflow(0, (nEquip.getWdef() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getMatk() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setMatk(getMaximumShortMaxIfOverflow(nEquip.getMatk(), (nEquip.getMatk() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setMatk(getMaximumShortMaxIfOverflow(0, (nEquip.getMatk() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getMdef() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setMdef(getMaximumShortMaxIfOverflow(nEquip.getMdef(), (nEquip.getMdef() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setMdef(getMaximumShortMaxIfOverflow(0, (nEquip.getMdef() + chscrollRandomizedStat(range, goodness))));
                }
            }

            if (nEquip.getSpeed() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setSpeed(getMaximumShortMaxIfOverflow(nEquip.getSpeed(), (nEquip.getSpeed() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setSpeed(getMaximumShortMaxIfOverflow(0, (nEquip.getSpeed() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getJump() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setJump(getMaximumShortMaxIfOverflow(nEquip.getJump(), (nEquip.getJump() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setJump(getMaximumShortMaxIfOverflow(0, (nEquip.getJump() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getHp() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setHp(getMaximumShortMaxIfOverflow(nEquip.getHp(), (nEquip.getHp() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setHp(getMaximumShortMaxIfOverflow(0, (nEquip.getHp() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getMp() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setMp(getMaximumShortMaxIfOverflow(nEquip.getMp(), (nEquip.getMp() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setMp(getMaximumShortMaxIfOverflow(0, (nEquip.getMp() + chscrollRandomizedStat(range, goodness))));
                }
            }
        }
    }

    private void scrollEquipWithChaos(Equip nEquip, int range, boolean goodness) {
        if (YamlConfig.config.server.CHSCROLL_STAT_RATE > 0) {
            int temp;
            short curStr, curDex, curInt, curLuk, curWatk, curWdef, curMatk, curMdef, curAcc, curAvoid, curSpeed, curJump, curHp, curMp;

            if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                curStr = nEquip.getStr();
                curDex = nEquip.getDex();
                curInt = nEquip.getInt();
                curLuk = nEquip.getLuk();
                curWatk = nEquip.getWatk();
                curWdef = nEquip.getWdef();
                curMatk = nEquip.getMatk();
                curMdef = nEquip.getMdef();
                curAcc = nEquip.getAcc();
                curAvoid = nEquip.getAvoid();
                curSpeed = nEquip.getSpeed();
                curJump = nEquip.getJump();
                curHp = nEquip.getHp();
                curMp = nEquip.getMp();
            } else {
                curStr = Short.MIN_VALUE;
                curDex = Short.MIN_VALUE;
                curInt = Short.MIN_VALUE;
                curLuk = Short.MIN_VALUE;
                curWatk = Short.MIN_VALUE;
                curWdef = Short.MIN_VALUE;
                curMatk = Short.MIN_VALUE;
                curMdef = Short.MIN_VALUE;
                curAcc = Short.MIN_VALUE;
                curAvoid = Short.MIN_VALUE;
                curSpeed = Short.MIN_VALUE;
                curJump = Short.MIN_VALUE;
                curHp = Short.MIN_VALUE;
                curMp = Short.MIN_VALUE;
            }

            for (int i = 0; i < YamlConfig.config.server.CHSCROLL_STAT_RATE; i++) {
                if (nEquip.getStr() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curStr + chscrollRandomizedStat(range, goodness);
                    } else {
                        temp = nEquip.getStr() + chscrollRandomizedStat(range, goodness);
                    }

                    curStr = getMaximumShortMaxIfOverflow(temp, curStr);
                }

                if (nEquip.getDex() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curDex + chscrollRandomizedStat(range, goodness);
                    } else {
                        temp = nEquip.getDex() + chscrollRandomizedStat(range, goodness);
                    }

                    curDex = getMaximumShortMaxIfOverflow(temp, curDex);
                }

                if (nEquip.getInt() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curInt + chscrollRandomizedStat(range, goodness);
                    } else {
                        temp = nEquip.getInt() + chscrollRandomizedStat(range, goodness);
                    }

                    curInt = getMaximumShortMaxIfOverflow(temp, curInt);
                }

                if (nEquip.getLuk() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curLuk + chscrollRandomizedStat(range, goodness);
                    } else {
                        temp = nEquip.getLuk() + chscrollRandomizedStat(range, goodness);
                    }

                    curLuk = getMaximumShortMaxIfOverflow(temp, curLuk);
                }

                if (nEquip.getWatk() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curWatk + chscrollRandomizedStat(range, goodness);
                    } else {
                        temp = nEquip.getWatk() + chscrollRandomizedStat(range, goodness);
                    }

                    curWatk = getMaximumShortMaxIfOverflow(temp, curWatk);
                }

                if (nEquip.getWdef() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curWdef + chscrollRandomizedStat(range, goodness);
                    } else {
                        temp = nEquip.getWdef() + chscrollRandomizedStat(range, goodness);
                    }

                    curWdef = getMaximumShortMaxIfOverflow(temp, curWdef);
                }

                if (nEquip.getMatk() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curMatk + chscrollRandomizedStat(range, goodness);
                    } else {
                        temp = nEquip.getMatk() + chscrollRandomizedStat(range, goodness);
                    }

                    curMatk = getMaximumShortMaxIfOverflow(temp, curMatk);
                }

                if (nEquip.getMdef() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curMdef + chscrollRandomizedStat(range, goodness);
                    } else {
                        temp = nEquip.getMdef() + chscrollRandomizedStat(range, goodness);
                    }

                    curMdef = getMaximumShortMaxIfOverflow(temp, curMdef);
                }

                if (nEquip.getAcc() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curAcc + chscrollRandomizedStat(range, goodness);
                    } else {
                        temp = nEquip.getAcc() + chscrollRandomizedStat(range, goodness);
                    }

                    curAcc = getMaximumShortMaxIfOverflow(temp, curAcc);
                }

                if (nEquip.getAvoid() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curAvoid + chscrollRandomizedStat(range, goodness);
                    } else {
                        temp = nEquip.getAvoid() + chscrollRandomizedStat(range, goodness);
                    }

                    curAvoid = getMaximumShortMaxIfOverflow(temp, curAvoid);
                }

                if (nEquip.getSpeed() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curSpeed + chscrollRandomizedStat(range, goodness);
                    } else {
                        temp = nEquip.getSpeed() + chscrollRandomizedStat(range, goodness);
                    }

                    curSpeed = getMaximumShortMaxIfOverflow(temp, curSpeed);
                }

                if (nEquip.getJump() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curJump + chscrollRandomizedStat(range, goodness);
                    } else {
                        temp = nEquip.getJump() + chscrollRandomizedStat(range, goodness);
                    }

                    curJump = getMaximumShortMaxIfOverflow(temp, curJump);
                }

                if (nEquip.getHp() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curHp + chscrollRandomizedStat(range, goodness);
                    } else {
                        temp = nEquip.getHp() + chscrollRandomizedStat(range, goodness);
                    }

                    curHp = getMaximumShortMaxIfOverflow(temp, curHp);
                }

                if (nEquip.getMp() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curMp + chscrollRandomizedStat(range, goodness);
                    } else {
                        temp = nEquip.getMp() + chscrollRandomizedStat(range, goodness);
                    }

                    curMp = getMaximumShortMaxIfOverflow(temp, curMp);
                }
            }

            nEquip.setStr((short) Math.max(0, curStr));
            nEquip.setDex((short) Math.max(0, curDex));
            nEquip.setInt((short) Math.max(0, curInt));
            nEquip.setLuk((short) Math.max(0, curLuk));
            nEquip.setWatk((short) Math.max(0, curWatk));
            nEquip.setWdef((short) Math.max(0, curWdef));
            nEquip.setMatk((short) Math.max(0, curMatk));
            nEquip.setMdef((short) Math.max(0, curMdef));
            nEquip.setAcc((short) Math.max(0, curAcc));
            nEquip.setAvoid((short) Math.max(0, curAvoid));
            nEquip.setSpeed((short) Math.max(0, curSpeed));
            nEquip.setJump((short) Math.max(0, curJump));
            nEquip.setHp((short) Math.max(0, curHp));
            nEquip.setMp((short) Math.max(0, curMp));
        } else {
            if (nEquip.getStr() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setStr(getMaximumShortMaxIfOverflow(nEquip.getStr(), (nEquip.getStr() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setStr(getMaximumShortMaxIfOverflow(0, (nEquip.getStr() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getDex() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setDex(getMaximumShortMaxIfOverflow(nEquip.getDex(), (nEquip.getDex() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setDex(getMaximumShortMaxIfOverflow(0, (nEquip.getDex() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getInt() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setInt(getMaximumShortMaxIfOverflow(nEquip.getInt(), (nEquip.getInt() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setInt(getMaximumShortMaxIfOverflow(0, (nEquip.getInt() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getLuk() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setLuk(getMaximumShortMaxIfOverflow(nEquip.getLuk(), (nEquip.getLuk() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setLuk(getMaximumShortMaxIfOverflow(0, (nEquip.getLuk() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getWatk() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setWatk(getMaximumShortMaxIfOverflow(nEquip.getWatk(), (nEquip.getWatk() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setWatk(getMaximumShortMaxIfOverflow(0, (nEquip.getWatk() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getWdef() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setWdef(getMaximumShortMaxIfOverflow(nEquip.getWdef(), (nEquip.getWdef() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setWdef(getMaximumShortMaxIfOverflow(0, (nEquip.getWdef() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getMatk() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setMatk(getMaximumShortMaxIfOverflow(nEquip.getMatk(), (nEquip.getMatk() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setMatk(getMaximumShortMaxIfOverflow(0, (nEquip.getMatk() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getMdef() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setMdef(getMaximumShortMaxIfOverflow(nEquip.getMdef(), (nEquip.getMdef() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setMdef(getMaximumShortMaxIfOverflow(0, (nEquip.getMdef() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getAcc() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setAcc(getMaximumShortMaxIfOverflow(nEquip.getAcc(), (nEquip.getAcc() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setAcc(getMaximumShortMaxIfOverflow(0, (nEquip.getAcc() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getAvoid() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setAvoid(getMaximumShortMaxIfOverflow(nEquip.getAvoid(), (nEquip.getAvoid() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setAvoid(getMaximumShortMaxIfOverflow(0, (nEquip.getAvoid() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getSpeed() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setSpeed(getMaximumShortMaxIfOverflow(nEquip.getSpeed(), (nEquip.getSpeed() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setSpeed(getMaximumShortMaxIfOverflow(0, (nEquip.getSpeed() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getJump() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setJump(getMaximumShortMaxIfOverflow(nEquip.getJump(), (nEquip.getJump() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setJump(getMaximumShortMaxIfOverflow(0, (nEquip.getJump() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getHp() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setHp(getMaximumShortMaxIfOverflow(nEquip.getHp(), (nEquip.getHp() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setHp(getMaximumShortMaxIfOverflow(0, (nEquip.getHp() + chscrollRandomizedStat(range, goodness))));
                }
            }
            if (nEquip.getMp() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setMp(getMaximumShortMaxIfOverflow(nEquip.getMp(), (nEquip.getMp() + chscrollRandomizedStat(range, goodness))));
                } else {
                    nEquip.setMp(getMaximumShortMaxIfOverflow(0, (nEquip.getMp() + chscrollRandomizedStat(range, goodness))));
                }
            }
        }
    }

    /*
        Issue with clean slate found thanks to Masterrulax
        Vicious added in the clean slate check thanks to Crypter (CrypterDEV)
    */
    public boolean canUseCleanSlate(Equip equip) {
        Map<String, Integer> eqStats = getEquipStats(equip.getItemId());
        if (eqStats == null || eqStats.get("tuc") == 0) {
            return false;
        }
        int totalUpgradeCount = eqStats.get("tuc");
        int freeUpgradeCount = equip.getUpgradeSlots();
        int viciousCount = equip.getVicious();
        int appliedScrollCount = equip.getLevel();
        return freeUpgradeCount + appliedScrollCount < totalUpgradeCount + viciousCount;
    }

    public Item scrollEquipWithId(Item equip, int scrollId, boolean usingWhiteScroll, int vegaItemId, boolean isGM) {
        boolean assertGM = (isGM && YamlConfig.config.server.USE_PERFECT_GM_SCROLL);

        if (equip instanceof Equip nEquip) {
            Map<String, Integer> stats = this.getEquipStats(scrollId);

            if (((nEquip.getUpgradeSlots() > 0 || ItemConstants.isCleanSlate(scrollId))) || assertGM) {
                double prop = (double) stats.get("success");

                switch (vegaItemId) {
                    case ItemId.VEGAS_SPELL_10:
                        if (prop == 10.0f) {
                            prop = 30.0f;
                        }
                        break;
                    case ItemId.VEGAS_SPELL_60:
                        if (prop == 60.0f) {
                            prop = 90.0f;
                        }
                        break;
                    case ItemId.CHAOS_SCROll_60:
                        prop = 100.0f;
                        break;
                    case ItemId.CHAOS_SCROll_GOODNESS_60:
                        prop = 100.0f;
                        break;
                }

                if (assertGM || rollSuccessChance(prop)) {
                    short flag = nEquip.getFlag();
                    switch (scrollId) {
                        case ItemId.SPIKES_SCROLL:
                            flag |= ItemConstants.SPIKES;
                            nEquip.setFlag((byte) flag);
                            break;
                        case ItemId.COLD_PROTECTION_SCROLl:
                            flag |= ItemConstants.COLD;
                            nEquip.setFlag((byte) flag);
                            break;
                        case ItemId.CLEAN_SLATE_1:
                        case ItemId.CLEAN_SLATE_3:
                        case ItemId.CLEAN_SLATE_5:
                        case ItemId.CLEAN_SLATE_20:
                            if (canUseCleanSlate(nEquip)) {
                                nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() + 1));
                            }
                            break;
                        case ItemId.CHAOS_SCROll_GOODNESS_60:
                            scrollEquipWithChaos(nEquip, YamlConfig.config.server.CHSCROLL_STAT_RANGE, true);
                            break;
                        case ItemId.CHAOS_SCROll_60:
                        case ItemId.LIAR_TREE_SAP:
                        case ItemId.MAPLE_SYRUP:
                            scrollEquipWithChaos(nEquip, YamlConfig.config.server.CHSCROLL_STAT_RANGE, false);
                            break;

                        default:
                            improveEquipStats(nEquip, stats);
                            break;
                    }
                    if (!ItemConstants.isCleanSlate(scrollId)) {
                        if (!assertGM && !ItemConstants.isModifierScroll(scrollId)) {   // issue with modifier scrolls taking slots found thanks to Masterrulax, justin, BakaKnyx
                            nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
                        }
                        nEquip.setLevel((byte) (nEquip.getLevel() + 1));
                    }
                } else {
                    if (!YamlConfig.config.server.USE_PERFECT_SCROLLING && !usingWhiteScroll && !ItemConstants.isCleanSlate(scrollId) && !assertGM && !ItemConstants.isModifierScroll(scrollId)) {
                        nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
                    }
                    if (Randomizer.nextInt(100) < stats.get("cursed")) {
                        return null;
                    }
                }
            }
        }
        return equip;
    }

    public static void improveEquipStats(Equip nEquip, Map<String, Integer> stats) {
        for (Entry<String, Integer> stat : stats.entrySet()) {
            switch (stat.getKey()) {
                case "STR":
                    nEquip.setStr(getShortMaxIfOverflow(nEquip.getStr() + stat.getValue().intValue()));
                    break;
                case "DEX":
                    nEquip.setDex(getShortMaxIfOverflow(nEquip.getDex() + stat.getValue().intValue()));
                    break;
                case "INT":
                    nEquip.setInt(getShortMaxIfOverflow(nEquip.getInt() + stat.getValue().intValue()));
                    break;
                case "LUK":
                    nEquip.setLuk(getShortMaxIfOverflow(nEquip.getLuk() + stat.getValue().intValue()));
                    break;
                case "PAD":
                    nEquip.setWatk(getShortMaxIfOverflow(nEquip.getWatk() + stat.getValue().intValue()));
                    break;
                case "PDD":
                    nEquip.setWdef(getShortMaxIfOverflow(nEquip.getWdef() + stat.getValue().intValue()));
                    break;
                case "MAD":
                    nEquip.setMatk(getShortMaxIfOverflow(nEquip.getMatk() + stat.getValue().intValue()));
                    break;
                case "MDD":
                    nEquip.setMdef(getShortMaxIfOverflow(nEquip.getMdef() + stat.getValue().intValue()));
                    break;
                case "ACC":
                    nEquip.setAcc(getShortMaxIfOverflow(nEquip.getAcc() + stat.getValue().intValue()));
                    break;
                case "EVA":
                    nEquip.setAvoid(getShortMaxIfOverflow(nEquip.getAvoid() + stat.getValue().intValue()));
                    break;
                case "Speed":
                    nEquip.setSpeed(getShortMaxIfOverflow(nEquip.getSpeed() + stat.getValue().intValue()));
                    break;
                case "Jump":
                    nEquip.setJump(getShortMaxIfOverflow(nEquip.getJump() + stat.getValue().intValue()));
                    break;
                case "MHP":
                    nEquip.setHp(getShortMaxIfOverflow(nEquip.getHp() + stat.getValue().intValue()));
                    break;
                case "MMP":
                    nEquip.setMp(getShortMaxIfOverflow(nEquip.getMp() + stat.getValue().intValue()));
                    break;
                case "afterImage":
                    break;
            }
        }
    }

    public Item getEquipById(int equipId) {
        return getEquipById(equipId, -1);
    }

    private Item getEquipById(int equipId, int ringId) {
        Equip nEquip;
        nEquip = new Equip(equipId, (byte) 0, ringId);
        nEquip.setQuantity((short) 1);
        Map<String, Integer> stats = this.getEquipStats(equipId);
        if (stats != null) {
            for (Entry<String, Integer> stat : stats.entrySet()) {
                if (stat.getKey().equals("STR")) {
                    nEquip.setStr((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("DEX")) {
                    nEquip.setDex((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("INT")) {
                    nEquip.setInt((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("LUK")) {
                    nEquip.setLuk((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("PAD")) {
                    nEquip.setWatk((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("PDD")) {
                    nEquip.setWdef((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("MAD")) {
                    nEquip.setMatk((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("MDD")) {
                    nEquip.setMdef((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("ACC")) {
                    nEquip.setAcc((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("EVA")) {
                    nEquip.setAvoid((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("Speed")) {
                    nEquip.setSpeed((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("Jump")) {
                    nEquip.setJump((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("MHP")) {
                    nEquip.setHp((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("MMP")) {
                    nEquip.setMp((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("tuc")) {
                    nEquip.setUpgradeSlots((byte) stat.getValue().intValue());
                } else if (isUntradeableRestricted(equipId)) {  // thanks Hyun & Thora for showing an issue with more than only "Untradeable" items being flagged as such here
                    short flag = nEquip.getFlag();
                    flag |= ItemConstants.UNTRADEABLE;
                    nEquip.setFlag(flag);
                } else if (stats.get("fs") > 0) {
                    short flag = nEquip.getFlag();
                    flag |= ItemConstants.SPIKES;
                    nEquip.setFlag(flag);
                    equipCache.put(equipId, nEquip);
                }
            }
        }
        return nEquip.copy();
    }

    private static short getRandStat(short defaultValue, int maxRange) {
        if (defaultValue == 0) {
            return 0;
        }
        int lMaxRange = (int) Math.min(Math.ceil(defaultValue * 0.1), maxRange);
        return (short) ((defaultValue - lMaxRange) + Math.floor(Randomizer.nextDouble() * (lMaxRange * 2 + 1)));
    }

    public Equip randomizeStats(Equip equip) {
        equip.setStr(getRandStat(equip.getStr(), 5));
        equip.setDex(getRandStat(equip.getDex(), 5));
        equip.setInt(getRandStat(equip.getInt(), 5));
        equip.setLuk(getRandStat(equip.getLuk(), 5));
        equip.setMatk(getRandStat(equip.getMatk(), 5));
        equip.setWatk(getRandStat(equip.getWatk(), 5));
        equip.setAcc(getRandStat(equip.getAcc(), 5));
        equip.setAvoid(getRandStat(equip.getAvoid(), 5));
        equip.setJump(getRandStat(equip.getJump(), 5));
        equip.setSpeed(getRandStat(equip.getSpeed(), 5));
        equip.setWdef(getRandStat(equip.getWdef(), 10));
        equip.setMdef(getRandStat(equip.getMdef(), 10));
        equip.setHp(getRandStat(equip.getHp(), 10));
        equip.setMp(getRandStat(equip.getMp(), 10));
        return equip;
    }

    private static short getRandUpgradedStat(short defaultValue, int maxRange) {
        if (defaultValue == 0) {
            return 0;
        }
        int lMaxRange = maxRange;
        return (short) (defaultValue + Math.floor(Randomizer.nextDouble() * (lMaxRange + 1)));
    }

    public Equip randomizeUpgradeStats(Equip equip) {
        equip.setStr(getRandUpgradedStat(equip.getStr(), 2));
        equip.setDex(getRandUpgradedStat(equip.getDex(), 2));
        equip.setInt(getRandUpgradedStat(equip.getInt(), 2));
        equip.setLuk(getRandUpgradedStat(equip.getLuk(), 2));
        equip.setMatk(getRandUpgradedStat(equip.getMatk(), 2));
        equip.setWatk(getRandUpgradedStat(equip.getWatk(), 2));
        equip.setAcc(getRandUpgradedStat(equip.getAcc(), 2));
        equip.setAvoid(getRandUpgradedStat(equip.getAvoid(), 2));
        equip.setJump(getRandUpgradedStat(equip.getJump(), 2));
        equip.setWdef(getRandUpgradedStat(equip.getWdef(), 5));
        equip.setMdef(getRandUpgradedStat(equip.getMdef(), 5));
        equip.setHp(getRandUpgradedStat(equip.getHp(), 5));
        equip.setMp(getRandUpgradedStat(equip.getMp(), 5));
        return equip;
    }

    public StatEffect getItemEffect(int itemId) {
        StatEffect ret = itemEffects.get(Integer.valueOf(itemId));
        if (ret == null) {
            Data item = getItemData(itemId);
            if (item == null) {
                return null;
            }
            Data spec = item.getChildByPath("specEx");
            if (spec == null) {
                spec = item.getChildByPath("spec");
            }
            ret = StatEffect.loadItemEffectFromData(spec, itemId);
            itemEffects.put(Integer.valueOf(itemId), ret);
        }
        return ret;
    }

    public int[][] getSummonMobs(int itemId) {
        Data data = getItemData(itemId);
        int theInt = data.getChildByPath("mob").getChildren().size();
        int[][] mobs2spawn = new int[theInt][2];
        for (int x = 0; x < theInt; x++) {
            mobs2spawn[x][0] = DataTool.getIntConvert("mob/" + x + "/id", data);
            mobs2spawn[x][1] = DataTool.getIntConvert("mob/" + x + "/prob", data);
        }
        return mobs2spawn;
    }

    public int getWatkForProjectile(int itemId) {
        Integer atk = projectileWatkCache.get(itemId);
        if (atk != null) {
            return atk.intValue();
        }
        Data data = getItemData(itemId);
        atk = Integer.valueOf(DataTool.getInt("info/incPAD", data, 0));
        projectileWatkCache.put(itemId, atk);
        return atk.intValue();
    }

    public String getName(int itemId) {
        if (nameCache.containsKey(itemId)) {
            return nameCache.get(itemId);
        }
        Data strings = getStringData(itemId);
        if (strings == null) {
            return null;
        }
        String ret = DataTool.getString("name", strings, null);
        nameCache.put(itemId, ret);
        return ret;
    }

    public String getMsg(int itemId) {
        if (msgCache.containsKey(itemId)) {
            return msgCache.get(itemId);
        }
        Data strings = getStringData(itemId);
        if (strings == null) {
            return null;
        }
        String ret = DataTool.getString("msg", strings, null);
        msgCache.put(itemId, ret);
        return ret;
    }

    public boolean isUntradeableRestricted(int itemId) {
        if (untradeableCache.containsKey(itemId)) {
            return untradeableCache.get(itemId);
        }

        boolean bRestricted = false;
        if (itemId != 0) {
            Data data = getItemData(itemId);
            if (data != null) {
                bRestricted = DataTool.getIntConvert("info/tradeBlock", data, 0) == 1;
            }
        }

        untradeableCache.put(itemId, bRestricted);
        return bRestricted;
    }

    public boolean isAccountRestricted(int itemId) {
        if (accountItemRestrictionCache.containsKey(itemId)) {
            return accountItemRestrictionCache.get(itemId);
        }

        boolean bRestricted = false;
        if (itemId != 0) {
            Data data = getItemData(itemId);
            if (data != null) {
                bRestricted = DataTool.getIntConvert("info/accountSharable", data, 0) == 1;
            }
        }

        accountItemRestrictionCache.put(itemId, bRestricted);
        return bRestricted;
    }

    public boolean isLootRestricted(int itemId) {
        if (dropRestrictionCache.containsKey(itemId)) {
            return dropRestrictionCache.get(itemId);
        }

        boolean bRestricted = false;
        if (itemId != 0) {
            Data data = getItemData(itemId);
            if (data != null) {
                bRestricted = DataTool.getIntConvert("info/tradeBlock", data, 0) == 1;
                if (!bRestricted) {
                    bRestricted = isAccountRestricted(itemId);
                }
            }
        }

        dropRestrictionCache.put(itemId, bRestricted);
        return bRestricted;
    }

    public boolean isDropRestricted(int itemId) {
        return isLootRestricted(itemId) || isQuestItem(itemId);
    }

    public boolean isPickupRestricted(int itemId) {
        if (pickupRestrictionCache.containsKey(itemId)) {
            return pickupRestrictionCache.get(itemId);
        }

        boolean bRestricted = false;
        if (itemId != 0) {
            Data data = getItemData(itemId);
            if (data != null) {
                bRestricted = DataTool.getIntConvert("info/only", data, 0) == 1;
            }
        }

        pickupRestrictionCache.put(itemId, bRestricted);
        return bRestricted;
    }

    private Pair<Map<String, Integer>, Data> getSkillStatsInternal(int itemId) {
        Map<String, Integer> ret = skillUpgradeCache.get(itemId);
        Data retSkill = skillUpgradeInfoCache.get(itemId);

        if (ret != null) {
            return new Pair<>(ret, retSkill);
        }

        retSkill = null;
        ret = new LinkedHashMap<>();
        Data item = getItemData(itemId);
        if (item != null) {
            Data info = item.getChildByPath("info");
            if (info != null) {
                for (Data data : info.getChildren()) {
                    if (data.getName().startsWith("inc")) {
                        ret.put(data.getName().substring(3), DataTool.getIntConvert(data));
                    }
                }
                ret.put("masterLevel", DataTool.getInt("masterLevel", info, 0));
                ret.put("reqSkillLevel", DataTool.getInt("reqSkillLevel", info, 0));
                ret.put("success", DataTool.getInt("success", info, 0));

                retSkill = info.getChildByPath("skill");
            }
        }

        skillUpgradeCache.put(itemId, ret);
        skillUpgradeInfoCache.put(itemId, retSkill);
        return new Pair<>(ret, retSkill);
    }

    public Map<String, Integer> getSkillStats(int itemId, double playerJob) {
        Pair<Map<String, Integer>, Data> retData = getSkillStatsInternal(itemId);
        if (retData.getLeft().isEmpty()) {
            return null;
        }

        Map<String, Integer> ret = new LinkedHashMap<>(retData.getLeft());
        Data skill = retData.getRight();
        int curskill;
        for (int i = 0; i < skill.getChildren().size(); i++) {
            curskill = DataTool.getInt(Integer.toString(i), skill, 0);
            if (curskill == 0) {
                break;
            }
            if (curskill / 10000 == playerJob) {
                ret.put("skillid", curskill);
                break;
            }
        }
        if (ret.get("skillid") == null) {
            ret.put("skillid", 0);
        }
        return ret;
    }

    public Pair<Integer, Boolean> canPetConsume(Integer petId, Integer itemId) {
        Pair<Integer, Set<Integer>> foodData = cashPetFoodCache.get(itemId);

        if (foodData == null) {
            Set<Integer> pets = new HashSet<>(4);
            int inc = 1;

            Data data = getItemData(itemId);
            if (data != null) {
                Data specData = data.getChildByPath("spec");
                for (Data specItem : specData.getChildren()) {
                    String itemName = specItem.getName();

                    try {
                        Integer.parseInt(itemName); // check if it's a petid node

                        Integer petid = DataTool.getInt(specItem, 0);
                        pets.add(petid);
                    } catch (NumberFormatException npe) {
                        if (itemName.contentEquals("inc")) {
                            inc = DataTool.getInt(specItem, 1);
                        }
                    }
                }
            }

            foodData = new Pair<>(inc, pets);
            cashPetFoodCache.put(itemId, foodData);
        }

        return new Pair<>(foodData.getLeft(), foodData.getRight().contains(petId));
    }

    public boolean isQuestItem(int itemId) {
        if (isQuestItemCache.containsKey(itemId)) {
            return isQuestItemCache.get(itemId);
        }
        Data data = getItemData(itemId);
        boolean questItem = (data != null && DataTool.getIntConvert("info/quest", data, 0) == 1);
        isQuestItemCache.put(itemId, questItem);
        return questItem;
    }

    public boolean isPartyQuestItem(int itemId) {
        if (isPartyQuestItemCache.containsKey(itemId)) {
            return isPartyQuestItemCache.get(itemId);
        }
        Data data = getItemData(itemId);
        boolean partyquestItem = (data != null && DataTool.getIntConvert("info/pquest", data, 0) == 1);
        isPartyQuestItemCache.put(itemId, partyquestItem);
        return partyquestItem;
    }

    private void loadCardIdData() {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT cardid, mobid FROM monstercarddata");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                monsterBookID.put(rs.getInt(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getCardMobId(int id) {
        return monsterBookID.get(id);
    }

    public boolean isUntradeableOnEquip(int itemId) {
        if (onEquipUntradeableCache.containsKey(itemId)) {
            return onEquipUntradeableCache.get(itemId);
        }
        boolean untradeableOnEquip = DataTool.getIntConvert("info/equipTradeBlock", getItemData(itemId), 0) > 0;
        onEquipUntradeableCache.put(itemId, untradeableOnEquip);
        return untradeableOnEquip;
    }

    public ScriptedItem getScriptedItemInfo(int itemId) {
        if (scriptedItemCache.containsKey(itemId)) {
            return scriptedItemCache.get(itemId);
        }
        if ((itemId / 10000) != 243) {
            return null;
        }
        Data itemInfo = getItemData(itemId);
        ScriptedItem script = new ScriptedItem(DataTool.getInt("spec/npc", itemInfo, 0),
                DataTool.getString("spec/script", itemInfo, ""),
                DataTool.getInt("spec/runOnPickup", itemInfo, 0) == 1);
        scriptedItemCache.put(itemId, script);
        return scriptedItemCache.get(itemId);
    }

    public boolean isKarmaAble(int itemId) {
        if (karmaCache.containsKey(itemId)) {
            return karmaCache.get(itemId);
        }
        boolean bRestricted = DataTool.getIntConvert("info/tradeAvailable", getItemData(itemId), 0) > 0;
        karmaCache.put(itemId, bRestricted);
        return bRestricted;
    }

    public int getStateChangeItem(int itemId) {
        if (triggerItemCache.containsKey(itemId)) {
            return triggerItemCache.get(itemId);
        } else {
            int triggerItem = DataTool.getIntConvert("info/stateChangeItem", getItemData(itemId), 0);
            triggerItemCache.put(itemId, triggerItem);
            return triggerItem;
        }
    }

    public int getCreateItem(int itemId) {
        if (createItem.containsKey(itemId)) {
            return createItem.get(itemId);
        } else {
            int itemFrom = DataTool.getIntConvert("info/create", getItemData(itemId), 0);
            createItem.put(itemId, itemFrom);
            return itemFrom;
        }
    }

    public int getMobItem(int itemId) {
        if (mobItem.containsKey(itemId)) {
            return mobItem.get(itemId);
        } else {
            int mobItemCatch = DataTool.getIntConvert("info/mob", getItemData(itemId), 0);
            mobItem.put(itemId, mobItemCatch);
            return mobItemCatch;
        }
    }

    public int getUseDelay(int itemId) {
        if (useDelay.containsKey(itemId)) {
            return useDelay.get(itemId);
        } else {
            int mobUseDelay = DataTool.getIntConvert("info/useDelay", getItemData(itemId), 0);
            useDelay.put(itemId, mobUseDelay);
            return mobUseDelay;
        }
    }

    public int getMobHP(int itemId) {
        if (mobHP.containsKey(itemId)) {
            return mobHP.get(itemId);
        } else {
            int mobHPItem = DataTool.getIntConvert("info/mobHP", getItemData(itemId), 0);
            mobHP.put(itemId, mobHPItem);
            return mobHPItem;
        }
    }

    public int getExpById(int itemId) {
        if (expCache.containsKey(itemId)) {
            return expCache.get(itemId);
        } else {
            int exp = DataTool.getIntConvert("spec/exp", getItemData(itemId), 0);
            expCache.put(itemId, exp);
            return exp;
        }
    }

    public int getMaxLevelById(int itemId) {
        if (levelCache.containsKey(itemId)) {
            return levelCache.get(itemId);
        } else {
            int level = DataTool.getIntConvert("info/maxLevel", getItemData(itemId), 256);
            levelCache.put(itemId, level);
            return level;
        }
    }

    public Pair<Integer, List<RewardItem>> getItemReward(int itemId) {//Thanks Celino - used some stuffs :)
        if (rewardCache.containsKey(itemId)) {
            return rewardCache.get(itemId);
        }
        int totalprob = 0;
        List<RewardItem> rewards = new ArrayList<>();
        for (Data child : getItemData(itemId).getChildByPath("reward").getChildren()) {
            RewardItem reward = new RewardItem();
            reward.itemid = DataTool.getInt("item", child, 0);
            reward.prob = (byte) DataTool.getInt("prob", child, 0);
            reward.quantity = (short) DataTool.getInt("count", child, 0);
            reward.effect = DataTool.getString("Effect", child, "");
            reward.worldmsg = DataTool.getString("worldMsg", child, null);
            reward.period = DataTool.getInt("period", child, -1);

            totalprob += reward.prob;

            rewards.add(reward);
        }
        Pair<Integer, List<RewardItem>> hmm = new Pair<>(totalprob, rewards);
        rewardCache.put(itemId, hmm);
        return hmm;
    }

    public boolean isConsumeOnPickup(int itemId) {
        if (consumeOnPickupCache.containsKey(itemId)) {
            return consumeOnPickupCache.get(itemId);
        }
        Data data = getItemData(itemId);
        boolean consume = DataTool.getIntConvert("spec/consumeOnPickup", data, 0) == 1 || DataTool.getIntConvert("specEx/consumeOnPickup", data, 0) == 1;
        consumeOnPickupCache.put(itemId, consume);
        return consume;
    }

    public final boolean isTwoHanded(int itemId) {
        switch (getWeaponType(itemId)) {
            case GENERAL2H_SWING:
            case BOW:
            case CLAW:
            case CROSSBOW:
            case POLE_ARM_SWING:
            case SPEAR_STAB:
            case SWORD2H:
            case GUN:
            case KNUCKLE:
                return true;
            default:
                return false;
        }
    }

    public boolean isCash(int itemId) {
        int itemType = itemId / 1000000;
        if (itemType == 5) {
            return true;
        }
        if (itemType != 1) {
            return false;
        }

        Map<String, Integer> eqpStats = getEquipStats(itemId);
        return eqpStats != null && eqpStats.get("cash") == 1;
    }

    public boolean isUpgradeable(int itemId) {
        Item it = this.getEquipById(itemId);
        Equip eq = (Equip) it;

        return (eq.getUpgradeSlots() > 0 || eq.getStr() > 0 || eq.getDex() > 0 || eq.getInt() > 0 || eq.getLuk() > 0 ||
                eq.getWatk() > 0 || eq.getMatk() > 0 || eq.getWdef() > 0 || eq.getMdef() > 0 || eq.getAcc() > 0 ||
                eq.getAvoid() > 0 || eq.getSpeed() > 0 || eq.getJump() > 0 || eq.getHp() > 0 || eq.getMp() > 0);
    }

    public boolean isUnmerchable(int itemId) {
        if (YamlConfig.config.server.USE_ENFORCE_UNMERCHABLE_CASH && isCash(itemId)) {
            return true;
        }

        return YamlConfig.config.server.USE_ENFORCE_UNMERCHABLE_PET && ItemConstants.isPet(itemId);
    }

    public Collection<Item> canWearEquipment(Character chr, Collection<Item> items) {
        Inventory inv = chr.getInventory(InventoryType.EQUIPPED);
        if (inv.checked()) {
            return items;
        }
        Collection<Item> itemz = new LinkedList<>();
        if (chr.getJob() == Job.SUPERGM || chr.getJob() == Job.GM) {
            for (Item item : items) {
                Equip equip = (Equip) item;
                equip.wear(true);
                itemz.add(item);
            }
            return itemz;
        }
        boolean highfivestamp = false;
        /* Removed because players shouldn't even get this, and gm's should just be gm job.
         try {
         for (Pair<Item, InventoryType> ii : ItemFactory.INVENTORY.loadItems(chr.getId(), false)) {
         if (ii.getRight() == InventoryType.CASH) {
         if (ii.getLeft().getItemId() == 5590000) {
         highfivestamp = true;
         }
         }
         }
         } catch (SQLException ex) {
            ex.printStackTrace();
         }*/
        int tdex = chr.getDex(),
                tstr = chr.getStr(),
                tint = chr.getInt(),
                tluk = chr.getLuk(),
                fame = chr.getFame();
        if (chr.getJob() != Job.SUPERGM || chr.getJob() != Job.GM) {
            for (Item item : inv.list()) {
                Equip equip = (Equip) item;
                tdex += equip.getDex();
                tstr += equip.getStr();
                tluk += equip.getLuk();
                tint += equip.getInt();
            }
        }
        for (Item item : items) {
            Equip equip = (Equip) item;
            int reqLevel = getEquipLevelReq(equip.getItemId());
            if (highfivestamp) {
                reqLevel -= 5;
                if (reqLevel < 0) {
                    reqLevel = 0;
                }
            }
            /*
             int reqJob = getEquipStats(equip.getItemId()).get("reqJob");
             if (reqJob != 0) {
             Really hard check, and not really needed in this one
             Gm's should just be GM job, and players cannot change jobs.
             }*/
//            Original
            if (reqLevel > chr.getLevel()) { // dont meet level requirement
                continue;
            } else if (getEquipStats(equip.getItemId()).get("reqDEX") > tdex) {
                continue;
            } else if (getEquipStats(equip.getItemId()).get("reqSTR") > tstr) {
                continue;
            } else if (getEquipStats(equip.getItemId()).get("reqLUK") > tluk) {
                continue;
            } else if (getEquipStats(equip.getItemId()).get("reqINT") > tint) {
                continue;
            }
            int reqPOP = getEquipStats(equip.getItemId()).get("reqPOP");
            if (reqPOP > 0) {
                if (getEquipStats(equip.getItemId()).get("reqPOP") > fame) {
                    continue;
                }
            }
            equip.wear(true);
            itemz.add(equip);
        }
        inv.checked(true);
        return itemz;
    }

    public boolean canWearEquipment(Character chr, Equip equip, int dst) {
        int id = equip.getItemId();

        if (ItemId.isWeddingRing(id) && chr.hasJustMarried()) {
            chr.dropMessage(5, "The Wedding Ring cannot be equipped on this map.");  // will dc everyone due to doubled couple effect
            return false;
        }

        String islot = getEquipmentSlot(id);
        if (!EquipSlot.getFromTextSlot(islot).isAllowed(dst, isCash(id))) {
            equip.wear(false);
            String itemName = ItemInformationProvider.getInstance().getName(equip.getItemId());
            Server.getInstance().broadcastGMMessage(chr.getWorld(), PacketCreator.sendYellowTip("[Warning]: " + chr.getName() + " tried to equip " + itemName + " into slot " + dst + "."));
            AutobanFactory.PACKET_EDIT.alert(chr, chr.getName() + " tried to forcibly equip an item.");
            log.warn("Chr {} tried to equip {} into slot {}", chr.getName(), itemName, dst);
            return false;
        }

        if (chr.getJob() == Job.SUPERGM || chr.getJob() == Job.GM) {
            equip.wear(true);
            return true;
        }


        boolean highfivestamp = false;
        /* Removed check above for message ><
         try {
         for (Pair<Item, InventoryType> ii : ItemFactory.INVENTORY.loadItems(chr.getId(), false)) {
         if (ii.getRight() == InventoryType.CASH) {
         if (ii.getLeft().getItemId() == 5590000) {
         highfivestamp = true;
         }
         }
         }
         } catch (SQLException ex) {
            ex.printStackTrace();
         }*/

        int reqLevel = getEquipLevelReq(equip.getItemId());
        if (highfivestamp) {
            reqLevel -= 5;
        }
        int i = 0; //lol xD
        //Removed job check. Shouldn't really be needed.
        if (reqLevel > chr.getLevel()) {
            i++;
        } else if (getEquipStats(equip.getItemId()).get("reqDEX") > chr.getTotalDex()) {
            i++;
        } else if (getEquipStats(equip.getItemId()).get("reqSTR") > chr.getTotalStr()) {
            i++;
        } else if (getEquipStats(equip.getItemId()).get("reqLUK") > chr.getTotalLuk()) {
            i++;
        } else if (getEquipStats(equip.getItemId()).get("reqINT") > chr.getTotalInt()) {
            i++;
        }
        int reqPOP = getEquipStats(equip.getItemId()).get("reqPOP");
        if (reqPOP > 0) {
            if (getEquipStats(equip.getItemId()).get("reqPOP") > chr.getFame()) {
                i++;
            }
        }

        if (i > 0) {
            equip.wear(false);
            return false;
        }
        equip.wear(true);
        return true;
    }

    public ArrayList<Pair<Integer, String>> getItemDataByName(String name) {
        ArrayList<Pair<Integer, String>> ret = new ArrayList<>();
        for (Pair<Integer, String> itemPair : ItemInformationProvider.getInstance().getAllItems()) {
            if (itemPair.getRight().toLowerCase().contains(name.toLowerCase())) {
                ret.add(itemPair);
            }
        }
        return ret;
    }

    private Data getEquipLevelInfo(int itemId) {
        Data equipLevelData = equipLevelInfoCache.get(itemId);
        if (equipLevelData == null) {
            if (equipLevelInfoCache.containsKey(itemId)) {
                return null;
            }

            Data iData = getItemData(itemId);
            if (iData != null) {
                Data data = iData.getChildByPath("info/level");
                if (data != null) {
                    equipLevelData = data.getChildByPath("info");
                }
            }

            equipLevelInfoCache.put(itemId, equipLevelData);
        }

        return equipLevelData;
    }

    public int getEquipLevel(int itemId, boolean getMaxLevel) {
        Integer eqLevel = equipMaxLevelCache.get(itemId);
        if (eqLevel == null) {
            eqLevel = 1;    // greater than 1 means that it was supposed to levelup on GMS

            Data data = getEquipLevelInfo(itemId);
            if (data != null) {
                if (getMaxLevel) {
                    int curLevel = 1;

                    while (true) {
                        Data data2 = data.getChildByPath(Integer.toString(curLevel));
                        if (data2 == null || data2.getChildren().size() <= 1) {
                            eqLevel = curLevel;
                            equipMaxLevelCache.put(itemId, eqLevel);
                            break;
                        }

                        curLevel++;
                    }
                } else {
                    Data data2 = data.getChildByPath("1");
                    if (data2 != null && data2.getChildren().size() > 1) {
                        eqLevel = 2;
                    }
                }
            }
        }

        return eqLevel;
    }

    public List<Pair<String, Integer>> getItemLevelupStats(int itemId, int level) {
        List<Pair<String, Integer>> list = new LinkedList<>();
        Data data = getEquipLevelInfo(itemId);
        if (data != null) {
            Data data2 = data.getChildByPath(Integer.toString(level));
            if (data2 != null) {
                for (Data da : data2.getChildren()) {
                    if (Math.random() < 0.9) {
                        if (da.getName().startsWith("incDEXMin")) {
                            list.add(new Pair<>("incDEX", Randomizer.rand(DataTool.getInt(da), DataTool.getInt(data2.getChildByPath("incDEXMax")))));
                        } else if (da.getName().startsWith("incSTRMin")) {
                            list.add(new Pair<>("incSTR", Randomizer.rand(DataTool.getInt(da), DataTool.getInt(data2.getChildByPath("incSTRMax")))));
                        } else if (da.getName().startsWith("incINTMin")) {
                            list.add(new Pair<>("incINT", Randomizer.rand(DataTool.getInt(da), DataTool.getInt(data2.getChildByPath("incINTMax")))));
                        } else if (da.getName().startsWith("incLUKMin")) {
                            list.add(new Pair<>("incLUK", Randomizer.rand(DataTool.getInt(da), DataTool.getInt(data2.getChildByPath("incLUKMax")))));
                        } else if (da.getName().startsWith("incMHPMin")) {
                            list.add(new Pair<>("incMHP", Randomizer.rand(DataTool.getInt(da), DataTool.getInt(data2.getChildByPath("incMHPMax")))));
                        } else if (da.getName().startsWith("incMMPMin")) {
                            list.add(new Pair<>("incMMP", Randomizer.rand(DataTool.getInt(da), DataTool.getInt(data2.getChildByPath("incMMPMax")))));
                        } else if (da.getName().startsWith("incPADMin")) {
                            list.add(new Pair<>("incPAD", Randomizer.rand(DataTool.getInt(da), DataTool.getInt(data2.getChildByPath("incPADMax")))));
                        } else if (da.getName().startsWith("incMADMin")) {
                            list.add(new Pair<>("incMAD", Randomizer.rand(DataTool.getInt(da), DataTool.getInt(data2.getChildByPath("incMADMax")))));
                        } else if (da.getName().startsWith("incPDDMin")) {
                            list.add(new Pair<>("incPDD", Randomizer.rand(DataTool.getInt(da), DataTool.getInt(data2.getChildByPath("incPDDMax")))));
                        } else if (da.getName().startsWith("incMDDMin")) {
                            list.add(new Pair<>("incMDD", Randomizer.rand(DataTool.getInt(da), DataTool.getInt(data2.getChildByPath("incMDDMax")))));
                        } else if (da.getName().startsWith("incACCMin")) {
                            list.add(new Pair<>("incACC", Randomizer.rand(DataTool.getInt(da), DataTool.getInt(data2.getChildByPath("incACCMax")))));
                        } else if (da.getName().startsWith("incEVAMin")) {
                            list.add(new Pair<>("incEVA", Randomizer.rand(DataTool.getInt(da), DataTool.getInt(data2.getChildByPath("incEVAMax")))));
                        } else if (da.getName().startsWith("incSpeedMin")) {
                            list.add(new Pair<>("incSpeed", Randomizer.rand(DataTool.getInt(da), DataTool.getInt(data2.getChildByPath("incSpeedMax")))));
                        } else if (da.getName().startsWith("incJumpMin")) {
                            list.add(new Pair<>("incJump", Randomizer.rand(DataTool.getInt(da), DataTool.getInt(data2.getChildByPath("incJumpMax")))));
                        }
                    }
                }
            }
        }

        return list;
    }

    private static int getCrystalForLevel(int level) {
        int range = (level - 1) / 10;

        if (range < 5) {
            return ItemId.BASIC_MONSTER_CRYSTAL_1;
        } else if (range > 11) {
            return ItemId.ADVANCED_MONSTER_CRYSTAL_3;
        } else {
            return switch (range) {
                case 5 -> ItemId.BASIC_MONSTER_CRYSTAL_2;
                case 6 -> ItemId.BASIC_MONSTER_CRYSTAL_3;
                case 7 -> ItemId.INTERMEDIATE_MONSTER_CRYSTAL_1;
                case 8 -> ItemId.INTERMEDIATE_MONSTER_CRYSTAL_2;
                case 9 -> ItemId.INTERMEDIATE_MONSTER_CRYSTAL_3;
                case 10 -> ItemId.ADVANCED_MONSTER_CRYSTAL_1;
                default -> ItemId.ADVANCED_MONSTER_CRYSTAL_2;
            };
        }
    }

    public Pair<String, Integer> getMakerReagentStatUpgrade(int itemId) {
        try {
            Pair<String, Integer> statUpgd = statUpgradeMakerCache.get(itemId);
            if (statUpgd != null) {
                return statUpgd;
            } else if (statUpgradeMakerCache.containsKey(itemId)) {
                return null;
            }

            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT stat, value FROM makerreagentdata WHERE itemid = ?")) {
                ps.setInt(1, itemId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String statType = rs.getString("stat");
                        int statGain = rs.getInt("value");

                        statUpgd = new Pair<>(statType, statGain);
                    }
                }
            }

            statUpgradeMakerCache.put(itemId, statUpgd);
            return statUpgd;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getMakerCrystalFromLeftover(Integer leftoverId) {
        try {
            Integer itemid = mobCrystalMakerCache.get(leftoverId);
            if (itemid != null) {
                return itemid;
            }

            itemid = -1;

            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT dropperid FROM drop_data WHERE itemid = ? ORDER BY dropperid;")) {
                ps.setInt(1, leftoverId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int dropperid = rs.getInt("dropperid");
                        itemid = getCrystalForLevel(LifeFactory.getMonsterLevel(dropperid) - 1);
                    }
                }
            }

            mobCrystalMakerCache.put(leftoverId, itemid);
            return itemid;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    public MakerItemCreateEntry getMakerItemEntry(int toCreate) {
        MakerItemCreateEntry makerEntry;

        if ((makerEntry = makerItemCache.get(toCreate)) != null) {
            return new MakerItemCreateEntry(makerEntry);
        } else {
            try (Connection con = DatabaseConnection.getConnection()) {
                int reqLevel = -1;
                int reqMakerLevel = -1;
                int cost = -1;
                int toGive = -1;
                try (PreparedStatement ps = con.prepareStatement("SELECT req_level, req_maker_level, req_meso, quantity FROM makercreatedata WHERE itemid = ?")) {
                    ps.setInt(1, toCreate);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            reqLevel = rs.getInt("req_level");
                            reqMakerLevel = rs.getInt("req_maker_level");
                            cost = rs.getInt("req_meso");
                            toGive = rs.getInt("quantity");
                        }
                    }
                }

                makerEntry = new MakerItemCreateEntry(cost, reqLevel, reqMakerLevel);
                makerEntry.addGainItem(toCreate, toGive);

                try (PreparedStatement ps = con.prepareStatement("SELECT req_item, count FROM makerrecipedata WHERE itemid = ?")) {
                    ps.setInt(1, toCreate);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            makerEntry.addReqItem(rs.getInt("req_item"), rs.getInt("count"));
                        }
                    }
                }
                makerItemCache.put(toCreate, new MakerItemCreateEntry(makerEntry));
            } catch (SQLException sqle) {
                sqle.printStackTrace();
                makerEntry = null;
            }
        }

        return makerEntry;
    }

    public int getMakerCrystalFromEquip(Integer equipId) {
        try {
            return getCrystalForLevel(getEquipLevelReq(equipId));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    public int getMakerStimulantFromEquip(Integer equipId) {
        try {
            return getCrystalForLevel(getEquipLevelReq(equipId));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    public List<Pair<Integer, Integer>> getMakerDisassembledItems(Integer itemId) {
        List<Pair<Integer, Integer>> items = new LinkedList<>();

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT req_item, count FROM makerrecipedata WHERE itemid = ? AND req_item >= 4260000 AND req_item < 4270000")) {
            ps.setInt(1, itemId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new Pair<>(rs.getInt("req_item"), rs.getInt("count") / 2));   // return to the player half of the crystals needed
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return items;
    }

    public int getMakerDisassembledFee(Integer itemId) {
        int fee = -1;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT req_meso FROM makercreatedata WHERE itemid = ?")) {
            ps.setInt(1, itemId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {   // cost is 13.6363~ % of the original value, trim by 1000.
                    float val = (float) (rs.getInt("req_meso") * 0.13636363636364);
                    fee = (int) (val / 1000);
                    fee *= 1000;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fee;
    }

    public int getMakerStimulant(int itemId) {  // thanks to Arnah
        Integer itemid = makerCatalystCache.get(itemId);
        if (itemid != null) {
            return itemid;
        }

        itemid = -1;
        for (Data md : etcData.getData("ItemMake.img").getChildren()) {
            Data me = md.getChildByPath(StringUtil.getLeftPaddedStr(Integer.toString(itemId), '0', 8));

            if (me != null) {
                itemid = DataTool.getInt(me.getChildByPath("catalyst"), -1);
                break;
            }
        }

        makerCatalystCache.put(itemId, itemid);
        return itemid;
    }

    public Set<String> getWhoDrops(Integer itemId) {
        Set<String> list = new HashSet<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT dropperid FROM drop_data WHERE itemid = ? LIMIT 50")) {
            ps.setInt(1, itemId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String resultName = MonsterInformationProvider.getInstance().getMobNameFromId(rs.getInt("dropperid"));
                    if (!resultName.isEmpty()) {
                        list.add(resultName);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    private boolean canUseSkillBook(Character player, Integer skillBookId) {
        Map<String, Integer> skilldata = getSkillStats(skillBookId, player.getJob().getId());
        if (skilldata == null || skilldata.get("skillid") == 0) {
            return false;
        }

        Skill skill2 = SkillFactory.getSkill(skilldata.get("skillid"));
        return (skilldata.get("skillid") != 0 && ((player.getSkillLevel(skill2) >= skilldata.get("reqSkillLevel") || skilldata.get("reqSkillLevel") == 0) && player.getMasterLevel(skill2) < skilldata.get("masterLevel")));
    }

    public List<Integer> usableMasteryBooks(Character player) {
        List<Integer> masterybook = new LinkedList<>();
        for (Integer i = 2290000; i <= 2290139; i++) {
            if (canUseSkillBook(player, i)) {
                masterybook.add(i);
            }
        }

        return masterybook;
    }

    public List<Integer> usableSkillBooks(Character player) {
        List<Integer> skillbook = new LinkedList<>();
        for (Integer i = 2280000; i <= 2280019; i++) {
            if (canUseSkillBook(player, i)) {
                skillbook.add(i);
            }
        }

        return skillbook;
    }

    public final QuestConsItem getQuestConsumablesInfo(final int itemId) {
        if (questItemConsCache.containsKey(itemId)) {
            return questItemConsCache.get(itemId);
        }
        Data data = getItemData(itemId);
        QuestConsItem qcItem = null;

        Data infoData = data.getChildByPath("info");
        if (infoData.getChildByPath("uiData") != null) {
            qcItem = new QuestConsItem();
            qcItem.exp = DataTool.getInt("exp", infoData);
            qcItem.grade = DataTool.getInt("grade", infoData);
            qcItem.questid = DataTool.getInt("questId", infoData);
            qcItem.items = new HashMap<>(2);

            Map<Integer, Integer> cItems = qcItem.items;
            Data ciData = infoData.getChildByPath("consumeItem");
            if (ciData != null) {
                for (Data ciItem : ciData.getChildren()) {
                    int itemid = DataTool.getInt("0", ciItem);
                    int qty = DataTool.getInt("1", ciItem);

                    cItems.put(itemid, qty);
                }
            }
        }

        questItemConsCache.put(itemId, qcItem);
        return qcItem;
    }

    public class ScriptedItem {

        private final boolean runOnPickup;
        private final int npc;
        private final String script;

        public ScriptedItem(int npc, String script, boolean rop) {
            this.npc = npc;
            this.script = script;
            this.runOnPickup = rop;
        }

        public int getNpc() {
            return npc;
        }

        public String getScript() {
            return script;
        }

        public boolean runOnPickup() {
            return runOnPickup;
        }
    }

    public static final class RewardItem {

        public int itemid, period;
        public short prob, quantity;
        public String effect, worldmsg;
    }

    public static final class QuestConsItem {

        public int questid, exp, grade;
        public Map<Integer, Integer> items;

        public Integer getItemRequirement(int itemid) {
            return items.get(itemid);
        }

    }

    // Scroll cache
    private final Map<Integer, List<Integer>> scrollsByEquipCategory = new HashMap<>();

    // Armor categories
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

    // Weapon categories
    public static final int CATEGORY_1H_SWORD = 130;
    public static final int CATEGORY_1H_AXE = 131;
    public static final int CATEGORY_1H_BW = 132;
    public static final int CATEGORY_DAGGER = 133;
    public static final int CATEGORY_WAND = 137;
    public static final int CATEGORY_STAFF = 138;
    public static final int CATEGORY_2H_SWORD = 140;
    public static final int CATEGORY_2H_AXE = 141;
    public static final int CATEGORY_2H_BW = 142;
    public static final int CATEGORY_SPEAR = 143;
    public static final int CATEGORY_POLEARM = 144;
    public static final int CATEGORY_BOW = 145;
    public static final int CATEGORY_CROSSBOW = 146;
    public static final int CATEGORY_CLAW = 147;
    public static final int CATEGORY_KNUCKLE = 148;
    public static final int CATEGORY_PISTOL = 149;

    public List<Integer> getScrollsByItemId(int itemId) {

        int equipCategory = (itemId / 10000);

        // Check cache first
        if (scrollsByEquipCategory.containsKey(equipCategory)) {
            return scrollsByEquipCategory.get(equipCategory);
        }

        List<Integer> ret = new ArrayList<>();

        // Add all chaos scrolls to all equipment categories
        ret.add(2049100); // Chaos Scroll 60% - Alters the equipment for better or worse. Not available on Cash Items.\nSuccess rate:60%
        ret.add(2049115); // Chaos Scroll of Goodness 60% - Alters the equipment for better. Not available on Cash Items.\nSuccess rate:60%
        ret.add(2049116); // Incredible Chaos Scroll of Goodness 60% - Alters the equipment for much better. Not available on Cash Items.\nSuccess rate:60%
        ret.add(2049117); // Miraculous Chaos Scroll of Goodness 60% - Alters the equipment for super much better. Not available on Cash Items.\nSuccess rate:60%

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
                // No pendant scrolls
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
            case CATEGORY_1H_SWORD -> {
                ret.add(2043000); // Scroll for One-Handed Sword for ATT - Improves attack on one-handed sword.\nSuccess rate:100%, weapon attack+1
                ret.add(2043001); // Scroll for One-Handed Sword for ATT - Improves attack on one-handed sword.\nSuccess rate:60%, weapon attack+2, STR+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043002); // Scroll for One-Handed Sword for ATT - Improves attack on one-handed sword.\nSuccess rate:10%, weapon attack+5, STR+3, weapon def.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043003); // Scroll for One-Handed Sword for ATT - Improves attack on one-handed sword.\nSuccess rate:100%, weapon attack+5, STR+3, weapon def.+1
                ret.add(2043008); // Scroll for One-Handed Sword for Magic Att. - Improves magic attack on one-handed sword.\nSuccess Rate 10%, magic attack+2, magic defense+1, INT+2. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043009); // Scroll for One-Handed Sword for Magic Att. - Improves magic attack on one-handed sword.\nSuccess Rate 60%, magic attack+1, INT+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043010); // Scroll for One-Handed Sword for Magic Att. - Improves magic attack on one-handed sword.\nSuccess Rate 100%, magic attack+1
                ret.add(2043011); // Scroll for One-Handed Sword for ATT - Improves attack on the one-handed sword.\nSuccess rate:65%, weapon attack+2, STR+1
                ret.add(2043012); // Scroll for One-Handed Sword for ATT - Improves attack on the one-handed sword.\nSuccess rate:15%, weapon attack+5, STR+3, weapon def.+1
                ret.add(2043015); // Scroll for One-Handed Sword for Accuracy 100% - Improves accuracy on one-handed swords.\nSuccess rate:100%, accuracy+1
                ret.add(2043017); // Scroll for One-Handed Sword for Accuracy 60% - Improves accuracy on one-handed swords.\nSuccess rate:60%, accuracy+3, DEX+2, weapon att.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043019); // Scroll for One-Handed Sword for Accuracy 10% - Improves accuracy on one-handed swords.\nSuccess rate:10%, accuracy+5, DEX+3, weapon att.+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043022); // Scroll for One-Handed Sword for ATT 50% - Improves attack on one-handed sword.\nSuccess rate:50%, weapon attack+5, STR+3, DEX+1
                ret.add(2043023); // Scroll for One-Handed Sword for ATT 100% - Improves attack on one-handed sword.\nSuccess rate:100%, weapon attack+2, STR+2
                ret.add(2043024); // Scroll for One-Handed Sword for Accuracy 65% - Improves Accuracy on One-Handed Swords.\nSuccess Rate: 65%, Accuracy+3, DEX+2, Weapon Attack+1
                ret.add(2043025); // Scroll for One-Handed Sword for Accuracy 15% - Improves Accuracy on One-Handed Swords.\nSuccess Rate: 15%, Accuracy+5, DEX+3, Weapon Attack+3
            }
            case CATEGORY_1H_AXE -> {
                ret.add(2043100); // Scroll for One-Handed Axe for ATT - Improves attack on one-handed axe.\nSuccess rate:100%, weapon attack+1
                ret.add(2043101); // Scroll for One-Handed Axe for ATT - Improves attack on one-handed axe.\nSuccess rate:60%, weapon attack+2, STR+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043102); // Scroll for One-Handed Axe for ATT - Improves attack on one-handed axe.\nSuccess rate: 10%, weapon attack +5, STR+3, weapon def. +1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043103); // Scroll for One-Handed Axe for ATT - Improves attack on one-handed axe.\nSuccess rate:100%, weapon attack+5, STR+3, weapon def.+1
                ret.add(2043106); // Scroll for One-Handed Axe for ATT - Improves attack on the one-handed axe.\nSuccess rate:65%, weapon attack+2, STR+1
                ret.add(2043107); // Scroll for One-Handed Axe for ATT - Improves attack on the one-handed axe.\nSuccess rate:15%, weapon attack+5, STR+3, weapon def.+1
                ret.add(2043110); // Scroll for One-Handed Axe for Accuracy 100% - Improves accuracy on one-handed axe.\nSuccess rate:100%, accuracy+1
                ret.add(2043112); // Scroll for One-Handed Axe for Accuracy 60% - Improves accuracy on one-handed axe.\nSuccess rate:60%, accuracy+3, DEX+2, weapon att.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043114); // Scroll for One-Handed Axe for Accuracy 10% - Improves accuracy on one-handed axe.\nSuccess rate:10%, accuracy+5, DEX+3, weapon att.+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043117); // Scroll for One-Handed Axe for ATT 100% - Improves attack on one-handed axe.\nSuccess rate:100%, weapon attack+2, STR+2
                ret.add(2043118); // Scroll for One-Handed Axe for Accuracy 65% - Improves Accuracy on One-Handed Axe.\nSuccess Rate: 65%, Accuracy+3, DEX+2, Weapon Attack+1
                ret.add(2043119); // Scroll for One-Handed Axe for Accuracy 15% - Improves Accuracy on One-Handed Axe.\nSuccess Rate: 15%, Accuracy+5, DEX+3, Weapon Attack+3
                ret.add(2043120); // Scroll for One-Handed Axe for ATT 50% - Improves attack on one-handed axe.\nSuccess rate: 50%, weapon attack +5, STR+3, DEX+1
            }
            case CATEGORY_1H_BW -> {
                ret.add(2043200); // Scroll for One-Handed BW for ATT - Improves attack on one-handed blunt weapon.\nSuccess rate:100%, weapon attack+1
                ret.add(2043201); // Scroll for One-Handed BW for ATT - Improves attack on one-handed blunt weapon.\nSuccess rate:60%, weapon attack+2, STR+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043202); // Scroll for One-Handed BW for ATT - Improves attack on one-handed blunt weapon.\nSuccess rate: 10%, weapon attack +5, STR+3, weapon def.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043203); // Scroll for One-Handed BW for ATT - Improves attack on one-handed blunt weaponnSuccess rate:100%, weapon attack+5, STR+3, weapon def.+1
                ret.add(2043206); // Scroll for One-Handed BW for ATT - Improves attack on the one-handed blunt weapon.\nSuccess rate:65%, weapon attack+2, STR+1
                ret.add(2043207); // Scroll for One-Handed BW for ATT - Improves attack on the one-handed blunt weapon.\nSuccess rate:15%, weapon attack+5, STR+3, weapon def.+1
                ret.add(2043210); // Scroll for One-Handed BW for Accuracy 100% - Improves accuracy on one-handed blunt weapon.\nSuccess rate:100%, accuracy+1
                ret.add(2043212); // Scroll for One-Handed BW for Accuracy 60% - Improves accuracy on one-handed blunt weapon.\nSuccess rate:60%, accuracy+3, DEX+2, weapon att.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043214); // Scroll for One-Handed BW for Accuracy 10% - Improves accuracy on one-handed blunt weapon.\nSuccess rate:10%, accuracy+5, DEX+3, weapon att.+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043217); // Scroll for One-Handed BW for ATT 100% - Improves attack on one-handed blunt weapon.\nSuccess rate:100%, weapon attack+2, STR+2
                ret.add(2043218); // Scroll for One-Handed BW for Accuracy 65% - Improves Accuracy on One-Handed Blunt Weapons.\nSuccess Rate: 65%, Accuracy+3, DEX+2, Weapon Attack+1
                ret.add(2043219); // Scroll for One-Handed BW for Accuracy 15% - Improves Accuracy on One-Handed Blunt Weapons.\nSuccess Rate: 15%, Accuracy+5, DEX+3, Weapon Attack+3
                ret.add(2043220); // Scroll for One-Handed BW for ATT 50% - Improves attack on one-handed blunt weapon.\nSuccess rate: 50%, weapon attack +5, STR+3, DEX+1
            }
            case CATEGORY_DAGGER -> {
                ret.add(2043300); // Scroll for Dagger for ATT - Improves attack on dagger.\nSuccess rate:100%, weapon attack+1
                ret.add(2043301); // Scroll for Dagger for ATT - Improves attack on dagger.\nSuccess rate:60%, weapon attack+2, LUK+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043302); // Scroll for Dagger for ATT - Improves attack on dagger.\nSuccess rate: 10%, weapon attack +5, LUK+3, weapon def.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043303); // Scroll for Dagger for ATT - Improves attack on dagger.\nSuccess rate:100%, weapon attack+5, LUK+3, weapon def.+1
                ret.add(2043306); // Scroll for Dagger for ATT - Improves attack on the dagger.\nSuccess rate:65%, weapon attack+2, LUK+1
                ret.add(2043307); // Scroll for Dagger for ATT - Improves attack on the dagger.\nSuccess rate:15%, weapon attack+5, LUK+3, weapon def.+1
                ret.add(2043312); // Scroll for Dagger for ATT 100% - Improves attack on dagger.\nSuccess rate:100%, weapon attack+2, LUK+2
                ret.add(2043313); // Scroll for Dagger for ATT 50% - Improves attack on dagger.\nSuccess rate: 50%, weapon attack +5, LUK+3, DEX+1
            }
            case CATEGORY_WAND -> {
                ret.add(2043700); // Scroll for Wand for Magic Att. - Improves magic on wand.\nSuccess rate:100%, magic attack+1
                ret.add(2043701); // Scroll for Wand for Magic Att. - Improves magic on wand.\nSuccess rate:60%, magic attack+2, INT+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043702); // Scroll for Wand for Magic Att. - Improves magic on wand.\nSuccess rate:10%, magic attack+5, INT+3, magic def.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043703); // Scroll for Wand for Magic Att. - Improves magic on wand.\nSuccess rate:100%, magic attack+5, INT+3, magic def.+1
                ret.add(2043706); // Scroll for Wand for Magic Att. - Improves magic attack on the wand.\nSuccess rate:65%, magic attack+2, INT+1
                ret.add(2043707); // Scroll for Wand for Magic Att. - Improves magic attack on the wand.\nSuccess rate:15%, magic attack+5, INT+3, magic def.+1
                ret.add(2043712); // Scroll for Wand for Magic ATT 100% - Improves magic on wand.\nSuccess rate:100%, magic attack+2, INT+2
                ret.add(2043713); // Scroll for Wand for Magic Att. 50% - Improves magic on wand.\nSuccess rate:50%, magic attack+5, INT+3, LUK+1
            }
            case CATEGORY_STAFF -> {
                ret.add(2043800); // Scroll for Staff for Magic Att. - Improves magic on staff.\nSuccess rate:100%, magic attack+1
                ret.add(2043801); // Scroll for Staff for Magic Att. - Improves magic on staff.\nSuccess rate:60%, magic attack+2, INT+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043802); // Scroll for Staff for Magic Att. - Improves magic on staff.\nSuccess rate:10%, magic attack+5, INT+3, magic def.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2043803); // Scroll for Staff for Magic Att. - Improves magic on staff.\nSuccess rate:100%, magic attack+5, INT+3, magic def.+1
                ret.add(2043806); // Scroll for Staff for Magic Att. - Improves magic attack on the staff.\nSuccess rate:65%, magic attack+2, INT+1
                ret.add(2043807); // Scroll for Staff for Magic Att. - Improves magic attack on the staff.\nSuccess rate:15%, magic attack+5, INT+3, magic def.+1
                ret.add(2043812); // Scroll for Staff for Magic ATT 100% - Improves magic on staff.\nSuccess rate:100%, magic attack+2, INT+2
                ret.add(2043813); // Scroll for Staff for Magic Att. 50% - Improves magic on staff.\nSuccess rate:50%, magic attack+5, INT+3, LUK+1
            }
            case CATEGORY_2H_SWORD -> {
                ret.add(2044000); // Scroll for Two-handed Sword for ATT - Improves attack on two-handed sword.\nSuccess rate:100%, weapon attack+1
                ret.add(2044001); // Scroll for Two-handed Sword for ATT - Improves attack on two-handed sword.\nSuccess rate:60%, weapon attack+2, STR+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044002); // Scroll for Two-handed Sword for ATT - Improves attack on two-handed sword.\nSuccess rate:10%, weapon attack+5, STR+3, weapon def.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044003); // Scroll for Two-handed Sword for ATT - Improves attack on two-handed sword weapon.\nSuccess rate:100%, weapon attack+5, STR+3, weapon def.+1
                ret.add(2044006); // Scroll for Two-Handed Sword for ATT - Improves attack on the two-handed sword.\nSuccess rate:65%, weapon attack+2, STR+1
                ret.add(2044007); // Scroll for Two-Handed Sword for ATT - Improves attack on the two-handed sword.\nSuccess rate:15%, weapon attack+5, STR+3, weapon def.+1
                ret.add(2044010); // Scroll for Two-Handed Sword for Accuracy 100% - Improves accuracy on two-handed swords.\nSuccess rate:100%, accuracy+1
                ret.add(2044012); // Scroll for Two-Handed Sword for Accuracy 60% - Improves accuracy on two-handed swords.\nSuccess rate:60%, accuracy+3, DEX+2, weapon att.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044014); // Scroll for Two-Handed Sword for Accuracy 10% - Improves accuracy on two-handed swords.\nSuccess rate:10%, accuracy+5, DEX+3, weapon att.+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044015); // Scroll for Two-Handed Swords for ATT 10% - Improves ATT on Two-Handed Swords. nSuccess rate: 10%, Weapons ATT +5, STR +3, Weapons Defense +1
                ret.add(2044025); // Scroll for Two-handed Sword for ATT 100% - Improves attack on two-handed sword.\nSuccess rate:100%, weapon attack+2, STR+2
                ret.add(2044026); // Scroll for Two-Handed Sword for Accuracy 65% - Improves Accuracy on Two-Handed Swords.\nSuccess Rate: 65%, Accuracy+3, DEX+2, Weapon Attack+1
                ret.add(2044027); // Scroll for Two-Handed Sword for Accuracy 15% - Improves Accuracy on Two-Handed Swords.\nSuccess Rate: 15%, Accuracy+5, DEX+3, Weapon Attack+3
                ret.add(2044028); // Scroll for Two-handed Sword for ATT 50% - Improves attack on two-handed sword.\nSuccess rate:50%, weapon attack+5, STR+3, DEX+1
            }
            case CATEGORY_2H_AXE -> {
                ret.add(2044100); // Scroll for Two-handed Axe for ATT - Improves attack on two-handed axe.\nSuccess rate:100%, weapon attack+1
                ret.add(2044101); // Scroll for Two-handed Axe for ATT - Improves attack on two-handed axe.\nSuccess rate:60%, weapon attack+2, STR+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044102); // Scroll for Two-handed Axe for ATT - Improves attack on two-handed axe.\nSuccess rate:10%, weapon attack+5, STR+3, weapon def. +1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044103); // Scroll for Two-handed Axe for ATT - Improves attack on two-handed axe.\nSuccess rate:100%, weapon attack+5, STR+3, weapon def.+1
                ret.add(2044106); // Scroll for Two-Handed Axe for ATT - Improves attack on the two-handed axe.\nSuccess rate:65%, weapon attack+2, STR+1
                ret.add(2044107); // Scroll for Two-Handed Axe for ATT - Improves attack on the two-handed axe.\nSuccess rate:15%, weapon attack+5, STR+3, weapon def.+1
                ret.add(2044110); // Scroll for Two-Handed Axe for Accuracy 100% - Improves accuracy on two-handed axe.\nSuccess rate:100%, accuracy+1
                ret.add(2044112); // Scroll for Two-Handed Axe for Accuracy 60% - Improves accuracy on two-handed axe.\nSuccess rate:60%, accuracy+3, DEX+2, weapon att.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044114); // Scroll for Two-Handed Axe for Accuracy 10% - Improves accuracy on two-handed axe.\nSuccess rate:10%, accuracy+5, DEX+3, weapon att.+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044117); // Scroll for Two-handed Axe for ATT 100% - Improves attack on two-handed axe.\nSuccess rate:100%, weapon attack+2, STR+2
                ret.add(2044118); // Scroll for Two-Handed Axe for Accuracy 65% - Improves Accuracy on Two-Handed Axe.\nSuccess Rate: 65%, Accuracy+3, DEX+2, Weapon Attack+1
                ret.add(2044119); // Scroll for Two-Handed Axe for Accuracy 15% - Improves Accuracy on Two-Handed Axe.\nSuccess Rate: 15%, Accuracy+5, DEX+3, Weapon Attack+3
                ret.add(2044120); // Scroll for Two-handed Axe for ATT 50% - Improves attack on two-handed axe.\nSuccess rate:50%, weapon attack+5, STR+3, DEX+1
            }
            case CATEGORY_2H_BW -> {
                ret.add(2044200); // Scroll for Two-handed BW for ATT - Improves attack on two-handed blunt weapon.\nSuccess rate:100%, weapon attack+1
                ret.add(2044201); // Scroll for Two-handed BW for ATT - Improves attack on two-handed blunt weapon.\nSuccess rate:60%, weapon attack+2, STR+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044202); // Scroll for Two-handed BW for ATT - Improves attack on two-handed blunt weapon.\nSuccess rate:10%, weapon attack+5, STR+3, weapon def.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044203); // Scroll for Two-handed BW for ATT - Improves attack on two-handed blunt weapon.\nSuccess rate:100%, weapon attack+5, STR+3, weapon def.+1
                ret.add(2044206); // Scroll for Two-Handed BW for ATT - Improves attack on the two-handed blunt weapon.\nSuccess rate:65%, weapon attack+2, STR+1
                ret.add(2044207); // Scroll for Two-Handed BW for ATT - Improves attack on the two-handed blunt weapon.\nSuccess rate:15%, weapon attack+5, STR+3, weapon def.+1
                ret.add(2044210); // Scroll for Two-Handed BW for Accuracy 100% - Improves accuracy on two-handed blunt weapon.\nSuccess rate:100%, accuracy+1
                ret.add(2044212); // Scroll for Two-Handed BW for Accuracy 60% - Improves accuracy on two-handed blunt weapon.\nSuccess rate:60%, accuracy+3, DEX+2, weapon att.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044214); // Scroll for Two-Handed BW for Accuracy 10% - Improves accuracy on two-handed blunt weapon.\nSuccess rate:10%, accuracy+5, DEX+3, weapon att.+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044217); // Scroll for Two-handed BW for ATT 100% - Improves attack on two-handed blunt weapon.\nSuccess rate:100%, weapon attack+2, STR+2
                ret.add(2044218); // Scroll for Two-Handed BW for Accuracy 65% - Improves Accuracy on Two-Handed Blunt Weapons.\nSuccess Rate: 65%, Accuracy+3, DEX+2, Weapon Attack+1
                ret.add(2044219); // Scroll for Two-Handed BW for Accuracy 15% - Improves Accuracy on Two-Handed Blunt Weapons.\nSuccess Rate: 15%, Accuracy+5, DEX+3, Weapon Attack+3
                ret.add(2044220); // Scroll for Two-handed BW for ATT 50% - Improves attack on two-handed blunt weapon.\nSuccess rate:50%, weapon attack+5, STR+3, DEX+1
            }
            case CATEGORY_SPEAR -> {
                ret.add(2044300); // Scroll for Spear for ATT - Improves attack on spear.\nSuccess rate:100%, weapon attack+1
                ret.add(2044301); // Scroll for Spear for ATT - Improves attack on spear.\nSuccess rate:60%, weapon attack+2, STR+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044302); // Scroll for Spear for ATT - Improves attack on spear.\nSuccess rate:10%, weapon attack+5, STR+3, weapon def.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044303); // Scroll for Spear for ATT - Improves attack on spear.\nSuccess rate:100%, weapon attack +5, STR+3, weapon def.+1
                ret.add(2044306); // Scroll for Spear for ATT - Improves attack on the Spear. nSuccess rate:65%, weapon attack+2, STR+1
                ret.add(2044307); // Scroll for Spear for ATT - Improves attack on the Spear. nSuccess rate:15%, weapon attack+5, STR+3, weapon def.+1
                ret.add(2044310); // Scroll for Spear for Accuracy 100% - Improves accuracy on spears.\nSuccess rate:100%, accuracy+1
                ret.add(2044312); // Scroll for Spear for Accuracy 60% - Improves accuracy on spears.\nSuccess rate:60%, accuracy+3, DEX+2, weapon att.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044314); // Scroll for Spear for Accuracy 10% - Improves accuracy on spears.\nSuccess rate:10%, accuracy+5, DEX+3, weapon att.+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044317); // Scroll for Spear for ATT 100% - Improves attack on spear.\nSuccess rate:100%, weapon attack+2, STR+2
                ret.add(2044318); // Scroll for Spears for Accuracy 65% - Improves Accuracy on Spears.\nSuccess Rate: 65%, Accuracy+3, DEX+2, Weapon Attack+1
                ret.add(2044319); // Scroll for Spears for Accuracy 15% - Improves Accuracy on Spears.\nSuccess Rate: 15%, Accuracy+5, DEX+3, Weapon Attack+3
                ret.add(2044320); // Scroll for Spear for ATT 50% - Improves attack on spear.\nSuccess rate:50%, weapon attack+5, STR+3, DEX+1
            }
            case CATEGORY_POLEARM -> {
                ret.add(2044400); // Scroll for Pole Arm for ATT - Improves attack on pole arm.\nSuccess rate:100%, weapon attack+1
                ret.add(2044401); // Scroll for Pole Arm for ATT - Improves attack on pole arm.\nSuccess rate:60%, weapon attack+2, STR+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044402); // Scroll for Pole Arm for ATT - Improves attack on pole arm.\nSuccess rate:10%, weapon attack+5, STR+3, weapon def.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044403); // Scroll for Pole Arm for ATT - Improves attack on pole arm.\nSuccess rate:100%, weapon attack +5, STR+3, weapon def.+1
                ret.add(2044406); // Scroll for Pole Arm for ATT - Improves attack on the Pole arm. nSuccess rate:65%, weapon attack+2, STR+1
                ret.add(2044407); // Scroll for Pole Arm for ATT - Improves attack on the Pole arm. nSuccess rate:15%, weapon attack+5, STR+3, weapon def.+1
                ret.add(2044410); // Scroll for Pole-Arm for Accuracy 100% - Improves accuracy on pole-arms.\nSuccess rate:100%, accuracy+1
                ret.add(2044412); // Scroll for Pole-Arm for Accuracy 60% - Improves accuracy on pole-arms.\nSuccess rate:60%, accuracy+3, DEX+2, weapon att.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044414); // Scroll for Pole-Arm for Accuracy 10% - Improves accuracy on pole-arms.\nSuccess rate:10%, accuracy+5, DEX+3, weapon att.+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044417); // Scroll for Pole Arm for ATT 100% - Improves attack on pole arm.\nSuccess rate:100%, weapon attack+2, STR+2
                ret.add(2044418); // Scroll for Polearm for Accuracy 65% - Improves Accuracy on Polearms.\nSuccess Rate: 65%, Accuracy+3, DEX+2, Weapon Attack+1
                ret.add(2044419); // Scroll for Polearm for Accuracy 15% - Improves Accuracy on Polearms.\nSuccess Rate: 15%, Accuracy+5, DEX+3, Weapon Attack+3
                ret.add(2044420); // Scroll for Pole Arm for ATT 50% - Improves attack on pole arm.\nSuccess rate:50%, weapon attack+5, STR+3, DEX+1
            }
            case CATEGORY_BOW -> {
                ret.add(2044500); // Scroll for Bow for ATT - Improves attack on bow.\nSuccess rate:100%, weapon attack+1
                ret.add(2044501); // Scroll for Bow for ATT - Improves attack on bow.\nSuccess rate: 60%, weapon attack+2, accuracy +1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044502); // Scroll for Bow for ATT - Improves attack on bow.\nSuccess rate:10%, weapon attack+5, accuracy+3, DEX+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044503); // Scroll for Bow for ATT - Improves attack on bow.\nSuccess rate:100%, weapon attack +5, accuracy +3, DEX+1
                ret.add(2044506); // Scroll for Bow for ATT - Improves attack on the Bow. nSuccess rate:65%, weapon attack+2, accuracy+1
                ret.add(2044507); // Scroll for Bow for ATT - Improves attack on the Bow. nSuccess rate:15%, weapon attack+5, accuracy+3, DEX+1
                ret.add(2044512); // Scroll for Bow for ATT 100% - Improves attack on bow.\nSuccess rate: 100%, weapon attack+2, accuracy +3
                ret.add(2044513); // Scroll for Bow for ATT 50% - Improves attack on bow.\nSuccess rate:50%, weapon attack +5, DEX+1, STR+1
            }
            case CATEGORY_CROSSBOW -> {
                ret.add(2044600); // Scroll for Crossbow for ATT - Improves attack on crossbow.\nSuccess rate:100%, weapon attack+1
                ret.add(2044601); // Scroll for Crossbow for ATT - Improves attack on crossbow.\nSuccess rate:60%, weapon attack+2, accuracy+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044602); // Scroll for Crossbow for ATT - Improves attack on crossbow.\nSuccess rate:10%, weapon attack+5, accuracy+3, DEX+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044603); // Scroll for Crossbow for ATT - Improves attack on crossbow.\nSuccess rate:100%, weapon attack+5, accuracy+3, DEX+1
                ret.add(2044606); // Scroll for Crossbow for ATT - Improves attack on the Crossbow. nSuccess rate:65%, weapon attack+2, accuracy+1
                ret.add(2044607); // Scroll for Crossbow for ATT - Improves attack on the Crossbow. nSuccess rate:15%, weapon attack+5, accuracy+3, DEX+1
                ret.add(2044612); // Scroll for Crossbow for ATT 100% - Improves attack on crossbow.\nSuccess rate:100%, weapon attack+2, accuracy+3
                ret.add(2044613); // Scroll for Crossbow for ATT 50% - Improves attack on crossbow.\nSuccess rate:50%, weapon attack+5, DEX+1, STR+1
            }
            case CATEGORY_CLAW -> {
                ret.add(2044700); // Scroll for Claw for ATT - Improves attack on claw.\nSuccess rate:100%, weapon attack+1
                ret.add(2044701); // Scroll for Claw for ATT - Improves attack on claw.\nSuccess rate:60%, weapon attack+2, accuracy+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044702); // Scroll for Claw for ATT - Improves attack on claw.\nSuccess rate:10%, weapon attack+5, accuracy+3, LUK+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044703); // Scroll for Claw for ATT - Improves attack on claw.\nSuccess rate:100%, weapon attack+5, accuracy+3, LUK+1
                ret.add(2044706); // Scroll for Claw for ATT - Improves attack on the Claw. nSuccess rate:65%, weapon attack+2, accuracy+1
                ret.add(2044707); // Scroll for Claw for ATT - Improves attack on the Claw. nSuccess rate:15%, weapon attack+5, accuracy+3, LUK+1
                ret.add(2044712); // Scroll for Claw for ATT 100% - Improves attack on claw.\nSuccess rate:100%, weapon attack+2, accuracy+3
                ret.add(2044713); // Scroll for Claw for ATT 50% - Improves attack on claw.\nSuccess rate:50%, weapon attack+5, LUK+1, DEX+1
            }
            case CATEGORY_KNUCKLE -> {
                ret.add(2044800); // Scroll for Knuckler for Attack 100% - Improves attack on Knucklers.\nSuccess rate:100%, weapon att. +1
                ret.add(2044801); // Scroll for Knuckler for Attack 60% - Improves attack on Knucklers.\nSuccess rate:60%, weapon att. +2, STR+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044802); // Scroll for Knuckler for ATT - Improves attack on Knucklers.\nSuccess rate:10%, weapon att. +5, STR+3, weapon def. +1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044805); // Scroll for Knuckle for Accuracy 100% - Improves accuracy on knuckles.\nSuccess rate:100%, accuracy+1
                ret.add(2044807); // Scroll for Knuckle for Accuracy 60% - Improves accuracy on knuckles.\nSuccess rate:60%, accuracy+3, DEX+2, weapon att.+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044809); // Scroll for Knuckle for Accuracy 10% - Improves accuracy on knuckles.\nSuccess rate:10%, accuracy+5, DEX+3, weapon att.+3. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044811); // Scroll for Knuckles for ATT 65% - Improves ATT on Knuckles.\nSuccess Rate:65%, Weapon Att+2, STR+1
                ret.add(2044812); // Scroll for Knuckles for ATT 15% - Improves ATT on Knuckles.\nSuccess Rate:15%, Weapon Att+5, STR+3, Weapon DEF+1
                ret.add(2044813); // Scroll for Knuckles for Accuracy 65% - Improves Accuracy on Knuckles.\nSuccess Rate:65%, Accuracy+3, DEX+2, Weapon Att+1
                ret.add(2044814); // Scroll for Knuckles for Accuracy 15% - Improves Accuracy on Knuckles.\nSuccess Rate:15%, Accuracy+5, DEX+3, Weapon Att+3
                ret.add(2044815); // Scroll for Knuckler for Attack 100% - Improves attack on Knucklers.\nSuccess rate: 100%. Weapon Attack +2, STR +1
                ret.add(2044817); // Scroll for Knuckler for Attack 50% - Improves attack on Knucklers.\nSuccess rate: 50%. Weapon Attack +5, STR +3,  DEX +1
            }
            case CATEGORY_PISTOL -> {
                ret.add(2044900); // Scroll for Gun for Attack 100% - Improves attack on Guns.\nSuccess rate:100%, weapon att. +1
                ret.add(2044901); // Scroll for Gun for Attack 60% - Improves attack on Guns.\nSuccess rate:60%, weapon att. +2, accuracy+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044902); // Scroll for Gun for ATT - Improves attack on Guns.\nSuccess rate:10%, weapon att. +5, accuracy+3, DEX+1. The success rate of this scroll can be enhanced by Vega's Spell.
                ret.add(2044906); // Scroll for Gun for ATT 65% - Improves ATT on guns.\nSuccess Rate:65%, Weapon Att.+2, Accuracy+1
                ret.add(2044907); // Scroll for Gun for ATT 15% - Improves ATT on guns.\nSuccess Rate:15%, Weapon Att.+5, Accuracy+3, DEX+1
                ret.add(2044908); // Scroll for Gun for Attack 100% - Improves attack on Guns.\nSuccess rate: 100%. Weapon Attack +2, Accuracy +1
                ret.add(2044910); // Scroll for Gun for Attack 50% - Improves attack on Guns.\nSuccess rate: 50%. Weapon Attack +5, LUK +1, DEX +1
            }
            default -> {
                // whoops
            }
        }

        // Add to cache
        scrollsByEquipCategory.put(equipCategory, ret);

        return scrollsByEquipCategory.get(equipCategory);
    }
}
