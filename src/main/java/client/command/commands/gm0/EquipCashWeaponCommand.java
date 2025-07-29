/*
    This file is part of the HeavenMS MapleStory Server, commands OdinMS-based
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

package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import client.inventory.Item;
import client.inventory.InventoryType;
import client.inventory.manipulator.InventoryManipulator;
import server.ItemInformationProvider;

public class EquipCashWeaponCommand extends Command {
    {
        setDescription("Equip a cash weapon from your equipment inventory. Usage: @equipcashweapon [itemId]");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        short cashWeaponSlot = (short) (-11 - 100); // -111
        
        Item totemItem = null;
        
        // Check if item ID was provided
        if (params.length > 0) {
            int itemId;
            try {
                itemId = Integer.parseInt(params[0]);
            } catch (NumberFormatException e) {
                player.yellowMessage("Invalid item ID. Please provide a valid number.");
                return;
            }
            
            // Check if player has the specific item in equipment inventory
            totemItem = player.getInventory(InventoryType.EQUIP).findById(itemId);
            if (totemItem == null) {
                player.yellowMessage("You don't have item " + itemId + " in your equipment inventory!");
                return;
            }
        } else {
            // No ID provided, find first cash weapon in equipment inventory
            for (short i = 1; i <= player.getInventory(InventoryType.EQUIP).getSlotLimit(); i++) {
                Item item = player.getInventory(InventoryType.EQUIP).getItem(i);
                if (item != null && item.getItemId() >= 1700000 && item.getItemId() <= 1799999) {
                    totemItem = item;
                    break;
                }
            }
            
            if (totemItem == null) {
                player.yellowMessage("You don't have any cash weapons in your equipment inventory!");
                return;
            }
        }
        
        // Get the item's position in inventory
        short sourceSlot = totemItem.getPosition();
        
        // Check if there's already something in the cash weapon slot
        Item currentItem = player.getInventory(InventoryType.EQUIPPED).getItem(cashWeaponSlot);
        if (currentItem != null) {
            // Check if we have space to unequip the current item
            if (player.getInventory(InventoryType.EQUIP).isFull()) {
                player.yellowMessage("Your equipment inventory is full! Please make space first.");
                return;
            }
            // Unequip the current item first
            InventoryManipulator.unequip(c, cashWeaponSlot, player.getInventory(InventoryType.EQUIP).getNextFreeSlot());
        }
        
        // Equip the totem to the cash weapon slot
        InventoryManipulator.equip(c, sourceSlot, cashWeaponSlot);
        
        // Update character appearance
        player.equipChanged();
        
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        player.yellowMessage("Successfully equipped " + ii.getName(totemItem.getItemId()) + " to your cash weapon slot!");
    }
}