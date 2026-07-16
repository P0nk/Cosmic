package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import client.command.ForgeEconomyConstants;
import client.inventory.manipulator.InventoryManipulator;
import tools.PacketCreator;

public class GlCommand extends Command {

    public GlCommand() {
        setDescription(
                "Convert 2 billion mesos into one Golden Maple Leaf."
        );
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        int leafId =
                ForgeEconomyConstants.GOLDEN_MAPLE_LEAF_ID;

        int exchangeCost =
                ForgeEconomyConstants.MESOS_PER_GOLDEN_MAPLE_LEAF;

        /*
         * Confirm the player has enough mesos.
         */
        if (player.getMeso() < exchangeCost) {
            player.dropMessage(
                    5,
                    "You need "
                            + String.format("%,d", exchangeCost)
                            + " mesos to receive one Golden Maple Leaf."
            );

            player.dropMessage(
                    5,
                    "Current mesos: "
                            + String.format("%,d", player.getMeso())
            );

            return;
        }

        /*
         * Confirm that the ETC inventory can hold the leaf
         * before removing any mesos.
         */
        if (!player.canHold(leafId)) {
            player.dropMessage(
                    5,
                    "Your ETC inventory is full. Make room before using @gl."
            );
            return;
        }

        /*
         * Award the Golden Maple Leaf.
         */
        InventoryManipulator.addById(
                c,
                leafId,
                (short) 1,
                "",
                -1
        );

        c.sendPacket(
                PacketCreator.getShowItemGain(
                        leafId,
                        (short) 1,
                        true
                )
        );

        /*
         * Remove exactly 2 billion mesos.
         */
        player.gainMeso(-exchangeCost, true);

        player.dropMessage(
                5,
                "Converted "
                        + String.format("%,d", exchangeCost)
                        + " mesos into 1 Golden Maple Leaf."
        );

        player.dropMessage(
                5,
                "Remaining mesos: "
                        + String.format("%,d", player.getMeso())
        );
    }
}