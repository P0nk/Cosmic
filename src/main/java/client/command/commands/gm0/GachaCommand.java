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

import client.Client;
import client.command.Command;
import client.command.CommandContext;
import constants.id.NpcId;
import server.ItemInformationProvider;
import server.gachapon.Gachapon;

public class GachaCommand extends Command {
    {
        setDescription("Show gachapon rewards.");
    }

    @Override
    public void execute(Client c, String[] params, CommandContext ctx) {
        Gachapon.GachaponType gacha = null;
        String search = c.getPlayer().getLastCommandMessage();
        String gachaName = "";
        String[] names = {"Henesys", "Ellinia", "Perion", "Kerning City", "Sleepywood", "Mushroom Shrine", "Showa Spa Male", "Showa Spa Female", "New Leaf City", "Nautilus Harbor"};
        int[] ids = {NpcId.GACHAPON_HENESYS, NpcId.GACHAPON_ELLINIA, NpcId.GACHAPON_PERION, NpcId.GACHAPON_KERNING,
                NpcId.GACHAPON_SLEEPYWOOD, NpcId.GACHAPON_MUSHROOM_SHRINE, NpcId.GACHAPON_SHOWA_MALE,
                NpcId.GACHAPON_SHOWA_FEMALE, NpcId.GACHAPON_NLC, NpcId.GACHAPON_NAUTILUS};
        for (int i = 0; i < names.length; i++) {
            if (search.equalsIgnoreCase(names[i])) {
                gachaName = names[i];
                gacha = Gachapon.GachaponType.getByNpcId(ids[i]);
            }
        }
        if (gacha == null) {
            c.getPlayer().yellowMessage("Please use @gacha <name> where name corresponds to one of the below:");
            for (String name : names) {
                c.getPlayer().yellowMessage(name);
            }
            return;
        }
        String talkStr = "The #b" + gachaName + "#k Gachapon contains the following items.\r\n\r\n";
        for (int i = 0; i < 2; i++) {
            for (int id : gacha.getItems(i)) {
                talkStr += "-" + ItemInformationProvider.getInstance().getName(id) + "\r\n";
            }
        }
        talkStr += "\r\nPlease keep in mind that there are items that are in all gachapons and are not listed here.";

        c.getAbstractPlayerInteraction().npcTalk(NpcId.MAPLE_ADMINISTRATOR, talkStr);
    }
}
