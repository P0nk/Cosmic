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
package server.life;

import client.Character;
import client.Client;
import client.inventory.InventoryType;
import client.inventory.Item;
import config.YamlConfig;
import constants.game.GameConstants;
import constants.id.NpcId;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.life.positioner.PlayerNPCPodium;
import server.life.positioner.PlayerNPCPositioner;
import server.maps.AbstractMapObject;
import server.maps.MapObject;
import server.maps.MapObjectType;
import server.maps.MapleMap;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.Pair;

import java.awt.*;
import java.sql.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author XoticStory
 * @author Ronan
 */
public class PlayerNPC extends AbstractMapObject {
    private static final Logger log = LoggerFactory.getLogger(PlayerNPC.class);
    private static final Map<Byte, List<Integer>> availablePlayerNpcScriptIds = new HashMap<>();
    private static final AtomicInteger runningOverallRank = new AtomicInteger();
    private static final List<AtomicInteger> runningWorldRank = new ArrayList<>();
    private static final Map<Pair<Integer, Integer>, AtomicInteger> runningWorldJobRank = new HashMap<>();

    private Map<Short, Integer> equips = new HashMap<>();
    private int scriptId, face, hair, gender, job;
    private byte skin;
    private String name = "";
    private int dir, FH, RX0, RX1, CY;
    private int worldRank, overallRank, worldJobRank, overallJobRank;

    static {
        getRunningMetadata();
    }

    public PlayerNPC(String name, int scriptId, int face, int hair, int gender, byte skin, Map<Short, Integer> equips, int dir, int FH, int RX0, int RX1, int CX, int CY, int oid) {
        this.equips = equips;
        this.scriptId = scriptId;
        this.face = face;
        this.hair = hair;
        this.gender = gender;
        this.skin = skin;
        this.name = name;
        this.dir = dir;
        this.FH = FH;
        this.RX0 = RX0;
        this.RX1 = RX1;
        this.CY = CY;
        this.job = 7777;    // supposed to be developer

        setPosition(new Point(CX, CY));
        setObjectId(oid);
    }

