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
package client.creator;

import client.Character;
import client.Client;
import client.SkinColor;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Item;
import config.YamlConfig;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ItemInformationProvider;
import tools.PacketCreator;

import java.util.function.Consumer;

/**
 * @author RonanLana
 */
public abstract class CharacterFactory {
    private static final Logger log = LoggerFactory.getLogger(CharacterFactory.class);




    protected synchronized static int createNewCharacter(Client c, String name, int face, int hair, int skin, int gender, CharacterFactoryRecipe recipe) {
        if (YamlConfig.config.server.COLLECTIVE_CHARSLOT ? c.getAvailableCharacterSlots() <= 0 : c.getAvailableCharacterWorldSlots() <= 0) {
            return -3;
        }

        if (!Character.canCreateChar(name)) {
            return -1;
        }

        Character newchar = Character.getDefault(c);
        newchar.setWorld(c.getWorld());
        newchar.setSkinColor(SkinColor.getById(skin));
        newchar.setGender(gender);
        newchar.setName(name);
        newchar.setHair(hair);
        newchar.setFace(face);

        newchar.setLevel(recipe.getLevel());
        newchar.setJob(recipe.getJob());
        newchar.setMapId(recipe.getMap());


        Inventory equipped = newchar.getInventory(InventoryType.EQUIPPED);
        ItemInformationProvider ii = ItemInformationProvider.getInstance();

        int top = recipe.getTop(), bottom = recipe.getBottom(), shoes = recipe.getShoes(), weapon = recipe.getWeapon();

        if (top > 0) {
            Item eq_top = ii.getEquipById(top);
            eq_top.setPosition((byte) -5);
            equipped.addItemFromDB(eq_top);

            Item m_medal = ii.getEquipById(1113232);
            m_medal.setPosition((byte) -12);
            equipped.addItemFromDB(m_medal);


            Item p_pet3 = ii.getEquipById(1812000);
            p_pet3.setPosition((byte) -23);
            equipped.addItemFromDB(p_pet3);

            Item p_pet4 = ii.getEquipById(1812002);
            p_pet4.setPosition((byte) -24);
            equipped.addItemFromDB(p_pet4);

            Item p_pet5 = ii.getEquipById(1812003);
            p_pet5.setPosition((byte) -25);
            equipped.addItemFromDB(p_pet5);

            Item p_pet6 = ii.getEquipById(1812004);
            p_pet6.setPosition((byte) -26);
            equipped.addItemFromDB(p_pet6);

            Item p_pet7 = ii.getEquipById(1812001);
            p_pet7.setPosition((byte) -28);
            equipped.addItemFromDB(p_pet7);

            Item p_pet8 = ii.getEquipById(1812005);
            p_pet8.setPosition((byte) -27);
            equipped.addItemFromDB(p_pet8);
        }

        if (bottom > 0) {
            Item eq_bottom = ii.getEquipById(bottom);
            eq_bottom.setPosition((byte) -6);
            equipped.addItemFromDB(eq_bottom);
        }

        if (shoes > 0) {
            Item eq_shoes = ii.getEquipById(shoes);
            eq_shoes.setPosition((byte) -7);
            equipped.addItemFromDB(eq_shoes);
        }

        if (weapon > 0) {
            Item eq_weapon = ii.getEquipById(weapon);
            eq_weapon.setPosition((byte) -11);
            equipped.addItemFromDB(eq_weapon.copy());
        }

        if (!newchar.insertNewChar(recipe)) {
            return -2;
        }

        c.sendPacket(PacketCreator.addNewCharEntry(newchar));

        Server.getInstance().createCharacterEntry(newchar);
        Server.getInstance().broadcastGMMessage(c.getWorld(), PacketCreator.sendYellowTip("[New Char]: " + c.getAccountName() + " has created a new character with IGN " + name));
        log.info("Account {} created chr with name {}", c.getAccountName(), name);

        return 0;
    }
}
