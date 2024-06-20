/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

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
package server.maps;

import constants.id.MapId;
import provider.Data;
import provider.DataProvider;
import provider.DataProviderFactory;
import provider.DataTool;
import provider.wz.WZFiles;
import scripting.event.EventInstanceManager;
import server.life.AbstractLoadedLife;
import server.life.LifeFactory;
import server.life.Monster;
import server.life.PlayerNPC;
import server.partyquest.GuardianSpawnPoint;
import tools.DatabaseConnection;
import tools.StringUtil;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;

public class MapFactory {
    private static final Data nameData;
    private static final DataProvider mapSource;

    static {
        nameData = DataProviderFactory.getDataProvider(WZFiles.STRING).getData("Map.img");
        mapSource = DataProviderFactory.getDataProvider(WZFiles.MAP);
    }

    private static void loadLifeFromWz(MapleMap map, Data mapData) {
        for (Data life : mapData.getChildByPath("life")) {
            life.getName();
            String id = DataTool.getString(life.getChildByPath("id"));
            String type = DataTool.getString(life.getChildByPath("type"));
            int team = DataTool.getInt("team", life, -1);
            if (map.isCPQMap2() && type.equals("m")) {
                if ((Integer.parseInt(life.getName()) % 2) == 0) {
                    team = 0;
                } else {
                    team = 1;
                }
            }
            int cy = DataTool.getInt(life.getChildByPath("cy"));
            Data dF = life.getChildByPath("f");
            int f = (dF != null) ? DataTool.getInt(dF) : 0;
            int fh = DataTool.getInt(life.getChildByPath("fh"));
            int rx0 = DataTool.getInt(life.getChildByPath("rx0"));
            int rx1 = DataTool.getInt(life.getChildByPath("rx1"));
            int x = DataTool.getInt(life.getChildByPath("x"));
            int y = DataTool.getInt(life.getChildByPath("y"));
            int hide = DataTool.getInt("hide", life, 0);
            int mobTime = DataTool.getInt("mobTime", life, 0);

            loadLifeRaw(map, Integer.parseInt(id), type, cy, f, fh, rx0, rx1, x, y, hide, mobTime, team);
        }
    }

    private static void loadLifeFromDb(MapleMap map) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM plife WHERE map = ? and world = ?")) {
            ps.setInt(1, map.getId());
            ps.setInt(2, map.getWorld());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("life");
                    String type = rs.getString("type");
                    int cy = rs.getInt("cy");
                    int f = rs.getInt("f");
                    int fh = rs.getInt("fh");
                    int rx0 = rs.getInt("rx0");
                    int rx1 = rs.getInt("rx1");
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int hide = rs.getInt("hide");
                    int mobTime = rs.getInt("mobtime");
                    int team = rs.getInt("team");

