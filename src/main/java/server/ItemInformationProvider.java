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
                ret += c.getPlayer().getSkillLevel(SkillFactory.getSkill(NightWalker.CLAW_MASTERY)) * 10;
            } else {
                ret += c.getPlayer().getSkillLevel(SkillFactory.getSkill(Assassin.CLAW_MASTERY)) * 10;
            }
        } else if (ItemConstants.isBullet(itemId)) {
            ret += c.getPlayer().getSkillLevel(SkillFactory.getSkill(Gunslinger.GUN_MASTERY)) * 10;
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
                } else {
                    ret = 100;
                }
            } else {
                ret = (short) DataTool.getInt(smEntry);
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

    private static short chscrollRandomizedStat(int range) {
        return (short) Randomizer.rand(-range, range);
    }

    public void scrollOptionEquipWithChaos(Equip nEquip, int range, boolean option) {
        // option: watk, matk, wdef, mdef, spd, jump, hp, mp
        //   stat: dex, luk, str, int, avoid, acc

        if (!option) {
            if (nEquip.getStr() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setStr(getMaximumShortMaxIfOverflow(nEquip.getStr(), (nEquip.getStr() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setStr(getMaximumShortMaxIfOverflow(0, (nEquip.getStr() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getDex() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setDex(getMaximumShortMaxIfOverflow(nEquip.getDex(), (nEquip.getDex() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setDex(getMaximumShortMaxIfOverflow(0, (nEquip.getDex() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getInt() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setInt(getMaximumShortMaxIfOverflow(nEquip.getInt(), (nEquip.getInt() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setInt(getMaximumShortMaxIfOverflow(0, (nEquip.getInt() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getLuk() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setLuk(getMaximumShortMaxIfOverflow(nEquip.getLuk(), (nEquip.getLuk() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setLuk(getMaximumShortMaxIfOverflow(0, (nEquip.getLuk() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getAcc() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setAcc(getMaximumShortMaxIfOverflow(nEquip.getAcc(), (nEquip.getAcc() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setAcc(getMaximumShortMaxIfOverflow(0, (nEquip.getAcc() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getAvoid() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setAvoid(getMaximumShortMaxIfOverflow(nEquip.getAvoid(), (nEquip.getAvoid() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setAvoid(getMaximumShortMaxIfOverflow(0, (nEquip.getAvoid() + chscrollRandomizedStat(range))));
                }
            }
        } else {
            if (nEquip.getWatk() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setWatk(getMaximumShortMaxIfOverflow(nEquip.getWatk(), (nEquip.getWatk() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setWatk(getMaximumShortMaxIfOverflow(0, (nEquip.getWatk() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getWdef() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setWdef(getMaximumShortMaxIfOverflow(nEquip.getWdef(), (nEquip.getWdef() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setWdef(getMaximumShortMaxIfOverflow(0, (nEquip.getWdef() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getMatk() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setMatk(getMaximumShortMaxIfOverflow(nEquip.getMatk(), (nEquip.getMatk() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setMatk(getMaximumShortMaxIfOverflow(0, (nEquip.getMatk() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getMdef() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setMdef(getMaximumShortMaxIfOverflow(nEquip.getMdef(), (nEquip.getMdef() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setMdef(getMaximumShortMaxIfOverflow(0, (nEquip.getMdef() + chscrollRandomizedStat(range))));
                }
            }

            if (nEquip.getSpeed() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setSpeed(getMaximumShortMaxIfOverflow(nEquip.getSpeed(), (nEquip.getSpeed() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setSpeed(getMaximumShortMaxIfOverflow(0, (nEquip.getSpeed() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getJump() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setJump(getMaximumShortMaxIfOverflow(nEquip.getJump(), (nEquip.getJump() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setJump(getMaximumShortMaxIfOverflow(0, (nEquip.getJump() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getHp() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setHp(getMaximumShortMaxIfOverflow(nEquip.getHp(), (nEquip.getHp() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setHp(getMaximumShortMaxIfOverflow(0, (nEquip.getHp() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getMp() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setMp(getMaximumShortMaxIfOverflow(nEquip.getMp(), (nEquip.getMp() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setMp(getMaximumShortMaxIfOverflow(0, (nEquip.getMp() + chscrollRandomizedStat(range))));
                }
            }
        }
    }

    private void scrollEquipWithChaos(Equip nEquip, int range) {
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
                        temp = curStr + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getStr() + chscrollRandomizedStat(range);
                    }

                    curStr = getMaximumShortMaxIfOverflow(temp, curStr);
                }

                if (nEquip.getDex() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curDex + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getDex() + chscrollRandomizedStat(range);
                    }

                    curDex = getMaximumShortMaxIfOverflow(temp, curDex);
                }

                if (nEquip.getInt() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curInt + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getInt() + chscrollRandomizedStat(range);
                    }

                    curInt = getMaximumShortMaxIfOverflow(temp, curInt);
                }

                if (nEquip.getLuk() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curLuk + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getLuk() + chscrollRandomizedStat(range);
                    }

                    curLuk = getMaximumShortMaxIfOverflow(temp, curLuk);
                }

                if (nEquip.getWatk() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curWatk + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getWatk() + chscrollRandomizedStat(range);
                    }

                    curWatk = getMaximumShortMaxIfOverflow(temp, curWatk);
                }

                if (nEquip.getWdef() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curWdef + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getWdef() + chscrollRandomizedStat(range);
                    }

                    curWdef = getMaximumShortMaxIfOverflow(temp, curWdef);
                }

                if (nEquip.getMatk() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curMatk + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getMatk() + chscrollRandomizedStat(range);
                    }

                    curMatk = getMaximumShortMaxIfOverflow(temp, curMatk);
                }

                if (nEquip.getMdef() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curMdef + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getMdef() + chscrollRandomizedStat(range);
                    }

                    curMdef = getMaximumShortMaxIfOverflow(temp, curMdef);
                }

                if (nEquip.getAcc() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curAcc + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getAcc() + chscrollRandomizedStat(range);
                    }

                    curAcc = getMaximumShortMaxIfOverflow(temp, curAcc);
                }

                if (nEquip.getAvoid() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curAvoid + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getAvoid() + chscrollRandomizedStat(range);
                    }

                    curAvoid = getMaximumShortMaxIfOverflow(temp, curAvoid);
                }

                if (nEquip.getSpeed() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curSpeed + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getSpeed() + chscrollRandomizedStat(range);
                    }

                    curSpeed = getMaximumShortMaxIfOverflow(temp, curSpeed);
                }

                if (nEquip.getJump() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curJump + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getJump() + chscrollRandomizedStat(range);
                    }

                    curJump = getMaximumShortMaxIfOverflow(temp, curJump);
                }

                if (nEquip.getHp() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curHp + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getHp() + chscrollRandomizedStat(range);
                    }

                    curHp = getMaximumShortMaxIfOverflow(temp, curHp);
                }

                if (nEquip.getMp() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curMp + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getMp() + chscrollRandomizedStat(range);
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
                    nEquip.setStr(getMaximumShortMaxIfOverflow(nEquip.getStr(), (nEquip.getStr() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setStr(getMaximumShortMaxIfOverflow(0, (nEquip.getStr() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getDex() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setDex(getMaximumShortMaxIfOverflow(nEquip.getDex(), (nEquip.getDex() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setDex(getMaximumShortMaxIfOverflow(0, (nEquip.getDex() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getInt() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setInt(getMaximumShortMaxIfOverflow(nEquip.getInt(), (nEquip.getInt() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setInt(getMaximumShortMaxIfOverflow(0, (nEquip.getInt() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getLuk() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setLuk(getMaximumShortMaxIfOverflow(nEquip.getLuk(), (nEquip.getLuk() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setLuk(getMaximumShortMaxIfOverflow(0, (nEquip.getLuk() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getWatk() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setWatk(getMaximumShortMaxIfOverflow(nEquip.getWatk(), (nEquip.getWatk() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setWatk(getMaximumShortMaxIfOverflow(0, (nEquip.getWatk() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getWdef() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setWdef(getMaximumShortMaxIfOverflow(nEquip.getWdef(), (nEquip.getWdef() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setWdef(getMaximumShortMaxIfOverflow(0, (nEquip.getWdef() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getMatk() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setMatk(getMaximumShortMaxIfOverflow(nEquip.getMatk(), (nEquip.getMatk() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setMatk(getMaximumShortMaxIfOverflow(0, (nEquip.getMatk() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getMdef() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setMdef(getMaximumShortMaxIfOverflow(nEquip.getMdef(), (nEquip.getMdef() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setMdef(getMaximumShortMaxIfOverflow(0, (nEquip.getMdef() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getAcc() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setAcc(getMaximumShortMaxIfOverflow(nEquip.getAcc(), (nEquip.getAcc() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setAcc(getMaximumShortMaxIfOverflow(0, (nEquip.getAcc() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getAvoid() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setAvoid(getMaximumShortMaxIfOverflow(nEquip.getAvoid(), (nEquip.getAvoid() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setAvoid(getMaximumShortMaxIfOverflow(0, (nEquip.getAvoid() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getSpeed() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setSpeed(getMaximumShortMaxIfOverflow(nEquip.getSpeed(), (nEquip.getSpeed() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setSpeed(getMaximumShortMaxIfOverflow(0, (nEquip.getSpeed() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getJump() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setJump(getMaximumShortMaxIfOverflow(nEquip.getJump(), (nEquip.getJump() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setJump(getMaximumShortMaxIfOverflow(0, (nEquip.getJump() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getHp() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setHp(getMaximumShortMaxIfOverflow(nEquip.getHp(), (nEquip.getHp() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setHp(getMaximumShortMaxIfOverflow(0, (nEquip.getHp() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getMp() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setMp(getMaximumShortMaxIfOverflow(nEquip.getMp(), (nEquip.getMp() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setMp(getMaximumShortMaxIfOverflow(0, (nEquip.getMp() + chscrollRandomizedStat(range))));
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
        if (eqStats == null || eqStats.get("tuc") == 0 ) {
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
                        case ItemId.CHAOS_SCROll_60:
                        case ItemId.LIAR_TREE_SAP:
                        case ItemId.MAPLE_SYRUP:
                            scrollEquipWithChaos(nEquip, YamlConfig.config.server.CHSCROLL_STAT_RANGE);
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
        int tdex = chr.getDex(), tstr = chr.getStr(), tint = chr.getInt(), tluk = chr.getLuk(), fame = chr.getFame();
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
            if (reqLevel > chr.getLevel()) {
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
}
