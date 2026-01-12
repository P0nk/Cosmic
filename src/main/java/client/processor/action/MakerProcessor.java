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
package client.processor.action;

import client.Character;
import client.Client;
import client.inventory.Equip;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.manipulator.InventoryManipulator;
import config.YamlConfig;
import constants.game.GameConstants;
import constants.id.ItemId;
import constants.inventory.ItemConstants;
import net.packet.InPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ItemInformationProvider;
import server.MakerItemFactory;
import server.MakerItemFactory.MakerItemCreateEntry;
import tools.PacketCreator;
import tools.Pair;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Ronan
 */
public class MakerProcessor {
    private static final Logger log = LoggerFactory.getLogger(MakerProcessor.class);
    private static final ItemInformationProvider ii = ItemInformationProvider.getInstance();

    public static void makerAction(InPacket p, Client c) {
        System.out.println("[MakerProcessorDebug] START: Received Maker Action Packet.");

        if (c.tryacquireClient()) {
            try {
                int type = p.readInt();
                System.out.println("[MakerProcessorDebug] Packet Type: " + type);

                int toCreate = p.readInt();
                System.out.println("[MakerProcessorDebug] Target ItemID (toCreate): " + toCreate);

                int toDisassemble = -1, pos = -1;
                boolean makerSucceeded = true;

                MakerItemCreateEntry recipe;
                Map<Integer, Short> reagentids = new LinkedHashMap<>();
                int stimulantid = -1;

                if (type == 3) {    // building monster crystal
                    System.out.println("[MakerProcessorDebug] Action: Monster Crystal (Type 3)");
                    int fromLeftover = toCreate;
                    System.out.println("[MakerProcessorDebug] fromLeftover ID: " + fromLeftover);

                    toCreate = ii.getMakerCrystalFromLeftover(toCreate);
                    System.out.println("[MakerProcessorDebug] Converted toCreate ID: " + toCreate);

                    if (toCreate == -1) {
                        System.out.println("[MakerProcessorDebug] FAIL: Leftover unavailable.");
                        c.sendPacket(PacketCreator.serverNotice(1, ii.getName(fromLeftover) + " is unavailable for Monster Crystal conversion."));
                        c.sendPacket(PacketCreator.makerEnableActions());
                        return;
                    }

                    recipe = MakerItemFactory.generateLeftoverCrystalEntry(fromLeftover, toCreate);

                } else if (type == 4) {  // disassembling
                    System.out.println("[MakerProcessorDebug] Action: Disassembling (Type 4)");
                    p.readInt(); // 1... probably inventory type
                    pos = p.readInt();
                    System.out.println("[MakerProcessorDebug] Inventory Slot Position: " + pos);

                    Item it = c.getPlayer().getInventory(InventoryType.EQUIP).getItem((short) pos);
                    System.out.println("[MakerProcessorDebug] Item at Slot " + pos + ": " + (it == null ? "NULL" : it.getItemId()));

                    if (it != null && it.getItemId() == toCreate) {
                        toDisassemble = toCreate;
                        System.out.println("[MakerProcessorDebug] Valid Item found for disassembly: " + toDisassemble);

                        Pair<Integer, List<Pair<Integer, Integer>>> pair = generateDisassemblyInfo(toDisassemble);

                        if (pair != null) {
                            System.out.println("[MakerProcessorDebug] Disassembly Info Found - Fee: " + pair.getLeft() + ", Gains: " + pair.getRight().size());
                            recipe = MakerItemFactory.generateDisassemblyCrystalEntry(toDisassemble, pair.getLeft(), pair.getRight());
                        } else {
                            System.out.println("[MakerProcessorDebug] FAIL: Disassembly info is null.");
                            c.sendPacket(PacketCreator.serverNotice(1, ii.getName(toCreate) + " is unavailable for Monster Crystal disassembly."));
                            c.sendPacket(PacketCreator.makerEnableActions());
                            return;
                        }
                    } else {
                        System.out.println("[MakerProcessorDebug] FAIL: Item mismatch (Packet ID: " + toCreate + " vs Slot ID: " + (it == null ? "null" : it.getItemId()) + ")");
                        c.sendPacket(PacketCreator.serverNotice(1, "An unknown error occurred when trying to apply that item for disassembly."));
                        c.sendPacket(PacketCreator.makerEnableActions());
                        return;
                    }
                } else {
                    System.out.println("[MakerProcessorDebug] Action: Standard Crafting (Type " + type + ")");

                    if (ItemConstants.isEquipment(toCreate)) {   // only equips uses stimulant and reagents
                        System.out.println("[MakerProcessorDebug] Target is Equipment.");
                        if (p.readByte() != 0) {  // stimulant
                            stimulantid = ii.getMakerStimulant(toCreate);
                            System.out.println("[MakerProcessorDebug] Stimulant ID from WZ: " + stimulantid);

                            if (!c.getAbstractPlayerInteraction().haveItem(stimulantid)) {
                                System.out.println("[MakerProcessorDebug] FAIL: Player missing stimulant " + stimulantid + ". Setting to -1.");
                                stimulantid = -1;
                            } else {
                                System.out.println("[MakerProcessorDebug] Player has stimulant: " + stimulantid);
                            }
                        }

                        int reagents = Math.min(p.readInt(), getMakerReagentSlots(toCreate));
                        System.out.println("[MakerProcessorDebug] Reagent Count Limit: " + reagents);

                        for (int i = 0; i < reagents; i++) {  // crystals
                            int reagentid = p.readInt();
                            System.out.println("[MakerProcessorDebug] Packet reading Reagent ID: " + reagentid);

                            if (ItemConstants.isMakerReagent(reagentid)) {
                                Short rs = reagentids.get(reagentid);
                                if (rs == null) {
                                    reagentids.put(reagentid, (short) 1);
                                } else {
                                    reagentids.put(reagentid, (short) (rs + 1));
                                }
                            } else {
                                System.out.println("[MakerProcessorDebug] Item " + reagentid + " is NOT a valid Maker Reagent.");
                            }
                        }
                        System.out.println("[MakerProcessorDebug] Reagents Map before inventory check: " + reagentids.toString());

                        List<Pair<Integer, Short>> toUpdate = new LinkedList<>();
                        for (Map.Entry<Integer, Short> r : reagentids.entrySet()) {
                            // DEBUGGING THE QUANTITY CHECK HERE
                            int needed = r.getValue();
                            int itemId = r.getKey();
                            int qty = c.getAbstractPlayerInteraction().getItemQuantity(itemId);

                            System.out.println("[MakerProcessorDebug] Reagent Check [ID: " + itemId + "] -> Need: " + needed + " | Has: " + qty);

                            if (qty < needed) {
                                System.out.println("[MakerProcessorDebug] WARNING: Player has insufficient Reagent " + itemId);
                                toUpdate.add(new Pair<>(itemId, (short) qty));
                            }
                        }

                        // remove those not present on player inventory
                        if (!toUpdate.isEmpty()) {
                            System.out.println("[MakerProcessorDebug] Cleaning up invalid reagents...");
                            for (Pair<Integer, Short> rp : toUpdate) {
                                if (rp.getRight() > 0) {
                                    reagentids.put(rp.getLeft(), rp.getRight());
                                } else {
                                    reagentids.remove(rp.getLeft());
                                }
                            }
                        }

                        if (!reagentids.isEmpty()) {
                            if (!removeOddMakerReagents(toCreate, reagentids)) {
                                System.out.println("[MakerProcessorDebug] FAIL: removeOddMakerReagents returned false.");
                                c.sendPacket(PacketCreator.serverNotice(1, "You can only use WATK and MATK Strengthening Gems on weapon items."));
                                c.sendPacket(PacketCreator.makerEnableActions());
                                return;
                            }
                        }
                    }

                    recipe = MakerItemFactory.getItemCreateEntry(toCreate, stimulantid, reagentids);
                }

                if (recipe == null) {
                    System.out.println("[MakerProcessorDebug] CRITICAL: Recipe generation returned NULL.");
                } else {
                    System.out.println("[MakerProcessorDebug] Recipe Generated -> Cost: " + recipe.getCost() + ", ReqLevel: " + recipe.getReqLevel());
                }

                short createStatus = getCreateStatus(c, recipe);
                System.out.println("[MakerProcessorDebug] Validation Status Code: " + createStatus);

                switch (createStatus) {
                    case -1:// non-available for Maker itemid has been tried to forge
                        log.warn("Chr {} tried to craft itemid {} using the Maker skill.", c.getPlayer().getName(), toCreate);
                        c.sendPacket(PacketCreator.serverNotice(1, "The requested item could not be crafted on this operation."));
                        c.sendPacket(PacketCreator.makerEnableActions());
                        break;

                    case 1: // no items
                        System.out.println("[MakerProcessorDebug] FAIL: Case 1 - Missing Items.");
                        c.sendPacket(PacketCreator.serverNotice(1, "You don't have all required items in your inventory to make " + ii.getName(toCreate) + "."));
                        c.sendPacket(PacketCreator.makerEnableActions());
                        break;

                    case 2: // no meso
                        System.out.println("[MakerProcessorDebug] FAIL: Case 2 - Missing Mesos. Need: " + recipe.getCost() + " Have: " + c.getPlayer().getMeso());
                        c.sendPacket(PacketCreator.serverNotice(1, "You don't have enough mesos (" + GameConstants.numberWithCommas(recipe.getCost()) + ") to complete this operation."));
                        c.sendPacket(PacketCreator.makerEnableActions());
                        break;

                    case 3: // no req level
                        c.sendPacket(PacketCreator.serverNotice(1, "You don't have enough level to complete this operation."));
                        c.sendPacket(PacketCreator.makerEnableActions());
                        break;

                    case 4: // no req skill level
                        c.sendPacket(PacketCreator.serverNotice(1, "You don't have enough Maker level to complete this operation."));
                        c.sendPacket(PacketCreator.makerEnableActions());
                        break;

                    case 5: // inventory full
                        c.sendPacket(PacketCreator.serverNotice(1, "Your inventory is full."));
                        c.sendPacket(PacketCreator.makerEnableActions());
                        break;

                    default:
                        System.out.println("[MakerProcessorDebug] Validation Passed. Proceeding to Transaction.");
                        if (toDisassemble != -1) {
                            InventoryManipulator.removeFromSlot(c, InventoryType.EQUIP, (short) pos, (short) 1, false);
                        } else {
                            for (Pair<Integer, Integer> pair : recipe.getReqItems()) {
                                System.out.println("[MakerProcessorDebug] Consuming Req Item: " + pair.getLeft() + " Qty: " + pair.getRight());
                                c.getAbstractPlayerInteraction().gainItem(pair.getLeft(), (short) -pair.getRight(), false);
                            }
                        }

                        int cost = recipe.getCost();
                        if (stimulantid == -1 && reagentids.isEmpty()) {
                            if (cost > 0) {
                                c.getPlayer().gainMeso(-cost, false);
                            }

                            for (Pair<Integer, Integer> pair : recipe.getGainItems()) {
                                c.getPlayer().setCS(true);
                                System.out.println("[MakerProcessorDebug] Gaining Item (Simple): " + pair.getLeft() + " Qty: " + pair.getRight());
                                c.getAbstractPlayerInteraction().gainItem(pair.getLeft(), pair.getRight().shortValue(), false);
                                c.getPlayer().setCS(false);
                            }
                        } else {
                            toCreate = recipe.getGainItems().get(0).getLeft();

                            if (stimulantid != -1) {
                                c.getAbstractPlayerInteraction().gainItem(stimulantid, (short) -1, false);
                            }
                            if (!reagentids.isEmpty()) {
                                for (Map.Entry<Integer, Short> r : reagentids.entrySet()) {
                                    System.out.println("[MakerProcessorDebug] Consuming Reagent: " + r.getKey() + " Qty: " + r.getValue());
                                    c.getAbstractPlayerInteraction().gainItem(r.getKey(), (short) (-1 * r.getValue()), false);
                                }
                            }

                            if (cost > 0) {
                                c.getPlayer().gainMeso(-cost, false);
                            }

                            makerSucceeded = addBoostedMakerItem(c, toCreate, stimulantid, reagentids);
                            System.out.println("[MakerProcessorDebug] Boosted Creation Result: " + makerSucceeded);
                        }

                        // thanks inhyuk for noticing missing MAKER_RESULT packets
                        if (type == 3) {
                            c.sendPacket(PacketCreator.makerResultCrystal(recipe.getGainItems().get(0).getLeft(), recipe.getReqItems().get(0).getLeft()));
                        } else if (type == 4) {
                            c.sendPacket(PacketCreator.makerResultDesynth(recipe.getReqItems().get(0).getLeft(), recipe.getCost(), recipe.getGainItems()));
                        } else {
                            c.sendPacket(PacketCreator.makerResult(makerSucceeded, recipe.getGainItems().get(0).getLeft(), recipe.getGainItems().get(0).getRight(), recipe.getCost(), recipe.getReqItems(), stimulantid, new LinkedList<>(reagentids.keySet())));
                        }

                        c.sendPacket(PacketCreator.showMakerEffect(makerSucceeded));
                        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PacketCreator.showForeignMakerEffect(c.getPlayer().getId(), makerSucceeded), false);

                        if (toCreate == 4260003 && type == 3 && c.getPlayer().getQuestStatus(6033) == 1) {
                            c.getAbstractPlayerInteraction().setQuestProgress(6033, 1);
                        }
                }
            } finally {
                c.releaseClient();
                System.out.println("[MakerProcessorDebug] END: Client Released.");
            }
        } else {
            System.out.println("[MakerProcessorDebug] ERROR: Failed to acquire Client Lock.");
        }
    }

    // checks and prevents hackers from PE'ing Maker operations with invalid operations
    private static boolean removeOddMakerReagents(int toCreate, Map<Integer, Short> reagentids) {
        Map<Integer, Integer> reagentType = new LinkedHashMap<>();
        List<Integer> toRemove = new LinkedList<>();

        boolean isWeapon = ItemConstants.isWeapon(toCreate) || YamlConfig.config.server.USE_MAKER_PERMISSIVE_ATKUP;  // thanks Vcoc for finding a case where a weapon wouldn't be counted as such due to a bounding on isWeapon

        for (Map.Entry<Integer, Short> r : reagentids.entrySet()) {
            int curRid = r.getKey();
            int type = r.getKey() / 100;

            if (type < 42502 && !isWeapon) {     // only weapons should gain w.att/m.att from these.
                return false;   //toRemove.add(curRid);
            } else {
                Integer tableRid = reagentType.get(type);

                if (tableRid != null) {
                    if (tableRid < curRid) {
                        toRemove.add(tableRid);
                        reagentType.put(type, curRid);
                    } else {
                        toRemove.add(curRid);
                    }
                } else {
                    reagentType.put(type, curRid);
                }
            }
        }

        // removing less effective gems of repeated type
        for (Integer i : toRemove) {
            reagentids.remove(i);
        }

        // the Maker skill will use only one of each gem
        for (Integer i : reagentids.keySet()) {
            reagentids.put(i, (short) 1);
        }

        return true;
    }

    private static int getMakerReagentSlots(int itemId) {
        try {
            int eqpLevel = ii.getEquipLevelReq(itemId);

            if (eqpLevel < 78) {
                return 1;
            } else if (eqpLevel >= 78 && eqpLevel < 108) {
                return 2;
            } else {
                return 3;
            }
        } catch (NullPointerException npe) {
            return 0;
        }
    }

    private static Pair<Integer, List<Pair<Integer, Integer>>> generateDisassemblyInfo(int itemId) {
        int recvFee = ii.getMakerDisassembledFee(itemId);

        if (recvFee > -1) {
            List<Pair<Integer, Integer>> gains = ii.getMakerDisassembledItems(itemId);
            if (!gains.isEmpty()) {
                return new Pair<>(recvFee, gains);
            }
        }

        return null;
    }

    public static int getMakerSkillLevel(Character chr) {
        return chr.getSkillLevel((chr.getJob().getId() / 1000) * 10000000 + 1007);
    }

    private static short getCreateStatus(Client c, MakerItemCreateEntry recipe) {
        if (recipe.isInvalid()) {
            System.out.println("[MakerProcessorDebug] getCreateStatus: Recipe Invalid.");
            return -1;
        }

        if (!hasItems(c, recipe)) {
            System.out.println("[MakerProcessorDebug] getCreateStatus: Missing Items.");
            return 1;
        }

        if (c.getPlayer().getMeso() < recipe.getCost()) {
            System.out.println("[MakerProcessorDebug] getCreateStatus: Missing Mesos.");
            return 2;
        }

        if (c.getPlayer().getLevel() < recipe.getReqLevel()) {
            System.out.println("[MakerProcessorDebug] getCreateStatus: Low Level.");
            return 3;
        }

        if (getMakerSkillLevel(c.getPlayer()) < recipe.getReqSkillLevel()) {
            System.out.println("[MakerProcessorDebug] getCreateStatus: Low Skill Level.");
            return 4;
        }

        List<Integer> addItemids = new LinkedList<>();
        List<Integer> addQuantity = new LinkedList<>();
        List<Integer> rmvItemids = new LinkedList<>();
        List<Integer> rmvQuantity = new LinkedList<>();

        for (Pair<Integer, Integer> p : recipe.getReqItems()) {
            rmvItemids.add(p.getLeft());
            rmvQuantity.add(p.getRight());
        }

        for (Pair<Integer, Integer> p : recipe.getGainItems()) {
            addItemids.add(p.getLeft());
            addQuantity.add(p.getRight());
        }

        if (!c.getAbstractPlayerInteraction().canHoldAllAfterRemoving(addItemids, addQuantity, rmvItemids, rmvQuantity)) {
            System.out.println("[MakerProcessorDebug] getCreateStatus: Inventory Full.");
            return 5;
        }

        return 0;
    }

    private static boolean hasItems(Client c, MakerItemCreateEntry recipe) {
        System.out.println("[MakerProcessorDebug] --- Checking Recipe Ingredients (hasItems) ---");
        for (Pair<Integer, Integer> p : recipe.getReqItems()) {
            int itemId = p.getLeft();
            int required = p.getRight();
            InventoryType type = ItemConstants.getInventoryType(itemId);
            int count = c.getPlayer().getInventory(type).countById(itemId);

            System.out.println("[MakerProcessorDebug] ItemID: " + itemId);
            System.out.println("[MakerProcessorDebug] -> Target Inventory Type: " + type.name());
            System.out.println("[MakerProcessorDebug] -> Qty in Inventory: " + count);
            System.out.println("[MakerProcessorDebug] -> Qty Required: " + required);

            if (count < required) {
                System.out.println("[MakerProcessorDebug] -> FAIL: Not enough items.");
                return false;
            }
        }
        System.out.println("[MakerProcessorDebug] --- Recipe Ingredients Check Passed ---");
        return true;
    }

    private static boolean addBoostedMakerItem(Client c, int itemid, int stimulantid, Map<Integer, Short> reagentids) {
        if (stimulantid != -1 && !ItemInformationProvider.rollSuccessChance(90.0)) {
            System.out.println("[MakerProcessorDebug] Stimulant Failed (10% fail rate triggered).");
            return false;
        }

        Item item = ii.getEquipById(itemid);
        if (item == null) {
            System.out.println("[MakerProcessorDebug] ERROR: Created item is NULL.");
            return false;
        }

        Equip eqp = (Equip) item;
        if (ItemConstants.isAccessory(item.getItemId()) && eqp.getUpgradeSlots() <= 0) {
            eqp.setUpgradeSlots(3);
        }

        if (YamlConfig.config.server.USE_ENHANCED_CRAFTING == true) {
            if (!(c.getPlayer().isGM() && YamlConfig.config.server.USE_PERFECT_GM_SCROLL)) {
                eqp.setUpgradeSlots((byte) (eqp.getUpgradeSlots() + 1));
            }
            item = ItemInformationProvider.getInstance().scrollEquipWithId(eqp, ItemId.CHAOS_SCROll_60, true, ItemId.CHAOS_SCROll_60, c.getPlayer().isGM());
        }

        if (!reagentids.isEmpty()) {
            Map<String, Integer> stats = new LinkedHashMap<>();
            List<Short> randOption = new LinkedList<>();
            List<Short> randStat = new LinkedList<>();

            for (Map.Entry<Integer, Short> r : reagentids.entrySet()) {
                Pair<String, Integer> reagentBuff = ii.getMakerReagentStatUpgrade(r.getKey());

                if (reagentBuff != null) {
                    String s = reagentBuff.getLeft();

                    if (s.substring(0, 4).contains("rand")) {
                        if (s.substring(4).equals("Stat")) {
                            randStat.add((short) (reagentBuff.getRight() * r.getValue()));
                        } else {
                            randOption.add((short) (reagentBuff.getRight() * r.getValue()));
                        }
                    } else {
                        String stat = s.substring(3);

                        if (!stat.equals("ReqLevel")) {    // improve req level... really?
                            switch (stat) {
                                case "MaxHP":
                                    stat = "MHP";
                                    break;

                                case "MaxMP":
                                    stat = "MMP";
                                    break;
                            }

                            Integer d = stats.get(stat);
                            if (d == null) {
                                stats.put(stat, reagentBuff.getRight() * r.getValue());
                            } else {
                                stats.put(stat, d + (reagentBuff.getRight() * r.getValue()));
                            }
                        }
                    }
                }
            }

            ItemInformationProvider.improveEquipStats(eqp, stats);

            for (Short sh : randStat) {
                ii.scrollOptionEquipWithChaos(eqp, sh, false, false);
            }

            for (Short sh : randOption) {
                ii.scrollOptionEquipWithChaos(eqp, sh, true, false);
            }
        }

        if (stimulantid != -1) {
            eqp = ii.randomizeUpgradeStats(eqp);
        }

        InventoryManipulator.addFromDrop(c, item, false, -1);
        return true;
    }
}