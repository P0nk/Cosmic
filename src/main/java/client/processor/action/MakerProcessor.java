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

import client.Client;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.MakerService;
import client.inventory.manipulator.InventoryManipulator;
import constants.game.GameConstants;
import constants.inventory.ItemConstants;
import net.packet.InPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ItemInformationProvider;
import server.MakerItemFactory;
import server.MakerItemFactory.MakerItemCreateEntry;
import tools.PacketCreator;
import tools.Pair;
import tools.Randomizer;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Ronan
 */
public class MakerProcessor {
    private static final ItemInformationProvider ii = ItemInformationProvider.getInstance();

    public static void makerAction(InPacket p, Client c) {
        if (c.tryacquireClient()) {
            try {
                int type = p.readInt();
                int toCreate = p.readInt();
                int toDisassemble = -1, pos = -1;
                boolean makerSucceeded = true;

                MakerItemCreateEntry recipe;
                Map<Integer, Short> reagentids = new LinkedHashMap<>();
                int stimulantid = -1;

                if (type == 3) {    // building monster crystal
                    int fromLeftover = toCreate;
                    toCreate = ii.getMakerCrystalFromLeftover(toCreate);
                    if (toCreate == -1) {
                        c.sendPacket(PacketCreator.serverNotice(1, ii.getName(fromLeftover) + " is unavailable for Monster Crystal conversion."));
                        c.sendPacket(PacketCreator.makerEnableActions());
                        return;
                    }

                    recipe = MakerItemFactory.generateLeftoverCrystalEntry(fromLeftover, toCreate);
                } else if (type == 4) {  // disassembling
                    p.readInt(); // 1... probably inventory type
                    pos = p.readInt();

                    Item it = c.getPlayer().getInventory(InventoryType.EQUIP).getItem((short) pos);
                    if (it != null && it.getItemId() == toCreate) {
                        toDisassemble = toCreate;

                        Pair<Integer, List<Pair<Integer, Integer>>> pair = MakerService.generateDisassemblyInfo(toDisassemble);
                        if (pair != null) {
                            recipe = MakerItemFactory.generateDisassemblyCrystalEntry(toDisassemble, pair.getLeft(), pair.getRight());
                        } else {
                            c.sendPacket(PacketCreator.serverNotice(1, ii.getName(toCreate) + " is unavailable for Monster Crystal disassembly."));
                            c.sendPacket(PacketCreator.makerEnableActions());
                            return;
                        }
                    } else {
                        c.sendPacket(PacketCreator.serverNotice(1, "An unknown error occurred when trying to apply that item for disassembly."));
                        c.sendPacket(PacketCreator.makerEnableActions());
                        return;
                    }
                } else {
                    if (ItemConstants.isEquipment(toCreate)) {   // only equips uses stimulant and reagents
                        if (p.readByte() != 0) {  // stimulant
                            stimulantid = ii.getMakerStimulant(toCreate);
                            if (!c.getAbstractPlayerInteraction().haveItem(stimulantid)) {
                                stimulantid = -1;
                            }
                        }

                        int reagents = Math.min(p.readInt(), MakerService.getMakerReagentSlots(toCreate));
                        for (int i = 0; i < reagents; i++) {  // crystals
                            int reagentid = p.readInt();
                            if (ItemConstants.isMakerReagent(reagentid)) {
                                Short rs = reagentids.get(reagentid);
                                if (rs == null) {
                                    reagentids.put(reagentid, (short) 1);
                                } else {
                                    reagentids.put(reagentid, (short) (rs + 1));
                                }
                            }
                        }

                        List<Pair<Integer, Short>> toUpdate = new LinkedList<>();
                        for (Map.Entry<Integer, Short> r : reagentids.entrySet()) {
                            int qty = c.getAbstractPlayerInteraction().getItemQuantity(r.getKey());

                            if (qty < r.getValue()) {
                                toUpdate.add(new Pair<>(r.getKey(), (short) qty));
                            }
                        }

                        // remove those not present on player inventory
                        if (!toUpdate.isEmpty()) {
                            for (Pair<Integer, Short> rp : toUpdate) {
                                if (rp.getRight() > 0) {
                                    reagentids.put(rp.getLeft(), rp.getRight());
                                } else {
                                    reagentids.remove(rp.getLeft());
                                }
                            }
                        }

                        if (!reagentids.isEmpty()) {
                            if (!MakerService.removeOddMakerReagents(toCreate, reagentids)) {
                                c.sendPacket(PacketCreator.serverNotice(1, "You can only use WATK and MATK Strengthening Gems on weapon items."));
                                c.sendPacket(PacketCreator.makerEnableActions());
                                return;
                            }
                        }
                    }

                    recipe = MakerItemFactory.getItemCreateEntry(toCreate, stimulantid, reagentids);
                }

                short createStatus = MakerService.getCreateStatus(c, recipe);

                MakerService.applyMakerResult(c, createStatus, toCreate, recipe, toDisassemble, (short) pos, stimulantid, reagentids, makerSucceeded, type);
            } finally {
                c.releaseClient();
            }
        }
    }
}
