package client.command.commands.gm6;

import client.Character;
import client.Client;
import client.command.Command;
import client.inventory.manipulator.InventoryManipulator;
import config.YamlConfig;
import constants.inventory.ItemConstants;
import server.ItemInformationProvider;
import server.loot.EquipWeaponDropper;

public class GiveEquipWeaponsCommand extends Command {
    {
        setDescription("Gives weapon equips from EquipWeaponDropper by job group and level band, paged by 96. Syntax: !giveequipweapons <job> <level> [page]");
    }

    private static final int PAGE_SIZE = 96;

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        if (params.length < 2) {
            player.yellowMessage("Syntax: !giveequipweapons <job> <level> [page]");
            player.yellowMessage("Example: !giveequipweapons warrior 100 1");
            player.yellowMessage("Jobs: beginner, warrior, mage, archer, thief, pirate, all");
            return;
        }

        EquipWeaponDropper.JobGroup group = parseJobGroup(params[0]);
        if (group == null) {
            player.yellowMessage("Invalid job '" + params[0] + "'. Use: beginner, warrior, mage, archer, thief, pirate, all");
            return;
        }

        int level;
        try {
            level = Integer.parseInt(params[1]);
        } catch (NumberFormatException e) {
            player.yellowMessage("Level must be a number. Example: !giveequipweapons warrior 100 1");
            return;
        }

        int page = 1;
        if (params.length >= 3) {
            try {
                page = Integer.parseInt(params[2]);
            } catch (NumberFormatException e) {
                player.yellowMessage("Page must be a number. Example: !giveequipweapons warrior 100 2");
                return;
            }
        }
        if (page < 1) page = 1;

        int band = EquipWeaponDropper.levelToBand(level);
        int[] itemIds = EquipWeaponDropper.getItemsFor(group, level);

        if (itemIds == null || itemIds.length == 0) {
            player.yellowMessage("No items found for " + group + " band " + band + ".");
            return;
        }

        int startIdx = (page - 1) * PAGE_SIZE;     // 0-based
        int endIdxExclusive = Math.min(startIdx + PAGE_SIZE, itemIds.length);

        if (startIdx >= itemIds.length) {
            int maxPage = (itemIds.length + PAGE_SIZE - 1) / PAGE_SIZE;
            player.yellowMessage("Page " + page + " is out of range. This category has " + itemIds.length + " items (" + maxPage + " page(s)).");
            return;
        }

        ItemInformationProvider ii = ItemInformationProvider.getInstance();

        // match !item behavior for non-GM3+ (optional, keeps consistency)
        short flag = 0;
        if (player.gmLevel() < 3) {
            flag |= ItemConstants.ACCOUNT_SHARING;
            flag |= ItemConstants.UNTRADEABLE;
        }

        int given = 0;
        int skipped = 0;

        for (int i = startIdx; i < endIdxExclusive; i++) {
            int itemId = itemIds[i];

            // basic safety checks
            if (ii.getName(itemId) == null) {
                skipped++;
                continue;
            }

            if (YamlConfig.config.server.BLOCK_GENERATE_CASH_ITEM && ii.isCash(itemId)) {
                skipped++;
                continue;
            }

            try {
                InventoryManipulator.addById(c, itemId, (short) 1, player.getName(), -1, flag, -1);
                given++;
            } catch (Exception ex) {
                skipped++; // Only count skipped if it actually failed to add
            }
        }

        int displayFrom = startIdx + 1; // 1-based for user message
        int displayTo = endIdxExclusive;

        player.yellowMessage("GiveEquipWeapons: " + group + " band " + band +
                " | page " + page + " | items " + displayFrom + "-" + displayTo +
                " of " + itemIds.length + " => given " + given + ", skipped " + skipped + ".");
    }

    private EquipWeaponDropper.JobGroup parseJobGroup(String s) {
        if (s == null) return null;
        String k = s.trim().toUpperCase();

        switch (k) {
            case "BEGINNER":
            case "BEG":
                return EquipWeaponDropper.JobGroup.BEGINNER;

            case "WARRIOR":
            case "WAR":
                return EquipWeaponDropper.JobGroup.WARRIOR;

            case "MAGE":
            case "MAG":
                return EquipWeaponDropper.JobGroup.MAGE;

            case "ARCHER":
            case "ARC":
            case "BOWMAN":
                return EquipWeaponDropper.JobGroup.ARCHER;

            case "THIEF":
            case "THF":
                return EquipWeaponDropper.JobGroup.THIEF;

            case "PIRATE":
            case "PIR":
                return EquipWeaponDropper.JobGroup.PIRATE;

            case "ALL":
                return EquipWeaponDropper.JobGroup.ALL;

            default:
                return null;
        }
    }
}
