package client.command.commands.gm0;

import client.BuffStat;
import client.Character;
import client.Client;
import client.command.Command;

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

        // Recalculate stats and push updated stats packet safely (no inventory packet
        // needed)
        player.recalcLocalStats();
    }
}
