package client.command.commands.gm4;

import client.Character;
import client.Client;
import client.command.Command;
import client.inventory.manipulator.InventoryManipulator;
import config.YamlConfig;

public class ExchangeCommand extends Command {
    {
        setDescription("Exchange 100 rebirths and Max Stats for a Max Stat Token.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        int maxAp = YamlConfig.config.server.MAX_AP;

        if (player.getReborns() < 100) {
            player.dropMessage(5, "You need at least 100 rebirths to exchange for a Max Stat Token.");
            return;
        }

        if (player.getStr() < maxAp || player.getDex() < maxAp || player.getInt() < maxAp || player.getLuk() < maxAp) {
            player.dropMessage(5, "You need all 4 main stats (STR, DEX, INT, LUK) to be at exactly Max AP (" + maxAp
                    + ") to exchange.");
            return;
        }

        if (!InventoryManipulator.checkSpace(c, 4001126, 1, "")) {
            player.dropMessage(5, "You do not have enough space in your ETC inventory to receive the Max Stat Token.");
            return;
        }

        // Deduct Rebirths
        player.setReborns(player.getReborns() - 100);

        // Reset Stats
        player.updateStrDexIntLuk(4);

        // Grant Item (Maple Leaf token for now)
        InventoryManipulator.addById(c, 4001126, (short) 1, "Max Stat Exchange", -1, -1);

        player.dropMessage(6,
                "Congratulations! You have exchanged 100 rebirths and your Max Stats for a Max Stat Token.");
        c.getWorldServer().broadcastPacket(tools.PacketCreator.serverNotice(6, "[Max Stat System] The mighty "
                + player.getName() + " has reached the pinnacle of strength and reborn down to basics once more!"));
    }
}
