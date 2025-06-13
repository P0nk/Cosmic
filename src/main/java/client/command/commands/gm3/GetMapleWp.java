package client.command.commands.gm3;

import client.Character;
import client.Client;
import client.command.Command;
import client.inventory.Pet;
import client.inventory.manipulator.InventoryManipulator;
import config.YamlConfig;
import constants.inventory.ItemConstants;
import server.ItemInformationProvider;

import static java.util.concurrent.TimeUnit.DAYS;

public class GetMapleWp extends Command {
    {
        setDescription("Spawn all lvl 64 weapon.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

//        if (params.length < 1) {
//            player.yellowMessage("Syntax: !getmaplewp");
//            return;
//        }

        int[] mapleWeaponIds = {
                1302030, 1312031, 1322052, 1332025, 1332056, 1372010,
                1382035, 1402035, 1412021, 1422027, 1432012, 1442044,
                1452016, 1462014, 1472032, 1482023, 1492023
        };

        int quantity = 1; // Or whatever quantity you want to give
        byte flag = (byte) 0; // Replace with appropriate flag value if needed

        for (int itemId : mapleWeaponIds) {
            InventoryManipulator.addById(c, itemId, (short) quantity, player.getName(), -1, flag, -1);
        }
    }
}
