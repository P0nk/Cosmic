package client.command.commands.gm4;

import client.Character; // Used in ItemCommand.java
import client.Client; // Used in ItemCommand.java
import client.command.Command; // Used in ItemCommand.java
import client.inventory.Pet; // Used in ItemCommand.java
import client.inventory.manipulator.InventoryManipulator; // Used in ItemCommand.java
import config.YamlConfig; // Used in ItemCommand.java
import constants.inventory.ItemConstants; // Used in ItemCommand.java
import server.ItemInformationProvider; // Used in ItemCommand.java

import static java.util.concurrent.TimeUnit.DAYS;

public class GiveItemCommand extends Command {
    {
        setDescription("Give an item to another player.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        if (params.length < 2) {
            player.yellowMessage("Syntax: !giveitem <player> <itemid> [quantity|days]");
            return;
        }

        String targetName = params[0];
        int itemId = Integer.parseInt(params[1]);
        ItemInformationProvider ii = ItemInformationProvider.getInstance();

        if (ii.getName(itemId) == null) {
            player.yellowMessage("Item id '" + itemId + "' does not exist.");
            return;
        }

        short quantity = 1;
        long expiration = -1;
        int petId = -1;

        if (params.length >= 3) {
            try {
                quantity = Short.parseShort(params[2]);
            } catch (NumberFormatException e) {
                quantity = 1;
            }
        }

        if (YamlConfig.config.server.BLOCK_GENERATE_CASH_ITEM && ii.isCash(itemId)) {
            player.yellowMessage("You cannot create a cash item with this command.");
            return;
        }

        Character target = c.getWorldServer().getPlayerStorage().getCharacterByName(targetName);
        if (target == null) {
            player.yellowMessage("Player '" + targetName + "' could not be found.");
            return;
        }

        if (ItemConstants.isPet(itemId)) {
            if (params.length >= 3) {
                quantity = 1;
                long days = Math.max(1, Integer.parseInt(params[2]));
                expiration = System.currentTimeMillis() + DAYS.toMillis(days);
                petId = Pet.createPet(itemId);
                InventoryManipulator.addById(target.getClient(), itemId, quantity, player.getName(), petId, expiration);
                return;
            } else {
                player.yellowMessage("Pet Syntax: !giveitem <player> <itemid> <days>");
                return;
            }
        }

        short flag = 0;
        if (player.gmLevel() < 3) {
            flag |= ItemConstants.ACCOUNT_SHARING;
            flag |= ItemConstants.UNTRADEABLE;
        }

        InventoryManipulator.addById(target.getClient(), itemId, quantity, player.getName(), petId, flag, expiration);
        player.message("Gave item " + itemId + " x" + quantity + " to " + target.getName() + ".");
        target.message("You received item " + itemId + " x" + quantity + " from " + player.getName() + ".");
    }
}
