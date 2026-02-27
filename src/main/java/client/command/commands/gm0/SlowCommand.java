package client.command.commands.gm0;

import client.BuffStat;
import client.Character;
import client.Client;
import client.command.Command;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.ModifyInventory;
import tools.PacketCreator;

import java.util.ArrayList;
import java.util.List;

public class SlowCommand extends Command {
    {
        setDescription("Toggles off equipment and buff speed/jump bonuses for navigating certain terrains.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        boolean newSlowMode = !player.isSlowMode();
        player.setSlowMode(newSlowMode);

        if (newSlowMode) {
            player.cancelEffectFromBuffStat(BuffStat.SPEED);
            player.cancelEffectFromBuffStat(BuffStat.JUMP);
            player.cancelEffectFromBuffStat(BuffStat.DASH);
            player.cancelEffectFromBuffStat(BuffStat.DASH2);
            player.cancelEffectFromBuffStat(BuffStat.MONSTER_RIDING);
            player.dropMessage(5, "Slow mode ENABLED: Equipment speed/jump bonuses are now hidden.");
        } else {
            player.dropMessage(5, "Slow mode DISABLED: Equipment speed/jump bonuses restored.");
        }

        // Refresh equipped inventory visually so the client sees the updated speed
        // values
        List<ModifyInventory> mods = new ArrayList<>();
        for (Item item : player.getInventory(InventoryType.EQUIPPED).list()) {
            mods.add(new ModifyInventory(3, item));
        }
        c.sendPacket(PacketCreator.modifyInventory(false, mods, player));
    }
}
