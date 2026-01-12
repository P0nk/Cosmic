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

import config.YamlConfig;
import constants.inventory.EquipType;
import tools.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Jay Estrella, Ronan
 */
public class MakerItemFactory {
    private static final ItemInformationProvider ii = ItemInformationProvider.getInstance();

    public static MakerItemCreateEntry getItemCreateEntry(int toCreate, int stimulantid, Map<Integer, Short> reagentids) {
        System.out.println("[MakerItemFactoryDebug] getItemCreateEntry invoked. TargetItemID: " + toCreate + ", StimulantID: " + stimulantid);

        MakerItemCreateEntry makerEntry = ii.getMakerItemEntry(toCreate);

        if (makerEntry.isInvalid()) {
            System.out.println("[MakerItemFactoryDebug] MakerEntry retrieved is invalid for ItemID: " + toCreate);
            return makerEntry;
        }

        System.out.println("[MakerItemFactoryDebug] Base Recipe Cost: " + makerEntry.cost);

        // THEY DECIDED FOR SOME BIZARRE PATTERN ON THE FEE THING, ALMOST RANDOMIZED.
        if (stimulantid != -1) {
            double stimFee = getMakerStimulantFee(toCreate);
            System.out.println("[MakerItemFactoryDebug] Applied Stimulant Fee: " + stimFee);
            makerEntry.addCost(stimFee);
        }

        if (!reagentids.isEmpty()) {
            System.out.println("[MakerItemFactoryDebug] Processing " + reagentids.size() + " reagents.");
            for (Entry<Integer, Short> r : reagentids.entrySet()) {
                int reagentLvl = (r.getKey() % 10) + 1;
                double feePerUnit = getMakerReagentFee(toCreate, reagentLvl);
                double totalReagentFee = feePerUnit * r.getValue();

                System.out.println("[MakerItemFactoryDebug] Reagent: " + r.getKey() + " (Lvl " + reagentLvl + ") | Qty: " + r.getValue() + " | Fee: " + totalReagentFee);
                makerEntry.addCost(totalReagentFee);
            }
        }

        makerEntry.trimCost();  // "commit" the real cost of the recipe.
        System.out.println("[MakerItemFactoryDebug] Final Trimmed Cost: " + makerEntry.getCost());

        return makerEntry;
    }

    public static MakerItemCreateEntry generateLeftoverCrystalEntry(int fromLeftoverid, int crystalId) {
        System.out.println("[MakerItemFactoryDebug] generateLeftoverCrystalEntry - From: " + fromLeftoverid + " -> To Crystal: " + crystalId);
        MakerItemCreateEntry ret = new MakerItemCreateEntry(0, 0, 1);
        ret.addReqItem(fromLeftoverid, 100);
        ret.addGainItem(crystalId, 1);
        return ret;
    }

    public static MakerItemCreateEntry generateDisassemblyCrystalEntry(int fromEquipid, int cost, List<Pair<Integer, Integer>> gains) {     // equipment at specific position already taken
        System.out.println("[MakerItemFactoryDebug] generateDisassemblyCrystalEntry - FromEquip: " + fromEquipid + ", Cost: " + cost);
        MakerItemCreateEntry ret = new MakerItemCreateEntry(cost, 0, 1);
        ret.addReqItem(fromEquipid, 1);
        for (Pair<Integer, Integer> p : gains) {
            System.out.println("[MakerItemFactoryDebug] Disassembly Gain: " + p.getLeft() + " x" + p.getRight());
            ret.addGainItem(p.getLeft(), p.getRight());
        }
        return ret;
    }

