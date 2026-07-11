package client.inventory;

import client.Character;
import client.Client;
import client.inventory.manipulator.InventoryManipulator;
import config.YamlConfig;
import constants.game.GameConstants;
import constants.id.ItemId;
import constants.inventory.ItemConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ItemInformationProvider;
import server.MakerItemFactory;
import tools.PacketCreator;
import tools.Pair;
import tools.Randomizer;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MakerService {
    private static final Logger log = LoggerFactory.getLogger(MakerService.class);
    private static final ItemInformationProvider ii = ItemInformationProvider.getInstance();

    // checks and prevents hackers from PE'ing Maker operations with invalid operations
    public static boolean removeOddMakerReagents(int toCreate, Map<Integer, Short> reagentids) {
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

                if (tableRid == null) {
                    reagentType.put(type, curRid);
                    continue;
                }

                if (tableRid < curRid) {
                    toRemove.add(tableRid);
                    reagentType.put(type, curRid);
                    continue;
                }

                toRemove.add(curRid);
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

    public static int getMakerReagentSlots(int itemId) {
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

    public static Pair<Integer, List<Pair<Integer, Integer>>> generateDisassemblyInfo(int itemId) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
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

    public static short getCreateStatus(Client c, MakerItemFactory.MakerItemCreateEntry recipe) {
        if (recipe.isInvalid()) {
            return -1;
        }

        if (!hasItems(c, recipe)) {
            return 1;
        }

        if (c.getPlayer().getMeso() < recipe.getCost()) {
            return 2;
        }

        if (c.getPlayer().getLevel() < recipe.getReqLevel()) {
            return 3;
        }

        if (getMakerSkillLevel(c.getPlayer()) < recipe.getReqSkillLevel()) {
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
            return 5;
        }

        return 0;
    }

    public static boolean hasItems(Client c, MakerItemFactory.MakerItemCreateEntry recipe) {
        for (Pair<Integer, Integer> p : recipe.getReqItems()) {
            int itemId = p.getLeft();
            if (c.getPlayer().getInventory(ItemConstants.getInventoryType(itemId)).countById(itemId) < p.getRight()) {
                return false;
            }
        }
        return true;
    }

    public static void applyMakerResult(Client c, short createStatus, int toCreate, MakerItemFactory.MakerItemCreateEntry recipe, int toDisassemble, short pos, int stimulantid, Map<Integer, Short> reagentids, boolean makerSucceeded, int type) {
        switch (createStatus) {
            case -1:// non-available for Maker itemid has been tried to forge
                log.warn("Chr {} tried to craft itemid {} using the Maker skill.", c.getPlayer().getName(), toCreate);
                c.sendPacket(PacketCreator.serverNotice(1, "The requested item could not be crafted on this operation."));
                c.sendPacket(PacketCreator.makerEnableActions());
                break;

            case 1: // no items
                c.sendPacket(PacketCreator.serverNotice(1, "You don't have all required items in your inventory to make " + ii.getName(toCreate) + "."));
                c.sendPacket(PacketCreator.makerEnableActions());
                break;

            case 2: // no meso
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
                if (toDisassemble != -1) {
                    InventoryManipulator.removeFromSlot(c, InventoryType.EQUIP, pos, (short) 1, false);
                } else {
                    for (Pair<Integer, Integer> pair : recipe.getReqItems()) {
                        c.getAbstractPlayerInteraction().gainItem(pair.getLeft(), (short) -pair.getRight(), false);
                    }
                }

                int cost = recipe.getCost();
                if (stimulantid == -1 && reagentids.isEmpty()) {
                    if (cost > 0) {
                        c.getPlayer().gainMeso(-cost, false);
                        for (Pair<Integer, Integer> pair : recipe.getGainItems()) {
                            c.getPlayer().setCS(true);
                            int luck = 0;
                            if (pair.getLeft() - 4250000 < 2000 && pair.getLeft() > 4250000) {
                                int rand = Randomizer.rand(1, 100);
                                if (pair.getLeft() % 10 == 0) {
                                    if (rand == 1) {
                                        luck = 2;
                                    }
                                    else if (rand < 12) {
                                        luck = 1;
                                    }
                                } else if (pair.getLeft() % 10 == 1) {
                                    if (rand < 6)
                                        luck = 1;
                                }
                            }

                            c.getAbstractPlayerInteraction().gainItem(pair.getLeft() + luck, (pair.getRight().shortValue()), false);
                            c.getPlayer().setCS(false);
                        }
                    }
                } else {
                    toCreate = recipe.getGainItems().get(0).getLeft();

                    if (stimulantid != -1) {
                        c.getAbstractPlayerInteraction().gainItem(stimulantid, (short) -1, false);
                    }
                    if (!reagentids.isEmpty()) {
                        for (Map.Entry<Integer, Short> r : reagentids.entrySet()) {
                            c.getAbstractPlayerInteraction().gainItem(r.getKey(), (short) (-1 * r.getValue()), false);
                        }
                    }

                    if (cost > 0) {
                        c.getPlayer().gainMeso(-cost, false);
                    }
                    makerSucceeded = MakerService.addBoostedMakerItem(c, toCreate, stimulantid, reagentids);
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
    }

    public static boolean addBoostedMakerItem(Client c, int itemid, int stimulantid, Map<Integer, Short> reagentids) {
        if (stimulantid != -1 && !ItemInformationProvider.rollSuccessChance(90.0)) {
            return false;
        }

        Item item = ii.getEquipById(itemid);
        if (item == null) {
            return false;
        }

        Equip eqp = (Equip) item;
        if (ItemConstants.isAccessory(item.getItemId()) && eqp.getUpgradeSlots() <= 0) {
            eqp.setUpgradeSlots(3);
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
                ii.scrollOptionEquipWithChaos(eqp, sh, false);
            }

            for (Short sh : randOption) {
                ii.scrollOptionEquipWithChaos(eqp, sh, true);
            }
        }

        if (stimulantid != -1) {
            eqp = ii.randomizeUpgradeStats(eqp);
        }

        InventoryManipulator.addFromDrop(c, item, false, -1);
        return true;
    }

    public static boolean addBoostedMakerItemSimple(Client c, int itemid, int stimulantid, Map<Integer, Short> reagentids) {
        if (stimulantid != -1 && !ItemInformationProvider.rollSuccessChance(90.0)) {
            return false;
        }

        Item item = ii.getEquipById(itemid);
        if (item == null) {
            return false;
        }

        Equip eqp = (Equip) item;

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

            ItemInformationProvider.improveEquipStats(eqp, false);

            for (Short sh : randStat) {
                ii.scrollOptionEquipWithChaos(eqp, sh, false);
            }

            for (Short sh : randOption) {
                ii.scrollOptionEquipWithChaos(eqp, sh, true);
            }
        }

        if (stimulantid != -1) {
            eqp = ii.randomizeUpgradeStats(eqp);
        }

        c.getAbstractPlayerInteraction().gainItem(itemid, (short)-1, false);
        InventoryManipulator.addFromDrop(c, item, false, -1);
        return true;
    }
}