    public PlayerNPC(ResultSet rs) {
        try {
            CY = rs.getInt("cy");
            name = rs.getString("name");
            hair = rs.getInt("hair");
            face = rs.getInt("face");
            skin = rs.getByte("skin");
            gender = rs.getInt("gender");
            dir = rs.getInt("dir");
            FH = rs.getInt("fh");
            RX0 = rs.getInt("rx0");
            RX1 = rs.getInt("rx1");
            scriptId = rs.getInt("scriptid");

            worldRank = rs.getInt("worldrank");
            overallRank = rs.getInt("overallrank");
            worldJobRank = rs.getInt("worldjobrank");
            overallJobRank = GameConstants.getOverallJobRankByScriptId(scriptId);
            job = rs.getInt("job");

            setPosition(new Point(rs.getInt("x"), CY));
            setObjectId(rs.getInt("id"));

            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT equippos, equipid FROM playernpcs_equip WHERE npcid = ?")) {
                ps.setInt(1, rs.getInt("id"));

                try (ResultSet rs2 = ps.executeQuery()) {
                    while (rs2.next()) {
                        equips.put(rs2.getShort("equippos"), rs2.getInt("equipid"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<Short, Integer> getEquips() {
        return equips;
    }

    public int getScriptId() {
        return scriptId;
    }

    public int getJob() {
        return job;
    }

    public int getDirection() {
        return dir;
    }

    public int getFH() {
        return FH;
    }

    public int getRX0() {
        return RX0;
    }

    public int getRX1() {
        return RX1;
    }

    public int getCY() {
        return CY;
    }

    public byte getSkin() {
        return skin;
    }

    public String getName() {
        return name;
    }

    public int getFace() {
        return face;
    }

    public int getHair() {
        return hair;
    }

    public int getGender() {
        return gender;
    }

    public int getWorldRank() {
        return worldRank;
    }

    public int getOverallRank() {
        return overallRank;
    }

    public int getWorldJobRank() {
        return worldJobRank;
    }

    public int getOverallJobRank() {
        return overallJobRank;
    }

    @Override
    public MapObjectType getType() {
        return MapObjectType.PLAYER_NPC;
    }

    @Override
    public void sendSpawnData(Client client) {
        client.sendPacket(PacketCreator.spawnPlayerNPC(this));
        client.sendPacket(PacketCreator.getPlayerNPC(this));
    }

    @Override
    public void sendDestroyData(Client client) {
        client.sendPacket(PacketCreator.removeNPCController(this.getObjectId()));
        client.sendPacket(PacketCreator.removePlayerNPC(this.getObjectId()));
    }

    private static void getRunningMetadata() {
        try (Connection con = DatabaseConnection.getConnection()) {
            getRunningOverallRanks(con);
            getRunningWorldRanks(con);
            getRunningWorldJobRanks(con);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void getRunningOverallRanks(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT max(overallrank) FROM playernpcs");
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                runningOverallRank.set(rs.getInt(1) + 1);
            } else {
                runningOverallRank.set(1);
            }
        }
    }

    private static void getRunningWorldRanks(Connection con) throws SQLException {
        int numWorlds = Server.getInstance().getWorldsSize();
        for (int i = 0; i < numWorlds; i++) {
            runningWorldRank.add(new AtomicInteger(1));
        }

        try (PreparedStatement ps = con.prepareStatement("SELECT world, max(worldrank) FROM playernpcs GROUP BY world ORDER BY world");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int wid = rs.getInt(1);
                if (wid < numWorlds) {
                    runningWorldRank.get(wid).set(rs.getInt(2) + 1);
                }
            }
        }
    }

    private static void getRunningWorldJobRanks(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT world, job, max(worldjobrank) FROM playernpcs GROUP BY world, job ORDER BY world, job");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                runningWorldJobRank.put(new Pair<>(rs.getInt(1), rs.getInt(2)), new AtomicInteger(rs.getInt(3) + 1));
            }
        }
    }

    private static int getAndIncrementRunningWorldJobRanks(int world, int job) {
        AtomicInteger wjr = runningWorldJobRank.get(new Pair<>(world, job));
        if (wjr == null) {
            wjr = new AtomicInteger(1);
            runningWorldJobRank.put(new Pair<>(world, job), wjr);
        }

        return wjr.getAndIncrement();
    }

    public static boolean canSpawnPlayerNpc(String name, int mapid) {
        boolean ret = true;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT name FROM playernpcs WHERE name LIKE ? AND map = ?")) {
            ps.setString(1, name);
            ps.setInt(2, mapid);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ret = false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ret;
    }

    public void updatePlayerNPCPosition(MapleMap map, Point newPos) {
        setPosition(newPos);
        RX0 = newPos.x + 50;
        RX1 = newPos.x - 50;
        CY = newPos.y;
        FH = map.getFootholds().findBelow(newPos).getId();

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE playernpcs SET x = ?, cy = ?, fh = ?, rx0 = ?, rx1 = ? WHERE id = ?")) {
            ps.setInt(1, newPos.x);
            ps.setInt(2, CY);
            ps.setInt(3, FH);
            ps.setInt(4, RX0);
            ps.setInt(5, RX1);
            ps.setInt(6, getObjectId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void fetchAvailableScriptIdsFromDb(byte branch, List<Integer> list) {
        try {
            int branchLen = (branch < 26) ? 100 : 400;
            int branchSid = NpcId.PLAYER_NPC_BASE + (branch * 100);
            int nextBranchSid = branchSid + branchLen;

            List<Integer> availables = new ArrayList<>(20);
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT scriptid FROM playernpcs WHERE scriptid >= ? AND scriptid < ? ORDER BY scriptid")) {
                ps.setInt(1, branchSid);
                ps.setInt(2, nextBranchSid);

                Set<Integer> usedScriptIds = new HashSet<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        usedScriptIds.add(rs.getInt(1));
                    }
                }

                int j = 0;
                for (int i = branchSid; i < nextBranchSid; i++) {
                    if (!usedScriptIds.contains(i)) {
                        if (PlayerNPCFactory.isExistentScriptid(i)) {  // thanks Ark, Zein, geno, Ariel, JrCl0wn for noticing client crashes due to use of missing scriptids
                            availables.add(i);
                            j++;

                            if (j == 20) {
                                break;
                            }
                        } else {
                            break;  // after this point no more scriptids expected...
                        }
                    }
                }
            }

            for (int i = availables.size() - 1; i >= 0; i--) {
                list.add(availables.get(i));
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
    }

    private static int getNextScriptId(byte branch) {
        List<Integer> availablesBranch = availablePlayerNpcScriptIds.get(branch);

        if (availablesBranch == null) {
            availablesBranch = new ArrayList<>(20);
            availablePlayerNpcScriptIds.put(branch, availablesBranch);
        }

        if (availablesBranch.isEmpty()) {
            fetchAvailableScriptIdsFromDb(branch, availablesBranch);

            if (availablesBranch.isEmpty()) {
                return -1;
            }
        }

        return availablesBranch.remove(availablesBranch.size() - 1);
    }

    private static PlayerNPC createPlayerNPCInternal(MapleMap map, Point pos, Character chr) {
        int mapId = map.getId();

        if (!canSpawnPlayerNpc(chr.getName(), mapId)) {
            return null;
        }

        byte branch = GameConstants.getHallOfFameBranch(chr.getJob(), mapId);

        int scriptId = getNextScriptId(branch);
        if (scriptId == -1) {
            return null;
        }

        if (pos == null) {
            if (GameConstants.isPodiumHallOfFameMap(map.getId())) {
                pos = PlayerNPCPodium.getNextPlayerNpcPosition(map);
            } else {
                pos = PlayerNPCPositioner.getNextPlayerNpcPosition(map);
            }

            if (pos == null) {
                return null;
            }
        }

        if (YamlConfig.config.server.USE_DEBUG) {
            log.debug("GOT SID {}, POS {}", scriptId, pos);
        }

        int worldId = chr.getWorld();
        int jobId = (chr.getJob().getId() / 100) * 100;

        PlayerNPC ret;
        try (Connection con = DatabaseConnection.getConnection()) {
            boolean createNew = false;
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM playernpcs WHERE scriptid = ?")) {
                ps.setInt(1, scriptId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        createNew = true;
                    }
                }
            }

            if (createNew) {   // creates new playernpc if scriptid doesn't exist
                final int npcId;
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO playernpcs (name, hair, face, skin, gender, x, cy, world, map, scriptid, dir, fh, rx0, rx1, worldrank, overallrank, worldjobrank, job) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, chr.getName());
                    ps.setInt(2, chr.getHair());
                    ps.setInt(3, chr.getFace());
                    ps.setInt(4, chr.getSkinColor().getId());
                    ps.setInt(5, chr.getGender());
                    ps.setInt(6, pos.x);
                    ps.setInt(7, pos.y);
                    ps.setInt(8, worldId);
                    ps.setInt(9, mapId);
                    ps.setInt(10, scriptId);
                    ps.setInt(11, 1);    // default direction
                    ps.setInt(12, map.getFootholds().findBelow(pos).getId());
                    ps.setInt(13, pos.x + 50);
                    ps.setInt(14, pos.x - 50);
                    ps.setInt(15, runningWorldRank.get(worldId).getAndIncrement());
                    ps.setInt(16, runningOverallRank.getAndIncrement());
                    ps.setInt(17, getAndIncrementRunningWorldJobRanks(worldId, jobId));
                    ps.setInt(18, jobId);
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        npcId = rs.getInt(1);
                    }
                }

                try (PreparedStatement ps = con.prepareStatement("INSERT INTO playernpcs_equip (npcid, equipid, equippos) VALUES (?, ?, ?)")) {
                    ps.setInt(1, npcId);

                    for (Item equip : chr.getInventory(InventoryType.EQUIPPED)) {
                        int position = Math.abs(equip.getPosition());
                        if ((position < 12 && position > 0) || (position > 100 && position < 112)) {
                            ps.setInt(2, equip.getItemId());
                            ps.setInt(3, equip.getPosition());
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM playernpcs WHERE id = ?")) {
                    ps.setInt(1, npcId);

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        ret = new PlayerNPC(rs);
                    }
                }
            } else {
                ret = null;
            }

            return ret;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<Integer> removePlayerNPCInternal(MapleMap map, Character chr) {
        Set<Integer> updateMapids = new HashSet<>();

        List<Integer> mapids = new LinkedList<>();
        mapids.add(chr.getWorld());

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id, map FROM playernpcs WHERE name LIKE ?" + (map != null ? " AND map = ?" : ""))) {
            ps.setString(1, chr.getName());
            if (map != null) {
                ps.setInt(2, map.getId());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    updateMapids.add(rs.getInt("map"));
                    int npcId = rs.getInt("id");

                    try (PreparedStatement ps2 = con.prepareStatement("DELETE FROM playernpcs WHERE id = ?")) {
                        ps2.setInt(1, npcId);
                        ps2.executeUpdate();
                    }

                    try (PreparedStatement ps2 = con.prepareStatement("DELETE FROM playernpcs_equip WHERE npcid = ?")) {
                        ps2.setInt(1, npcId);
                        ps2.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        mapids.addAll(updateMapids);

        return mapids;
    }

    private static synchronized Pair<PlayerNPC, List<Integer>> processPlayerNPCInternal(MapleMap map, Point pos, Character chr, boolean create) {
        if (create) {
            return new Pair<>(createPlayerNPCInternal(map, pos, chr), null);
        } else {
            return new Pair<>(null, removePlayerNPCInternal(map, chr));
        }
    }

    public static boolean spawnPlayerNPC(int mapid, Character chr) {
        return spawnPlayerNPC(mapid, null, chr);
    }

    public static boolean spawnPlayerNPC(int mapid, Point pos, Character chr) {
        if (chr == null) {
            return false;
        }

        PlayerNPC pn = processPlayerNPCInternal(chr.getClient().getChannelServer().getMapFactory().getMap(mapid), pos, chr, true).getLeft();
        if (pn != null) {
            for (Channel channel : Server.getInstance().getChannelsFromWorld(chr.getWorld())) {
                MapleMap m = channel.getMapFactory().getMap(mapid);

                m.addPlayerNPCMapObject(pn);
                m.broadcastMessage(PacketCreator.spawnPlayerNPC(pn));
                m.broadcastMessage(PacketCreator.getPlayerNPC(pn));
            }

            return true;
        } else {
            return false;
        }
    }

    private static PlayerNPC getPlayerNPCFromWorldMap(String name, int world, int map) {
        World wserv = Server.getInstance().getWorld(world);
        for (MapObject pnpcObj : wserv.getChannel(1).getMapFactory().getMap(map).getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.PLAYER_NPC))) {
            PlayerNPC pn = (PlayerNPC) pnpcObj;

            if (name.contentEquals(pn.getName()) && pn.getScriptId() < NpcId.CUSTOM_DEV) {
                return pn;
            }
        }

        return null;
    }

    public static void removePlayerNPC(Character chr) {
        if (chr == null) {
            return;
        }

        List<Integer> updateMapids = processPlayerNPCInternal(null, null, chr, false).getRight();
        int worldid = updateMapids.remove(0);

        for (Integer mapid : updateMapids) {
            PlayerNPC pn = getPlayerNPCFromWorldMap(chr.getName(), worldid, mapid);

            if (pn != null) {
                for (Channel channel : Server.getInstance().getChannelsFromWorld(worldid)) {
                    MapleMap m = channel.getMapFactory().getMap(mapid);
                    m.removeMapObject(pn);

                    m.broadcastMessage(PacketCreator.removeNPCController(pn.getObjectId()));
                    m.broadcastMessage(PacketCreator.removePlayerNPC(pn.getObjectId()));
                }
            }
        }
    }

    public static void multicastSpawnPlayerNPC(int mapid, int world) {
        World wserv = Server.getInstance().getWorld(world);
        if (wserv == null) {
            return;
        }

        Client c = Client.createMock();
        c.setWorld(world);
        c.setChannel(1);

        for (Character mc : wserv.loadAndGetAllCharactersView()) {
            mc.setClient(c);
            spawnPlayerNPC(mapid, mc);
        }
    }

    public static void removeAllPlayerNPC() {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT DISTINCT world, map FROM playernpcs");
             ResultSet rs = ps.executeQuery()) {
            int wsize = Server.getInstance().getWorldsSize();
            while (rs.next()) {
                int world = rs.getInt("world"), map = rs.getInt("map");
                if (world >= wsize) {
                    continue;
                }

                for (Channel channel : Server.getInstance().getChannelsFromWorld(world)) {
                    MapleMap m = channel.getMapFactory().getMap(map);

                    for (MapObject pnpcObj : m.getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.PLAYER_NPC))) {
                        PlayerNPC pn = (PlayerNPC) pnpcObj;
                        m.removeMapObject(pnpcObj);
                        m.broadcastMessage(PacketCreator.removeNPCController(pn.getObjectId()));
                        m.broadcastMessage(PacketCreator.removePlayerNPC(pn.getObjectId()));
                    }
                }
            }

            try (PreparedStatement ps2 = con.prepareStatement("DELETE FROM playernpcs")) {
                ps2.executeUpdate();
            }

            try (PreparedStatement ps2 = con.prepareStatement("DELETE FROM playernpcs_equip")) {
                ps2.executeUpdate();
            }

            try (PreparedStatement ps2 = con.prepareStatement("DELETE FROM playernpcs_field")) {
                ps2.executeUpdate();
            }

            for (World w : Server.getInstance().getWorlds()) {
                w.resetPlayerNpcMapData();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}