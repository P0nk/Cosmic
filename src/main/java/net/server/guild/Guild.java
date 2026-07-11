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
package net.server.guild;

import client.Character;
import client.Client;
import config.YamlConfig;
import net.packet.Packet;
import net.server.PlayerStorage;
import net.server.Server;
import net.server.channel.Channel;
import net.server.coordinator.matchchecker.MatchCheckerCoordinator;
import net.server.coordinator.world.InviteCoordinator;
import net.server.coordinator.world.InviteCoordinator.InviteResult;
import net.server.coordinator.world.InviteCoordinator.InviteType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.NoteService;
import tools.DatabaseConnection;
import tools.PacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Guild {
    private static final Logger log = LoggerFactory.getLogger(Guild.class);

    private enum BCOp {
        NONE, DISBAND, EMBLEMCHANGE
    }

    private final List<GuildCharacter> members;
    private final Lock membersLock = new ReentrantLock(true);

    private final String[] rankTitles = new String[5]; // 1 = master, 2 = jr, 5 = lowest member
    private String name, notice;
    private int id, gp, logo, logoColor, leader, capacity, logoBG, logoBGColor, signature, allianceId;
    private final int world;
    private final Map<Integer, List<Integer>> notifications = new LinkedHashMap<>();
    private boolean bDirty = true;

    public Guild(int guildid, int world) {
        this.world = world;
        members = new ArrayList<>();

        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM guilds WHERE guildid = " + guildid);
                 ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    id = -1;
                    return;
                }
                id = guildid;
                name = rs.getString("name");
                gp = rs.getInt("GP");
                logo = rs.getInt("logo");
                logoColor = rs.getInt("logoColor");
                logoBG = rs.getInt("logoBG");
                logoBGColor = rs.getInt("logoBGColor");
                capacity = rs.getInt("capacity");
                for (int i = 1; i <= 5; i++) {
                    rankTitles[i - 1] = rs.getString("rank" + i + "title");
                }
                leader = rs.getInt("leader");
                notice = rs.getString("notice");
                signature = rs.getInt("signature");
                allianceId = rs.getInt("allianceId");
            }

            try (PreparedStatement ps = con.prepareStatement("SELECT id, name, level, job, guildrank, allianceRank FROM characters WHERE guildid = ? ORDER BY guildrank ASC, name ASC")) {
                ps.setInt(1, guildid);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return;
                    }

                    do {
                        members.add(new GuildCharacter(null, rs.getInt("id"), rs.getInt("level"), rs.getString("name"), (byte) -1, world, rs.getInt("job"), rs.getInt("guildrank"), guildid, false, rs.getInt("allianceRank")));
                    } while (rs.next());
                }
            }
        } catch (SQLException se) {
            log.error("Unable to read guild information from sql", se);
        }
    }

    private void buildNotifications() {
        if (!bDirty) {
            return;
        }
        Set<Integer> chs = Server.getInstance().getOpenChannels(world);
        synchronized (notifications) {
            if (notifications.keySet().size() != chs.size()) {
                notifications.clear();
                for (Integer ch : chs) {
                    notifications.put(ch, new LinkedList<>());
                }
            } else {
                for (List<Integer> l : notifications.values()) {
                    l.clear();
                }
            }
        }

        membersLock.lock();
        try {
            for (GuildCharacter mgc : members) {
                if (!mgc.isOnline()) {
                    continue;
                }

                List<Integer> chl;
                synchronized (notifications) {
                    chl = notifications.get(mgc.getChannel());
                }
                if (chl != null) {
                    chl.add(mgc.getId());
                }
                //Unable to connect to Channel... error was here
            }
        } finally {
            membersLock.unlock();
        }

        bDirty = false;
    }

    public void writeToDB(boolean bDisband) {
        try (Connection con = DatabaseConnection.getConnection()) {

            if (!bDisband) {
                StringBuilder builder = new StringBuilder();
                builder.append("UPDATE guilds SET GP = ?, logo = ?, logoColor = ?, logoBG = ?, logoBGColor = ?, ");
                for (int i = 0; i < 5; i++) {
                    builder.append("rank").append(i + 1).append("title = ?, ");
                }
                builder.append("capacity = ?, notice = ? WHERE guildid = ?");
                try (PreparedStatement ps = con.prepareStatement(builder.toString())) {
                    ps.setInt(1, gp);
                    ps.setInt(2, logo);
                    ps.setInt(3, logoColor);
                    ps.setInt(4, logoBG);
                    ps.setInt(5, logoBGColor);
                    for (int i = 6; i < 11; i++) {
                        ps.setString(i, rankTitles[i - 6]);
                    }
                    ps.setInt(11, capacity);
                    ps.setString(12, notice);
                    ps.setInt(13, this.id);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = 0, guildrank = 5 WHERE guildid = ?")) {
                    ps.setInt(1, this.id);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement("DELETE FROM guilds WHERE guildid = ?")) {
                    ps.setInt(1, this.id);
                    ps.executeUpdate();
                }

                membersLock.lock();
                try {
                    this.broadcast(GuildPackets.guildDisband(this.id));
                } finally {
                    membersLock.unlock();
                }
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public int getId() {
        return id;
    }

    public int getLeaderId() {
        return leader;
    }

    public int setLeaderId(int charId) {
        return leader = charId;
    }

    public int getGP() {
        return gp;
    }

    public int getLogo() {
        return logo;
    }

    public void setLogo(int l) {
        logo = l;
    }

    public int getLogoColor() {
        return logoColor;
    }

    public void setLogoColor(int c) {
        logoColor = c;
    }

    public int getLogoBG() {
        return logoBG;
    }

    public void setLogoBG(int bg) {
        logoBG = bg;
    }

    public int getLogoBGColor() {
        return logoBGColor;
    }

    public void setLogoBGColor(int c) {
        logoBGColor = c;
    }

    public String getNotice() {
        if (notice == null) {
            return "";
        }
        return notice;
    }

    public String getName() {
        return name;
    }

    public List<GuildCharacter> getMembers() {
        membersLock.lock();
        try {
            return new ArrayList<>(members);
        } finally {
            membersLock.unlock();
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public int getSignature() {
        return signature;
    }

    public void broadcastNameChanged() {
        PlayerStorage ps = Server.getInstance().getWorld(world).getPlayerStorage();

        for (GuildCharacter mgc : getMembers()) {
            Character chr = ps.getCharacterById(mgc.getId());
            if (chr == null || !chr.isLoggedinWorld()) {
                continue;
            }

            Packet packet = GuildPackets.guildNameChanged(chr.getId(), this.getName());
            chr.getMap().broadcastPacket(chr, packet);
        }
    }

    public void broadcastEmblemChanged() {
        PlayerStorage ps = Server.getInstance().getWorld(world).getPlayerStorage();

        for (GuildCharacter mgc : getMembers()) {
            Character chr = ps.getCharacterById(mgc.getId());
            if (chr == null || !chr.isLoggedinWorld()) {
                continue;
            }

            Packet packet = GuildPackets.guildMarkChanged(chr.getId(), this);
            chr.getMap().broadcastPacket(chr, packet);
        }
    }

    public void broadcastInfoChanged() {
        PlayerStorage ps = Server.getInstance().getWorld(world).getPlayerStorage();

        for (GuildCharacter mgc : getMembers()) {
            Character chr = ps.getCharacterById(mgc.getId());
            if (chr == null || !chr.isLoggedinWorld()) {
                continue;
            }

            chr.sendPacket(GuildPackets.showGuildInfo(chr));
        }
    }

    public void broadcast(Packet packet) {
        broadcast(packet, -1, BCOp.NONE);
    }

    public void broadcast(Packet packet, int exception) {
        broadcast(packet, exception, BCOp.NONE);
    }

    public void broadcast(Packet packet, int exceptionId, BCOp bcop) {
        membersLock.lock(); // membersLock awareness thanks to ProjectNano dev team
        try {
            synchronized (notifications) {
                if (bDirty) {
                    buildNotifications();
                }
                try {
                    for (Integer b : Server.getInstance().getOpenChannels(world)) {
                        if (notifications.get(b).size() > 0) {
                            if (bcop == BCOp.DISBAND) {
                                Server.getInstance().getWorld(world).setGuildAndRank(notifications.get(b), 0, 5, exceptionId);
                            } else if (bcop == BCOp.EMBLEMCHANGE) {
                                Server.getInstance().getWorld(world).changeEmblem(this.id, notifications.get(b), new GuildSummary(this));
                            } else {
                                Server.getInstance().getWorld(world).sendPacket(notifications.get(b), packet, exceptionId);
                            }
                        }
                    }
                } catch (Exception re) {
                    log.error("Failed to contact channel(s) for broadcast.", re);
                }
            }
        } finally {
            membersLock.unlock();
        }
    }

    public void guildMessage(Packet serverNotice) {
        membersLock.lock();
        try {
            for (GuildCharacter mgc : members) {
                for (Channel cs : Server.getInstance().getChannelsFromWorld(world)) {
                    if (cs.getPlayerStorage().getCharacterById(mgc.getId()) != null) {
                        cs.getPlayerStorage().getCharacterById(mgc.getId()).sendPacket(serverNotice);
                        break;
                    }
                }
            }
        } finally {
            membersLock.unlock();
        }
    }

    public void dropMessage(String message) {
        dropMessage(5, message);
    }

    public void dropMessage(int type, String message) {
        membersLock.lock();
        try {
            for (GuildCharacter mgc : members) {
                if (mgc.getCharacter() != null) {
                    mgc.getCharacter().dropMessage(type, message);
                }
            }
        } finally {
            membersLock.unlock();
        }
    }

    public void broadcastMessage(Packet packet) {
        Server.getInstance().guildMessage(id, packet);
    }

    public final void setOnline(int cid, boolean online, int channel) {
        membersLock.lock();
        try {
            boolean bBroadcast = true;
            for (GuildCharacter mgc : members) {
                if (mgc.getId() == cid) {
                    if (mgc.isOnline() && online) {
                        bBroadcast = false;
                    }
                    mgc.setOnline(online);
                    mgc.setChannel(channel);
                    break;
                }
            }
            if (bBroadcast) {
                this.broadcast(GuildPackets.guildMemberOnline(id, cid, online), cid);
            }
            bDirty = true;
        } finally {
            membersLock.unlock();
        }
    }

    public void guildChat(String name, int cid, String message) {
        membersLock.lock();
        try {
            this.broadcast(PacketCreator.multiChat(name, message, 2), cid);
        } finally {
            membersLock.unlock();
        }
    }

    public String getRankTitle(int rank) {
        return rankTitles[rank - 1];
    }

    public static int createGuild(int leaderId, String name) {
        try (Connection con = DatabaseConnection.getConnection()) {

            try (PreparedStatement ps = con.prepareStatement("SELECT guildid FROM guilds WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return 0;
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement("INSERT INTO guilds (`leader`, `name`, `signature`) VALUES (?, ?, ?)")) {
                ps.setInt(1, leaderId);
                ps.setString(2, name);
                ps.setInt(3, (int) System.currentTimeMillis());
                ps.executeUpdate();
            }

            final int guildId;
            try (PreparedStatement ps = con.prepareStatement("SELECT guildid FROM guilds WHERE leader = ?")) {
                ps.setInt(1, leaderId);

                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    guildId = rs.getInt("guildid");
                }
            }

            try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = ? WHERE id = ?")) {
                ps.setInt(1, guildId);
                ps.setInt(2, leaderId);
                ps.executeUpdate();
            }

            return guildId;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int addGuildMember(GuildCharacter mgc, Character chr) {
        membersLock.lock();
        try {
            if (members.size() >= capacity) {
                return 0;
            }
            for (int i = members.size() - 1; i >= 0; i--) {
                if (members.get(i).getGuildRank() < 5 || members.get(i).getName().compareTo(mgc.getName()) < 0) {
                    mgc.setCharacter(chr);
                    members.add(i + 1, mgc);
                    bDirty = true;
                    break;
                }
            }

            this.broadcast(GuildPackets.newGuildMember(mgc));
            return 1;
        } finally {
            membersLock.unlock();
        }
    }

    public void leaveGuild(GuildCharacter mgc) {
        membersLock.lock();
        try {
            this.broadcast(GuildPackets.memberLeft(mgc, false));
            members.remove(mgc);
            bDirty = true;
        } finally {
            membersLock.unlock();
        }
    }

    public void expelMember(GuildCharacter initiator, String name, int cid, NoteService noteService) {
        membersLock.lock();
        try {
            java.util.Iterator<GuildCharacter> itr = members.iterator();
            GuildCharacter mgc;
            while (itr.hasNext()) {
                mgc = itr.next();
                if (mgc.getId() == cid && initiator.getGuildRank() < mgc.getGuildRank()) {
                    this.broadcast(GuildPackets.memberLeft(mgc, true));
                    itr.remove();
                    bDirty = true;
                    try {
                        if (mgc.isOnline()) {
                            Server.getInstance().getWorld(mgc.getWorld()).setGuildAndRank(cid, 0, 5);
                        } else {
                            noteService.sendNormal("You have been expelled from the guild.", initiator.getName(), mgc.getName());
                            Server.getInstance().getWorld(mgc.getWorld()).setOfflineGuildStatus((short) 0, (byte) 5, cid);
                        }
                    } catch (Exception re) {
                        re.printStackTrace();
                        return;
                    }
                    return;
                }
            }
            log.warn("Unable to find member with name {} and id {}", name, cid);
        } finally {
            membersLock.unlock();
        }
    }

    public void changeRank(int cid, int newRank) {
        membersLock.lock();
        try {
            for (GuildCharacter mgc : members) {
                if (cid == mgc.getId()) {
                    changeRank(mgc, newRank);
                    return;
                }
            }
        } finally {
            membersLock.unlock();
        }
    }

    public void changeRank(GuildCharacter mgc, int newRank) {
        try {
            if (mgc.isOnline()) {
                Server.getInstance().getWorld(mgc.getWorld()).setGuildAndRank(mgc.getId(), this.id, newRank);
                mgc.setGuildRank(newRank);
            } else {
                Server.getInstance().getWorld(mgc.getWorld()).setOfflineGuildStatus((short) this.id, (byte) newRank, mgc.getId());
                mgc.setOfflineGuildRank(newRank);
            }
        } catch (Exception re) {
            re.printStackTrace();
            return;
        }

        membersLock.lock();
        try {
            this.broadcast(GuildPackets.changeRank(mgc));
        } finally {
            membersLock.unlock();
        }
    }

    public void setGuildNotice(String notice) {
        this.notice = notice;
        this.writeToDB(false);

        membersLock.lock();
        try {
            this.broadcast(GuildPackets.guildNotice(this.id, notice));
        } finally {
            membersLock.unlock();
        }
    }

    public void memberLevelJobUpdate(GuildCharacter mgc) {
        membersLock.lock();
        try {
            for (GuildCharacter member : members) {
                if (mgc.equals(member)) {
                    member.setJobId(mgc.getJobId());
                    member.setLevel(mgc.getLevel());
                    this.broadcast(GuildPackets.guildMemberLevelJobUpdate(mgc));
                    break;
                }
            }
        } finally {
            membersLock.unlock();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof GuildCharacter o) {
            return (o.getId() == id && o.getName().equals(name));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 89 * hash + this.id;
        return hash;
    }

    public void changeRankTitle(String[] ranks) {
        System.arraycopy(ranks, 0, rankTitles, 0, 5);

        membersLock.lock();
        try {
            this.broadcast(GuildPackets.rankTitleChange(this.id, ranks));
        } finally {
            membersLock.unlock();
        }

        this.writeToDB(false);
    }

    public void disbandGuild() {
        if (allianceId > 0) {
            if (!Alliance.removeGuildFromAlliance(allianceId, id, world)) {
                Alliance.disbandAlliance(allianceId);
            }
        }

        membersLock.lock();
        try {
            this.writeToDB(true);
            this.broadcast(null, -1, BCOp.DISBAND);
        } finally {
            membersLock.unlock();
        }
    }

    public void setGuildEmblem(short bg, byte bgcolor, short logo, byte logocolor) {
        this.logoBG = bg;
        this.logoBGColor = bgcolor;
        this.logo = logo;
        this.logoColor = logocolor;
        this.writeToDB(false);

        membersLock.lock();
        try {
            this.broadcast(null, -1, BCOp.EMBLEMCHANGE);
        } finally {
            membersLock.unlock();
        }
    }

    public GuildCharacter getMGC(int cid) {
        membersLock.lock();
        try {
            for (GuildCharacter mgc : members) {
                if (mgc.getId() == cid) {
                    return mgc;
                }
            }
            return null;
        } finally {
            membersLock.unlock();
        }
    }

    public boolean increaseCapacity() {
        if (capacity > 99) {
            return false;
        }
        capacity += 5;
        this.writeToDB(false);

        membersLock.lock();
        try {
            this.broadcast(GuildPackets.guildCapacityChange(this.id, this.capacity));
        } finally {
            membersLock.unlock();
        }

        return true;
    }

    public void gainGP(int amount) {
        this.gp += amount;
        this.writeToDB(false);
        this.guildMessage(GuildPackets.updateGP(this.id, this.gp));
        this.guildMessage(PacketCreator.getGPMessage(amount));
    }

    public void removeGP(int amount) {
        this.gp -= amount;
        this.writeToDB(false);
        this.guildMessage(GuildPackets.updateGP(this.id, this.gp));
    }

    public static GuildResponse sendInvitation(Client c, String targetName) {
        Character mc = c.getChannelServer().getPlayerStorage().getCharacterByName(targetName);
        if (mc == null) {
            return GuildResponse.NOT_IN_CHANNEL;
        }
        if (mc.getGuildId() > 0) {
            return GuildResponse.ALREADY_IN_GUILD;
        }

        Character sender = c.getPlayer();
        if (InviteCoordinator.createInvite(InviteType.GUILD, sender, sender.getGuildId(), mc.getId())) {
            mc.sendPacket(GuildPackets.guildInvite(sender.getGuildId(), sender.getName()));
            return null;
        } else {
            return GuildResponse.MANAGING_INVITE;
        }
    }

    public static boolean answerInvitation(int targetId, String targetName, int guildId, boolean answer) {
        InviteResult res = InviteCoordinator.answerInvite(InviteType.GUILD, targetId, guildId, answer);

        GuildResponse mgr;
        Character sender = res.from;
        switch (res.result) {
            case ACCEPTED:
                return true;

            case DENIED:
                mgr = GuildResponse.DENIED_INVITE;
                break;

            default:
                mgr = GuildResponse.NOT_FOUND_INVITE;
        }

        if (mgr != null && sender != null) {
            sender.sendPacket(mgr.getPacket(targetName));
        }
        return false;
    }

    public static Set<Character> getEligiblePlayersForGuild(Character guildLeader) {
        Set<Character> guildMembers = new HashSet<>();
        guildMembers.add(guildLeader);

        MatchCheckerCoordinator mmce = guildLeader.getWorldServer().getMatchCheckerCoordinator();
        for (Character chr : guildLeader.getMap().getAllPlayers()) {
            if (chr.getParty() == null && chr.getGuild() == null && mmce.getMatchConfirmationLeaderid(chr.getId()) == -1) {
                guildMembers.add(chr);
            }
        }

        return guildMembers;
    }

    public static void displayGuildRanks(Client c, int npcid) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT `name`, `GP`, `logoBG`, `logoBGColor`, `logo`, `logoColor` FROM guilds ORDER BY `GP` DESC LIMIT 50", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = ps.executeQuery()) {
            c.sendPacket(GuildPackets.showGuildRanks(npcid, rs));
        } catch (SQLException e) {
            log.error("Failed to display guild ranks.", e);
        }
    }

    public int getAllianceId() {
        return allianceId;
    }

    public void setAllianceId(int aid) {
        this.allianceId = aid;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE guilds SET allianceId = ? WHERE guildid = ?")) {
            ps.setInt(1, aid);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void resetAllianceGuildPlayersRank() {
        try {
            membersLock.lock();
            try {
                for (GuildCharacter mgc : members) {
                    if (mgc.isOnline()) {
                        mgc.setAllianceRank(5);
                    }
                }
            } finally {
                membersLock.unlock();
            }

            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("UPDATE characters SET allianceRank = ? WHERE guildid = ?")) {
                ps.setInt(1, 5);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int getIncreaseGuildCost(int size) {
        int cost = YamlConfig.config.server.EXPAND_GUILD_BASE_COST + Math.max(0, (size - 15) / 5) * YamlConfig.config.server.EXPAND_GUILD_TIER_COST;

        if (size > 30) {
            return Math.min(YamlConfig.config.server.EXPAND_GUILD_MAX_COST, Math.max(cost, 5000000));
        } else {
            return cost;
        }
    }
}
