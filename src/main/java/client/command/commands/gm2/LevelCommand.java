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

/*
   @Author: Arthur L - Refactored command content into modules
   @Modified: To support target player selection (Safety: Level First)
*/
package client.command.commands.gm2;

import client.Character;
import client.Client;
import client.command.Command;
import config.YamlConfig;

public class LevelCommand extends Command {
    {
        setDescription("Set a level. Usage: !level <level> [ign]");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        Character target = player; // Default target is self
        int targetLevel = 0;

        if (params.length < 1) {
            player.yellowMessage("Syntax: !level <level> [ign]");
            return;
        }

        // Step 1: Parse the Level (First Parameter)
        try {
            targetLevel = Integer.parseInt(params[0]);
        } catch (NumberFormatException e) {
            player.yellowMessage("Invalid Level. Please enter a number first. Syntax: !level <level> [ign]");
            return;
        }

        // Step 2: Check for optional target name (Second Parameter)
        if (params.length > 1) {
            String targetName = params[1];
            target = c.getChannelServer().getPlayerStorage().getCharacterByName(targetName);

            if (target == null) {
                player.yellowMessage("Player '" + targetName + "' could not be found in this channel.");
                return;
            }
        }

        // Step 3: Execute Leveling Logic on Target
        // We strip the EXP first so they start fresh at the new level
        target.loseExp(target.getExp(), false, false);

        // Set level to (Target - 1) because levelUp() is called immediately after to refresh stats
        target.setLevel(Math.min(targetLevel, target.getMaxClassLevel()) - 1);

        target.resetPlayerRates();
        if (YamlConfig.config.server.USE_ADD_RATES_BY_LEVEL) {
            target.setPlayerRates();
        }
        target.setWorldRates();

        // This triggers the level up effect and recalculates stats
        target.levelUp(false);

        // Confirmation
        if (player != target) {
            player.yellowMessage("Set " + target.getName() + "'s level to " + target.getLevel());
        } else {
            player.yellowMessage("Set your level to " + target.getLevel());
        }
    }
}