    private static double getMakerStimulantFee(int itemid) {
        System.out.println("[MakerItemFactoryDebug] Calculating Stimulant Fee for ItemID: " + itemid);

        if (YamlConfig.config.server.USE_MAKER_FEE_HEURISTICS) {
            EquipType et = EquipType.getEquipTypeById(itemid);
            int eqpLevel = ii.getEquipLevelReq(itemid);
            double fee;

            switch (et) {
                case CAP:
                    fee = 1145.736246 * Math.exp(0.03336832546 * eqpLevel);
                    break;
                case LONGCOAT:
                    fee = 2117.469118 * Math.exp(0.03355349137 * eqpLevel);
                    break;
                case SHOES:
                    fee = 1218.624674 * Math.exp(0.0342266043 * eqpLevel);
                    break;
                case GLOVES:
                    fee = 2129.531152 * Math.exp(0.03421778102 * eqpLevel);
                    break;
                case COAT:
                    fee = 1770.630579 * Math.exp(0.03359768677 * eqpLevel);
                    break;
                case PANTS:
                    fee = 1442.98837 * Math.exp(0.03444783295 * eqpLevel);
                    break;
                case SHIELD:
                    fee = 6312.40136 * Math.exp(0.0237929527 * eqpLevel);
                    break;
                default:    // weapons
                    fee = 4313.581428 * Math.exp(0.03147837094 * eqpLevel);
                    break;
            }
            System.out.println("[MakerItemFactoryDebug] Heuristic Stimulant Result: " + fee + " (EqpLevel: " + eqpLevel + ", Type: " + et + ")");
            return fee;
        } else {
            System.out.println("[MakerItemFactoryDebug] Static Stimulant Fee used: 14000");
            return 14000;
        }
    }

    private static double getMakerReagentFee(int itemid, int reagentLevel) {
        if (YamlConfig.config.server.USE_MAKER_FEE_HEURISTICS) {
            EquipType et = EquipType.getEquipTypeById(itemid);
            int eqpLevel = ii.getEquipLevelReq(itemid);
            double fee;

            switch (et) {
                case CAP:
                    fee = 5592.01613 * Math.exp(0.02914576018 * eqpLevel) * reagentLevel;
                    break;
                case LONGCOAT:
                    fee = 3405.23441 * Math.exp(0.03413001038 * eqpLevel) * reagentLevel;
                    break;
                case SHOES:
                    fee = 2115.697484 * Math.exp(0.0354881705 * eqpLevel) * reagentLevel;
                    break;
                case GLOVES:
                    fee = 4684.040894 * Math.exp(0.03166500585 * eqpLevel) * reagentLevel;
                    break;
                case COAT:
                    fee = 2955.89017 * Math.exp(0.0339948456 * eqpLevel) * reagentLevel;
                    break;
                case PANTS:
                    fee = 1774.722181 * Math.exp(0.03854321409 * eqpLevel) * reagentLevel;
                    break;
                case SHIELD:
                    fee = 12014.11296 * Math.exp(0.02185471162 * eqpLevel) * reagentLevel;
                    break;
                default:    // weapons
                    fee = 4538.650247 * Math.exp(0.0371980303 * eqpLevel) * reagentLevel;
                    break;
            }
            return fee;
        } else {
            return 8000 * reagentLevel;
        }
    }

    public static class MakerItemCreateEntry {
        private final int reqLevel;
        private final int reqMakerLevel;
        private double cost;
        private int reqCost;
        private final List<Pair<Integer, Integer>> reqItems = new ArrayList<>(); // itemId / amount
        private final List<Pair<Integer, Integer>> gainItems = new ArrayList<>(); // itemId / amount

        public MakerItemCreateEntry(int cost, int reqLevel, int reqMakerLevel) {
            this.cost = cost;
            this.reqLevel = reqLevel;
            this.reqMakerLevel = reqMakerLevel;
        }

        public MakerItemCreateEntry(MakerItemCreateEntry mi) {
            this.cost = mi.cost;
            this.reqLevel = mi.reqLevel;
            this.reqMakerLevel = mi.reqMakerLevel;

            reqItems.addAll(mi.reqItems);

            gainItems.addAll(mi.gainItems);
        }

        public List<Pair<Integer, Integer>> getReqItems() {
            return reqItems;
        }

        public List<Pair<Integer, Integer>> getGainItems() {
            return gainItems;
        }

        public int getReqLevel() {
            return reqLevel;
        }

        public int getReqSkillLevel() {
            return reqMakerLevel;
        }

        public int getCost() {
            return reqCost;
        }

        public void addCost(double amount) {
            cost += amount;
        }

        protected void addReqItem(int itemId, int amount) {
            reqItems.add(new Pair<>(itemId, amount));
        }

        protected void addGainItem(int itemId, int amount) {
            gainItems.add(new Pair<>(itemId, amount));
        }

        public void trimCost() {
            reqCost = (int) (cost / 1000);
            reqCost *= 1000;
        }

        public boolean isInvalid() {    // thanks Rohenn, Wh1SK3Y for noticing some items not getting checked properly
            return reqLevel < 0;
        }
    }
}