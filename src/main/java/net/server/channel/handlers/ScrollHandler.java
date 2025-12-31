package net.server.channel.handlers;

import client.Character;
import client.Client;
import client.Skill;
import client.SkillFactory;
import client.inventory.Equip;
import client.inventory.Equip.ScrollResult;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.ModifyInventory;
import client.inventory.manipulator.InventoryManipulator;
import constants.id.ItemId;
import constants.inventory.ItemConstants;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import server.ItemInformationProvider;
import tools.PacketCreator;

import java.util.ArrayList;
import java.util.List;

/**
 * This handler processes the scrolls applied to the equipment.
 * It checks if the scroll is valid, applies the effects, and updates the inventory.
 */
public final class ScrollHandler extends AbstractPacketHandler {

    @Override
    public final void handlePacket(InPacket p, Client c) {
        if (c.tryacquireClient()) {
            try {
                // Reading the packet data
                p.readInt();  // Placeholder read, no usage
                short scrollSlot = p.readShort();
                System.out.println("scrollSlot: " + scrollSlot); // Print after declaring
                short equipSlot = p.readShort();
                System.out.println("equipSlot: " + equipSlot); // Print after declaring

                byte ws = (byte) p.readShort();  // White scroll status
                boolean whiteScroll = false; // white scroll being used?
                boolean legendarySpirit = false; // legendary spirit skill

                // Determine if White Scroll is being used
                if ((ws & 2) == 2) {
                    whiteScroll = true;
                }
                System.out.println("whiteScroll: " + whiteScroll); // Print after declaring

                // Initialize necessary objects
                ItemInformationProvider ii = ItemInformationProvider.getInstance();
                System.out.println("ItemInformationProvider instance: " + ii); // Print after declaring
                Character chr = c.getPlayer();
                System.out.println("Character: " + chr); // Print after declaring
                Equip toScroll = (Equip) chr.getInventory(InventoryType.EQUIPPED).getItem(equipSlot);
                System.out.println("toScroll: " + toScroll); // Print after declaring
                Skill LegendarySpirit = SkillFactory.getSkill(1003);
                System.out.println("LegendarySpirit skill: " + LegendarySpirit); // Print after declaring

                // Check if the Legendary Spirit skill is active
                if (chr.getSkillLevel(LegendarySpirit) > 0 && equipSlot >= 0) {
                    legendarySpirit = true;
                    toScroll = (Equip) chr.getInventory(InventoryType.EQUIP).getItem(equipSlot);
                    System.out.println("legendarySpirit: " + legendarySpirit); // Print after declaring
                }

                byte oldLevel = toScroll.getLevel();  // Old level of the item
                System.out.println("oldLevel: " + oldLevel); // Print after declaring
                byte oldSlots = toScroll.getUpgradeSlots();  // Old upgrade slots of the item
                System.out.println("oldSlots: " + oldSlots); // Print after declaring
                Inventory useInventory = chr.getInventory(InventoryType.USE);
                System.out.println("useInventory: " + useInventory); // Print after declaring
                Item scroll = useInventory.getItem(scrollSlot);
                System.out.println("scroll: " + scroll); // Print after declaring
                Item wscroll = null;

                // Check if Clean Slate can be used
                if (ItemConstants.isCleanSlate(scroll.getItemId()) && !ii.canUseCleanSlate(toScroll)) {
                    announceCannotScroll(c, legendarySpirit);
                    return;
                } else if (!ItemConstants.isModifierScroll(scroll.getItemId()) && toScroll.getUpgradeSlots() < 1) {
                    // Check if the scroll is valid and upgrade slots are available
                    announceCannotScroll(c, legendarySpirit);  // thanks onechord for noticing zero upgrade slots freezing Legendary Scroll UI
                    return;
                }

                // Get scroll requirements
                List<Integer> scrollReqs = ii.getScrollReqs(scroll.getItemId());
                System.out.println("scrollReqs: " + scrollReqs); // Print after declaring
                if (scrollReqs.size() > 0 && !scrollReqs.contains(toScroll.getItemId())) {
                    announceCannotScroll(c, legendarySpirit);
                    return;
                }

                // Handle white scroll logic
                if (whiteScroll) {
                    wscroll = useInventory.findById(ItemId.WHITE_SCROLL);
                    if (wscroll == null) {
                        whiteScroll = false;
                    }
                    System.out.println("wscroll: " + wscroll); // Print after declaring
                }

                // Check if the scroll can be applied to the item
                System.out.println("canScroll check: " + canScroll(scroll.getItemId(), toScroll.getItemId())); // Print before using method
                if (!ItemConstants.isChaosScroll(scroll.getItemId()) && !ItemConstants.isCleanSlate(scroll.getItemId()) && !canScroll(scroll.getItemId(), toScroll.getItemId())) {
                    announceCannotScroll(c, legendarySpirit);
                    return;
                }

                // Apply the scroll
                Equip scrolled = (Equip) ii.scrollEquipWithId(toScroll, scroll.getItemId(), whiteScroll, 0, chr.isGM());
                ScrollResult scrollSuccess = Equip.ScrollResult.FAIL;  // fail by default
                System.out.println("scrolled: " + scrolled); // Print after declaring
                if (scrolled == null) {
                    scrollSuccess = Equip.ScrollResult.CURSE;  // Curse outcome
                } else if (scrolled.getLevel() > oldLevel || (ItemConstants.isCleanSlate(scroll.getItemId()) && scrolled.getUpgradeSlots() == oldSlots + 1) || ItemConstants.isFlagModifier(scroll.getItemId(), scrolled.getFlag())) {
                    scrollSuccess = Equip.ScrollResult.SUCCESS;  // Success outcome
                }
                System.out.println("scrollSuccess: " + scrollSuccess); // Print after declaring

                // Lock the inventory to prevent concurrent changes
                useInventory.lockInventory();
                try {
                    // Check if scroll quantity is valid
                    if (scroll.getQuantity() < 1) {
                        announceCannotScroll(c, legendarySpirit);
                        return;
                    }

                    // Handle white scroll usage if necessary
                    if (whiteScroll && !ItemConstants.isCleanSlate(scroll.getItemId())) {
                        if (wscroll.getQuantity() < 1) {
                            announceCannotScroll(c, legendarySpirit);
                            return;
                        }
                        // Remove one white scroll from the inventory
                        InventoryManipulator.removeFromSlot(c, InventoryType.USE, wscroll.getPosition(), (short) 1, false, false);
                    }

                    // Remove the scroll from the inventory
                    InventoryManipulator.removeFromSlot(c, InventoryType.USE, scroll.getPosition(), (short) 1, false);
                } finally {
                    useInventory.unlockInventory();
                }

                // Prepare modifications to the inventory based on the scroll result
                final List<ModifyInventory> mods = new ArrayList<>();
                if (scrollSuccess == Equip.ScrollResult.CURSE) {
                    // Handle curse outcome (item destruction)
                    if (!ItemId.isWeddingRing(toScroll.getItemId())) {
                        mods.add(new ModifyInventory(3, toScroll));
                        if (equipSlot < 0) {
                            Inventory inv = chr.getInventory(InventoryType.EQUIPPED);

                            inv.lockInventory();
                            try {
                                chr.unequippedItem(toScroll);
                                inv.removeItem(toScroll.getPosition());
                            } finally {
                                inv.unlockInventory();
                            }
                        } else {
                            Inventory inv = chr.getInventory(InventoryType.EQUIP);

                            inv.lockInventory();
                            try {
                                inv.removeItem(toScroll.getPosition());
                            } finally {
                                inv.unlockInventory();
                            }
                        }
                    } else {
                        // Special case for wedding rings
                        scrolled = toScroll;
                        scrollSuccess = Equip.ScrollResult.FAIL;

                        mods.add(new ModifyInventory(3, scrolled));
                        mods.add(new ModifyInventory(0, scrolled));
                    }
                } else {
                    // Success case
                    mods.add(new ModifyInventory(3, scrolled));
                    mods.add(new ModifyInventory(0, scrolled));
                }

                // Send the updated inventory to the client
                c.sendPacket(PacketCreator.modifyInventory(true, mods));
                chr.getMap().broadcastMessage(PacketCreator.getScrollEffect(chr.getId(), scrollSuccess, legendarySpirit, whiteScroll));

                // If the equipment is newly equipped, update the player
                if (equipSlot < 0 && (scrollSuccess == Equip.ScrollResult.SUCCESS || scrollSuccess == Equip.ScrollResult.CURSE)) {
                    chr.equipChanged();
                }
            } finally {
                // Release the client lock
                c.releaseClient();
            }
        }
    }

    private static void announceCannotScroll(Client c, boolean legendarySpirit) {
        // Announce failure (either normal or legendary spirit failure)
        if (legendarySpirit) {
            c.sendPacket(PacketCreator.getScrollEffect(c.getPlayer().getId(), Equip.ScrollResult.FAIL, false, false));
        } else {
            c.sendPacket(PacketCreator.getInventoryFull());
        }
    }

    private static boolean canScroll(int scrollid, int itemid) {
        // Determine if the scroll can be applied to the item
        int sid = scrollid / 100;

        System.out.println("Checking if scroll " + scrollid + " can be used on item " + itemid); // Print before returning result
        switch (sid) {
            case 20492: // Scroll for accessory (pendant, belt, ring)
                return canScroll(ItemId.RING_STR_100_SCROLL, itemid) || canScroll(ItemId.DRAGON_STONE_SCROLL, itemid) ||
                        canScroll(ItemId.BELT_STR_100_SCROLL, itemid);

            default:
                return (scrollid / 100) % 100 == (itemid / 10000) % 100;
        }
    }
}
