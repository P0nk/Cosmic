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
*/
package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import config.YamlConfig;
import server.events.FeverScheduler;

public class RatesCommand extends Command {
    {
        setDescription("Show your rates.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        boolean isSubscribed = server.subscription.SubscriptionManager.isSubscribed(player.getId());
        String subMsg = isSubscribed ? " #r(1.5x Sub Bonus!)#k" : "";

        FeverScheduler fever = FeverScheduler.getInstance();
        boolean feverActive = fever.isFeverActive();
        String feverNote = " #r(Fever Active!)#k";

        String showMsg_ = "#eCHARACTER RATES#n" + "\r\n\r\n";
        showMsg_ += "EXP Rate: #e#b" + (isSubscribed ? (player.getExpRate() * 1.5) : player.getExpRate()) + "x#k#n"
                + subMsg
                + (feverActive && fever.getCurrentFever() == FeverScheduler.FeverType.EXP ? feverNote : "")
                + (player.hasNoviceExpRate() ? " - novice rate" : "") + "\r\n";
        showMsg_ += "MESO Rate: #e#b" + (isSubscribed ? (player.getMesoRate() * 1.5) : player.getMesoRate()) + "x#k#n"
                + subMsg
                + (feverActive && fever.getCurrentFever() == FeverScheduler.FeverType.MESO ? feverNote : "") + "\r\n";
        showMsg_ += "DROP Rate: #e#b" + player.getDropRate() + "x#k#n"
                + (feverActive && fever.getCurrentFever() == FeverScheduler.FeverType.DROP ? feverNote : "") + "\r\n";
        showMsg_ += "BOSS DROP Rate: #e#b" + player.getBossDropRate() + "x#k#n" + "\r\n";
        if (YamlConfig.config.server.USE_QUEST_RATE) {
            showMsg_ += "QUEST Rate: #e#b" + c.getWorldServer().getQuestRate() + "x#k#n" + "\r\n";
        }

        player.showHint(showMsg_, 300);
    }
}
