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
import client.SkillFactory;
import client.inventory.InventoryType;
import client.inventory.Pet;
import client.inventory.manipulator.InventoryManipulator;
import constants.id.ItemId;
import provider.DataProvider;
import provider.DataProviderFactory;
import provider.DataTool;
import provider.wz.WZFiles;
import tools.PacketCreator;

import java.awt.*;

/**
 * @author RonanLana - just added locking on OdinMS' SpawnPetHandler method body
 */
public class SpawnPetProcessor {
    private static final DataProvider dataRoot = DataProviderFactory.getDataProvider(WZFiles.ITEM);

    public static void processSpawnPet(Client c, byte slot, boolean lead) {
        if (c.tryacquireClient()) {
            try {
                Character chr = c.getPlayer();
                Pet pet = chr.getInventory(InventoryType.CASH).getItem(slot).getPet();
                if (pet == null) {
                    return;
                }

                int petid = pet.getItemId();
                if (petid == ItemId.DRAGON_PET || petid == ItemId.ROBO_PET) {
                    if (chr.haveItem(petid + 1)) {
                        chr.dropMessage(5, "You can't hatch your " + (petid == ItemId.DRAGON_PET ? "Dragon egg" : "Robo egg") + " if you already have a Baby " + (petid == ItemId.DRAGON_PET ? "Dragon." : "Robo."));
                        c.sendPacket(PacketCreator.enableActions());
                        return;
                    } else {
                        int evolveid = DataTool.getInt("info/evol1", dataRoot.getData("Pet/" + petid + ".img"));
                        int petId = Pet.createPet(evolveid);
                        if (petId == -1) {
                            return;
                        }
                        long expiration = chr.getInventory(InventoryType.CASH).getItem(slot).getExpiration();
                        InventoryManipulator.removeById(c, InventoryType.CASH, petid, (short) 1, false, false);
                        InventoryManipulator.addById(c.getPlayer(), evolveid, (short) 1, null, petId, expiration);

                        c.sendPacket(PacketCreator.enableActions());
                        return;
                    }
                }
                if (chr.getPetIndex(pet) != -1) {
                    chr.unequipPet(pet, true);
                } else {
                    if (chr.getSkillLevel(SkillFactory.getSkill(8)) == 0 && chr.getPet(0) != null) {
                        chr.unequipPet(chr.getPet(0), false);
                    }
                    if (lead) {
                        chr.shiftPetsRight();
                    }
                    Point pos = chr.getPosition();
                    pos.y -= 12;
                    pet.setPos(pos);
                    pet.setFh(chr.getMap().getFootholds().findBelow(pet.getPos()).getId());
                    pet.setStance(0);
                    pet.setSummoned(true);
                    pet.saveToDb();
                    chr.addPet(pet);
                    chr.getMap().broadcastMessage(c.getPlayer(), PacketCreator.showPet(c.getPlayer(), pet, false, false), true);
                    c.sendPacket(PacketCreator.petStatUpdate(c.getPlayer()));
                    c.sendPacket(PacketCreator.enableActions());

                    chr.commitExcludedItems();
                    chr.getClient().getWorldServer().registerPetHunger(chr, chr.getPetIndex(pet));
                }
            } finally {
                c.releaseClient();
            }
        }
    }
}
