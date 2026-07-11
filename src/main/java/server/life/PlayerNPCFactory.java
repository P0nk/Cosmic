/*
    This file is part of the HeavenMS MapleStory Server
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
package server.life;

import constants.id.ItemId;
import constants.id.MapId;
import constants.id.NpcId;
import net.server.Server;
import provider.Data;
import provider.DataProvider;
import provider.DataProviderFactory;
import provider.DataTool;
import provider.wz.WZFiles;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author RonanLana
 */
public class PlayerNPCFactory {
    private static final DataProvider npcData = DataProviderFactory.getDataProvider(WZFiles.NPC);

    private static final Map<Integer, List<PlayerNPC>> dnpcMaps = new HashMap<>();
    private static Integer runningDeveloperOid = 2147483000;  // 647 slots, long enough

    public synchronized static boolean isExistentScriptid(int scriptid) {
        return npcData.getData(scriptid + ".img") != null;
    }

    private static void loadDeveloperRoomMetadata(DataProvider npc) {
        Data thisData = npc.getData(NpcId.CUSTOM_DEV + ".img");
        if (thisData != null) {
            DataProvider map = DataProviderFactory.getDataProvider(WZFiles.MAP);

            thisData = map.getData("Map/Map7/" + MapId.DEVELOPERS_HQ + ".img");
            if (thisData != null) {
                DataProvider sound = DataProviderFactory.getDataProvider(WZFiles.SOUND);

                thisData = sound.getData("Field.img");
                if (thisData != null) {
                    Data md = thisData.getChildByPath("anthem/brazil");
                    if (md != null) {
                        Server.getInstance().setAvailableDeveloperRoom();
                    }
                }
            }
        }
    }

    public synchronized static void loadFactoryMetadata() {
        DataProvider npc = npcData;
        loadDeveloperRoomMetadata(npc);

        DataProvider etc = DataProviderFactory.getDataProvider(WZFiles.ETC);
        Data dnpcData = etc.getData("DeveloperNpc.img");
        if (dnpcData != null) {
            for (Data data : dnpcData.getChildren()) {
                int scriptId = Integer.parseInt(data.getName());

                String name = DataTool.getString("name", data, "");
                int face = DataTool.getIntConvert("face", data, 20000);
                int hair = DataTool.getIntConvert("hair", data, 30000);
                int gender = DataTool.getIntConvert("gender", data, 0);
                byte skin = (byte) DataTool.getIntConvert("skin", data, 0);
                int dir = DataTool.getIntConvert("dir", data, 0);
                int mapid = DataTool.getIntConvert("map", data, 0);
                int FH = DataTool.getIntConvert("fh", data, 0);
                int RX0 = DataTool.getIntConvert("rx0", data, 0);
                int RX1 = DataTool.getIntConvert("rx1", data, 0);
                int CX = DataTool.getIntConvert("cx", data, 0);
                int CY = DataTool.getIntConvert("cy", data, 0);

                Map<Short, Integer> equips = new HashMap<>();
                for (Data edata : data.getChildByPath("equips").getChildren()) {
                    short equippos = (short) DataTool.getIntConvert("pos", edata);
                    int equipid = DataTool.getIntConvert("itemid", edata);

                    equips.put(equippos, equipid);
                }

                List<PlayerNPC> dnpcSet = dnpcMaps.get(mapid);
                if (dnpcSet == null) {
                    dnpcSet = new LinkedList<>();
                    dnpcMaps.put(mapid, dnpcSet);
                }

                dnpcSet.add(new PlayerNPC(name, scriptId, face, hair, gender, skin, equips, dir, FH, RX0, RX1, CX, CY, runningDeveloperOid));
                runningDeveloperOid++;
            }
        } else {
            Data thisData = npc.getData(NpcId.CUSTOM_DEV + ".img");

            if (thisData != null) {
                byte[] encData = {0x52, 0x6F, 0x6E, 0x61, 0x6E};
                String name = new String(encData);
                int face = 20104, hair = 30215, gender = 0, skin = 0, dir = 0, mapid = MapId.DEVELOPERS_HQ;
                int FH = 4, RX0 = -143, RX1 = -243, CX = -193, CY = 117, scriptId = NpcId.CUSTOM_DEV;

                Map<Short, Integer> equips = new HashMap<>();
                equips.put((short) -1, ItemId.GREEN_HEADBAND);
                equips.put((short) -11, ItemId.TIMELESS_NIBLEHEIM);
                equips.put((short) -8, ItemId.BLUE_KORBEN);
                equips.put((short) -6, ItemId.MITHRIL_PLATINE_PANTS);
                equips.put((short) -7, ItemId.BLUE_CARZEN_BOOTS);
                equips.put((short) -5, ItemId.MITHRIL_PLATINE);

                List<PlayerNPC> dnpcSet = dnpcMaps.get(mapid);
                if (dnpcSet == null) {
                    dnpcSet = new LinkedList<>();
                    dnpcMaps.put(mapid, dnpcSet);
                }

                dnpcSet.add(new PlayerNPC(name, scriptId, face, hair, gender, (byte) skin, equips, dir, FH, RX0, RX1, CX, CY, runningDeveloperOid));
                runningDeveloperOid++;
            }
        }
    }

    public synchronized static List<PlayerNPC> getDeveloperNpcsFromMapid(int mapid) {
        return dnpcMaps.get(mapid);
    }
}