                    loadLifeRaw(map, id, type, cy, f, fh, rx0, rx1, x, y, hide, mobTime, team);
                }
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
    }

    private static void loadLifeRaw(MapleMap map, int id, String type, int cy, int f, int fh, int rx0, int rx1, int x, int y, int hide, int mobTime, int team) {
        AbstractLoadedLife myLife = loadLife(id, type, cy, f, fh, rx0, rx1, x, y, hide);
        if (myLife instanceof Monster monster) {

            if (mobTime == -1) { //does not respawn, force spawn once
                map.spawnMonster(monster);
            } else {
                map.addMonsterSpawn(monster, mobTime, team);
            }

            //should the map be reseted, use allMonsterSpawn list of monsters to spawn them again
            map.addAllMonsterSpawn(monster, mobTime, team);
        } else {
            map.addMapObject(myLife);
        }
    }

    public static MapleMap loadMapFromWz(int mapid, int world, int channel, EventInstanceManager event) {
        MapleMap map;

        String mapName = getMapName(mapid);
        Data mapData = mapSource.getData(mapName);    // source.getData issue with giving nulls in rare ocasions found thanks to MedicOP
        Data infoData = mapData.getChildByPath("info");

        String link = DataTool.getString(infoData.getChildByPath("link"), "");
        if (!link.equals("")) { //nexon made hundreds of dojo maps so to reduce the size they added links.
            mapName = getMapName(Integer.parseInt(link));
            mapData = mapSource.getData(mapName);
        }
        float monsterRate = 0;
        Data mobRate = infoData.getChildByPath("mobRate");
        if (mobRate != null) {
            monsterRate = (Float) mobRate.getData();
        }
        map = new MapleMap(mapid, world, channel, DataTool.getInt("returnMap", infoData), monsterRate);
        map.setEventInstance(event);

        String onFirstEnter = DataTool.getString(infoData.getChildByPath("onFirstUserEnter"), String.valueOf(mapid));
        map.setOnFirstUserEnter(onFirstEnter.equals("") ? String.valueOf(mapid) : onFirstEnter);

        String onEnter = DataTool.getString(infoData.getChildByPath("onUserEnter"), String.valueOf(mapid));
        map.setOnUserEnter(onEnter.equals("") ? String.valueOf(mapid) : onEnter);

        map.setFieldLimit(DataTool.getInt(infoData.getChildByPath("fieldLimit"), 0));
        map.setMobInterval((short) DataTool.getInt(infoData.getChildByPath("createMobInterval"), 5000));
        PortalFactory portalFactory = new PortalFactory();
        for (Data portal : mapData.getChildByPath("portal")) {
            map.addPortal(portalFactory.makePortal(DataTool.getInt(portal.getChildByPath("pt")), map.getId(), portal));
        }
        Data timeMob = infoData.getChildByPath("timeMob");
        if (timeMob != null) {
            map.setTimeMob(DataTool.getInt(timeMob.getChildByPath("id")), DataTool.getString(timeMob.getChildByPath("message")));
        }

        int[] bounds = new int[4];
        bounds[0] = DataTool.getInt(infoData.getChildByPath("VRTop"));
        bounds[1] = DataTool.getInt(infoData.getChildByPath("VRBottom"));

        if (bounds[0] == bounds[1]) {    // old-style baked map
            Data minimapData = mapData.getChildByPath("miniMap");
            if (minimapData != null) {
                bounds[0] = DataTool.getInt(minimapData.getChildByPath("centerX")) * -1;
                bounds[1] = DataTool.getInt(minimapData.getChildByPath("centerY")) * -1;
                bounds[2] = DataTool.getInt(minimapData.getChildByPath("height"));
                bounds[3] = DataTool.getInt(minimapData.getChildByPath("width"));

                map.setMapPointBoundings(bounds[0], bounds[1], bounds[2], bounds[3]);
            } else {
                int dist = (1 << 18);
                map.setMapPointBoundings(-dist / 2, -dist / 2, dist, dist);
            }
        } else {
            bounds[2] = DataTool.getInt(infoData.getChildByPath("VRLeft"));
            bounds[3] = DataTool.getInt(infoData.getChildByPath("VRRight"));

            map.setMapLineBoundings(bounds[0], bounds[1], bounds[2], bounds[3]);
        }

        List<Foothold> allFootholds = new LinkedList<>();
        Point lBound = new Point();
        Point uBound = new Point();
        for (Data footRoot : mapData.getChildByPath("foothold")) {
            for (Data footCat : footRoot) {
                for (Data footHold : footCat) {
                    int x1 = DataTool.getInt(footHold.getChildByPath("x1"));
                    int y1 = DataTool.getInt(footHold.getChildByPath("y1"));
                    int x2 = DataTool.getInt(footHold.getChildByPath("x2"));
                    int y2 = DataTool.getInt(footHold.getChildByPath("y2"));
                    Foothold fh = new Foothold(new Point(x1, y1), new Point(x2, y2), Integer.parseInt(footHold.getName()));
                    fh.setPrev(DataTool.getInt(footHold.getChildByPath("prev")));
                    fh.setNext(DataTool.getInt(footHold.getChildByPath("next")));
                    if (fh.getX1() < lBound.x) {
                        lBound.x = fh.getX1();
                    }
                    if (fh.getX2() > uBound.x) {
                        uBound.x = fh.getX2();
                    }
                    if (fh.getY1() < lBound.y) {
                        lBound.y = fh.getY1();
                    }
                    if (fh.getY2() > uBound.y) {
                        uBound.y = fh.getY2();
                    }
                    allFootholds.add(fh);
                }
            }
        }
        FootholdTree fTree = new FootholdTree(lBound, uBound);
        for (Foothold fh : allFootholds) {
            fTree.insert(fh);
        }
        map.setFootholds(fTree);
        if (mapData.getChildByPath("area") != null) {
            for (Data area : mapData.getChildByPath("area")) {
                int x1 = DataTool.getInt(area.getChildByPath("x1"));
                int y1 = DataTool.getInt(area.getChildByPath("y1"));
                int x2 = DataTool.getInt(area.getChildByPath("x2"));
                int y2 = DataTool.getInt(area.getChildByPath("y2"));
                map.addMapleArea(new Rectangle(x1, y1, (x2 - x1), (y2 - y1)));
            }
        }
        if (mapData.getChildByPath("seat") != null) {
            int seats = mapData.getChildByPath("seat").getChildren().size();
            map.setSeats(seats);
        }
        if (event == null) {
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT * FROM playernpcs WHERE map = ? AND world = ?")) {
                ps.setInt(1, mapid);
                ps.setInt(2, world);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        map.addPlayerNPCMapObject(new PlayerNPC(rs));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        loadLifeFromWz(map, mapData);
        loadLifeFromDb(map);

        if (map.isCPQMap()) {
            Data mcData = mapData.getChildByPath("monsterCarnival");
            if (mcData != null) {
                map.setDeathCP(DataTool.getIntConvert("deathCP", mcData, 0));
                map.setMaxMobs(DataTool.getIntConvert("mobGenMax", mcData, 20));    // thanks Atoot for noticing CPQ1 bf. 3 and 4 not accepting spawns due to undefined limits, Lame for noticing a need to cap mob spawns even on such undefined limits
                map.setTimeDefault(DataTool.getIntConvert("timeDefault", mcData, 0));
                map.setTimeExpand(DataTool.getIntConvert("timeExpand", mcData, 0));
                map.setMaxReactors(DataTool.getIntConvert("guardianGenMax", mcData, 16));
                Data guardianGenData = mcData.getChildByPath("guardianGenPos");
                for (Data node : guardianGenData.getChildren()) {
                    GuardianSpawnPoint pt = new GuardianSpawnPoint(new Point(DataTool.getIntConvert("x", node), DataTool.getIntConvert("y", node)));
                    pt.setTeam(DataTool.getIntConvert("team", node, -1));
                    pt.setTaken(false);
                    map.addGuardianSpawnPoint(pt);
                }
                if (mcData.getChildByPath("skill") != null) {
                    for (Data area : mcData.getChildByPath("skill")) {
                        map.addSkillId(DataTool.getInt(area));
                    }
                }

                if (mcData.getChildByPath("mob") != null) {
                    for (Data area : mcData.getChildByPath("mob")) {
                        map.addMobSpawn(DataTool.getInt(area.getChildByPath("id")), DataTool.getInt(area.getChildByPath("spendCP")));
                    }
                }
            }

        }

        if (mapData.getChildByPath("reactor") != null) {
            for (Data reactor : mapData.getChildByPath("reactor")) {
                String id = DataTool.getString(reactor.getChildByPath("id"));
                if (id != null) {
                    Reactor newReactor = loadReactor(reactor, id, (byte) DataTool.getInt(reactor.getChildByPath("f"), 0));
                    map.spawnReactor(newReactor);
                }
            }
        }

        map.setMapName(loadPlaceName(mapid));
        map.setStreetName(loadStreetName(mapid));

        map.setClock(mapData.getChildByPath("clock") != null);
        map.setEverlast(DataTool.getIntConvert("everlast", infoData, 0) != 0); // thanks davidlafriniere for noticing value 0 accounting as true
        map.setTown(DataTool.getIntConvert("town", infoData, 0) != 0);
        map.setHPDec(DataTool.getIntConvert("decHP", infoData, 0));
        map.setHPDecProtect(DataTool.getIntConvert("protectItem", infoData, 0));
        map.setForcedReturnMap(DataTool.getInt(infoData.getChildByPath("forcedReturn"), MapId.NONE));
        map.setBoat(mapData.getChildByPath("shipObj") != null);
        map.setTimeLimit(DataTool.getIntConvert("timeLimit", infoData, -1));
        map.setFieldType(DataTool.getIntConvert("fieldType", infoData, 0));
        map.setMobCapacity(DataTool.getIntConvert("fixedMobCapacity", infoData, 500));//Is there a map that contains more than 500 mobs?

        Data recData = infoData.getChildByPath("recovery");
        if (recData != null) {
            map.setRecovery(DataTool.getFloat(recData));
        }

        HashMap<Integer, Integer> backTypes = new HashMap<>();
        try {
            for (Data layer : mapData.getChildByPath("back")) { // yolo
                int layerNum = Integer.parseInt(layer.getName());
                int btype = DataTool.getInt(layer.getChildByPath("type"), 0);

                backTypes.put(layerNum, btype);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // swallow cause I'm cool
        }

        map.setBackgroundTypes(backTypes);
        map.generateMapDropRangeCache();

        return map;
    }

    private static AbstractLoadedLife loadLife(int id, String type, int cy, int f, int fh, int rx0, int rx1, int x, int y, int hide) {
        AbstractLoadedLife myLife = LifeFactory.getLife(id, type);
        myLife.setCy(cy);
        myLife.setF(f);
        myLife.setFh(fh);
        myLife.setRx0(rx0);
        myLife.setRx1(rx1);
        myLife.setPosition(new Point(x, y));
        if (hide == 1) {
            myLife.setHide(true);
        }
        return myLife;
    }

    private static Reactor loadReactor(Data reactor, String id, final byte FacingDirection) {
        Reactor myReactor = new Reactor(ReactorFactory.getReactor(Integer.parseInt(id)), Integer.parseInt(id));
        int x = DataTool.getInt(reactor.getChildByPath("x"));
        int y = DataTool.getInt(reactor.getChildByPath("y"));
        myReactor.setFacingDirection(FacingDirection);
        myReactor.setPosition(new Point(x, y));
        myReactor.setDelay((int) SECONDS.toMillis(DataTool.getInt(reactor.getChildByPath("reactorTime"))));
        myReactor.setName(DataTool.getString(reactor.getChildByPath("name"), ""));
        myReactor.resetReactorActions(0);
        return myReactor;
    }

    private static String getMapName(int mapid) {
        String mapName = StringUtil.getLeftPaddedStr(Integer.toString(mapid), '0', 9);
        StringBuilder builder = new StringBuilder("Map/Map");
        int area = mapid / 100000000;
        builder.append(area);
        builder.append("/");
        builder.append(mapName);
        builder.append(".img");
        mapName = builder.toString();
        return mapName;
    }

    private static String getMapStringName(int mapid) {
        StringBuilder builder = new StringBuilder();
        if (mapid < 100000000) {
            builder.append("maple");
        } else if (mapid >= 100000000 && mapid < MapId.ORBIS) {
            builder.append("victoria");
        } else if (mapid >= MapId.ORBIS && mapid < MapId.ELLIN_FOREST) {
            builder.append("ossyria");
        } else if (mapid >= MapId.ELLIN_FOREST && mapid < 400000000) {
            builder.append("elin");
        } else if (mapid >= MapId.SINGAPORE && mapid < 560000000) {
            builder.append("singapore");
        } else if (mapid >= MapId.NEW_LEAF_CITY && mapid < 620000000) {
            builder.append("MasteriaGL");
        } else if (mapid >= 677000000 && mapid < 677100000) {
            builder.append("Episode1GL");
        } else if (mapid >= 670000000 && mapid < 682000000) {
            if ((mapid >= 674030000 && mapid < 674040000) || (mapid >= 680100000 && mapid < 680200000)) {
                builder.append("etc");
            } else {
                builder.append("weddingGL");
            }
        } else if (mapid >= 682000000 && mapid < 683000000) {
            builder.append("HalloweenGL");
        } else if (mapid >= 683000000 && mapid < 684000000) {
            builder.append("event");
        } else if (mapid >= MapId.MUSHROOM_SHRINE && mapid < 900000000) {
            if ((mapid >= 889100000 && mapid < 889200000)) {
                builder.append("etc");
            } else {
                builder.append("jp");
            }
        } else {
            builder.append("etc");
        }
        builder.append("/").append(mapid);
        return builder.toString();
    }

    public static String loadPlaceName(int mapid) {
        try {
            return DataTool.getString("mapName", nameData.getChildByPath(getMapStringName(mapid)), "");
        } catch (Exception e) {
            return "";
        }
    }

    public static String loadStreetName(int mapid) {
        try {
            return DataTool.getString("streetName", nameData.getChildByPath(getMapStringName(mapid)), "");
        } catch (Exception e) {
            return "";
        }
    }

}
