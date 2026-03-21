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
package client.command.commands.gm2;

import client.Character;
import client.Client;
import client.command.Command;
import provider.Data;
import provider.DataProvider;
import provider.DataProviderFactory;
import provider.DataTool;
import provider.wz.WZFiles;
import scripting.npc.NPCScriptManager;
import server.ItemInformationProvider;
import server.quest.Quest;
import tools.PacketCreator;
import tools.Pair;

import java.util.ArrayList;

public class SearchCommand extends Command {
    private static Data npcStringData;
    private static Data mobStringData;
    private static Data skillStringData;
    private static Data mapStringData;

    {
        setDescription("Search String.wz.");

        DataProvider dataProvider = DataProviderFactory.getDataProvider(WZFiles.STRING);
        npcStringData = dataProvider.getData("Npc.img");
        mobStringData = dataProvider.getData("Mob.img");
        skillStringData = dataProvider.getData("Skill.img");
        mapStringData = dataProvider.getData("Map.img");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        if (params.length < 2) {
            player.yellowMessage("Syntax: !search <type> <name>");
            return;
        }
        StringBuilder sb = new StringBuilder();

        boolean item = false;
        boolean mob = false;
        boolean npc = false;
        ArrayList<Integer> searchdata = new ArrayList<>();

        String search = joinStringFrom(params, 1);
        long start = System.currentTimeMillis();//for the lulz
        Data data = null;
        if (!params[0].equalsIgnoreCase("ITEM")) {
            int searchType = 0;

            if (params[0].equalsIgnoreCase("NPC")) {
                data = npcStringData;
            } else if (params[0].equalsIgnoreCase("MOB") || params[0].equalsIgnoreCase("MONSTER")) {
                data = mobStringData;
            } else if (params[0].equalsIgnoreCase("SKILL")) {
                data = skillStringData;
            } else if (params[0].equalsIgnoreCase("MAP")) {
                data = mapStringData;
                searchType = 1;
            } else if (params[0].equalsIgnoreCase("QUEST")) {
                data = mapStringData;
                searchType = 2;
            } else {
                sb.append("#bInvalid search.\r\nSyntax: '!search [type] [name]', where [type] is MAP, QUEST, NPC, ITEM, MOB, or SKILL.");
            }
            if (data != null) {
                String name;

                if (searchType == 0) {
                    int counter = -1;
                    for (Data searchData : data.getChildren()) {
                        name = DataTool.getString(searchData.getChildByPath("name"), "NO-NAME");
                        if (name.toLowerCase().contains(search.toLowerCase())) {
                            counter++;
                            sb.append("#L").append(counter).append("##b").append(Integer.parseInt(searchData.getName())).append("#k - #r").append(name).append("#l\r\n");
                            searchdata.add(Integer.parseInt(searchData.getName()));
                            if (params[0].equalsIgnoreCase("MOB") || params[0].equalsIgnoreCase("MONSTER")) {
                                mob = true;
                            } else if (params[0].equalsIgnoreCase("NPC")) {
                                npc = true;
                            }
                        }
                    }
                } else if (searchType == 1) {
                    String mapName, streetName;

                    for (Data searchDataDir : data.getChildren()) {
                        for (Data searchData : searchDataDir.getChildren()) {
                            mapName = DataTool.getString(searchData.getChildByPath("mapName"), "NO-NAME");
                            streetName = DataTool.getString(searchData.getChildByPath("streetName"), "NO-NAME");

                            if (mapName.toLowerCase().contains(search.toLowerCase()) || streetName.toLowerCase().contains(search.toLowerCase())) {
                                sb.append("#b").append(Integer.parseInt(searchData.getName())).append("#k - #r").append(streetName).append(" - ").append(mapName).append("\r\n");
                            }
                        }
                    }
                } else {
                    for (Quest mq : Quest.getMatchedQuests(search)) {
                        sb.append("#b").append(mq.getId()).append("#k - #r");

                        String parentName = mq.getParentName();
                        if (!parentName.isEmpty()) {
                            sb.append(parentName).append(" - ");
                        }
                        sb.append(mq.getName()).append("\r\n");
                    }
                }
            }
        } else {
            int counter = -1;
            for (Pair<Integer, String> itemPair : ItemInformationProvider.getInstance().getAllItems()) {
                if (sb.length() < 32654) {//ohlol
                    if (itemPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                        counter++;
                        sb.append("#L").append(counter).append("##b").append(itemPair.getLeft()).append("#k - #r#z").append(itemPair.getLeft()).append("##l\r\n");
                        searchdata.add(itemPair.getLeft());
                        item = true;
                    }
                } else {
                    sb.append("#bCouldn't load all items, there are too many results.\r\n");
                    break;
                }
            }
        }
        if (sb.length() == 0) {
            sb.append("#bNo ").append(params[0].toLowerCase()).append("s found.\r\n");
        }
        sb.append("\r\n#kLoaded within ").append((double) (System.currentTimeMillis() - start) / 1000).append(" seconds.");//because I can, and it's free

        if (!item && !mob && !npc) {
            c.sendPacket(PacketCreator.getNPCTalk(9010000, (byte) 0, sb.toString(), "00 00", (byte) 0));
        } else {
            c.getPlayer().setDataSearch(sb.toString());
            c.getPlayer().setDataSearchArr(searchdata);
            c.getPlayer().setDataSearchType(item ? "item" : (mob ? "mob" : "npc"));
            NPCScriptManager.getInstance().start(c, 1032009, "search", c.getPlayer());
        }
    }
}
