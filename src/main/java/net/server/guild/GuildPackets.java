package net.server.guild;

import client.Character;
import client.Client;
import net.opcodes.SendOpcode;
import net.packet.OutPacket;
import net.packet.Packet;
import net.server.Server;
import tools.PacketCreator;
import tools.Pair;
import tools.StringUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public class GuildPackets {
    public static Packet showGuildInfo(Character chr) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
        p.writeByte(0x1A); // signature for showing guild info
        if (chr == null) { // show empty guild (used for leaving, expelled)
            p.writeByte(0);
            return p;
        }
        Guild g = chr.getClient().getWorldServer().getGuild(chr.getMGC());
        if (g == null) { // failed to read from DB - don't show a guild
            p.writeByte(0);
            return p;
        }
        p.writeByte(1); // bInGuild
        p.writeInt(g.getId());
        p.writeString(g.getName());
        for (int i = 1; i <= 5; i++) {
            p.writeString(g.getRankTitle(i));
        }
        Collection<GuildCharacter> members = g.getMembers();
        p.writeByte(members.size()); // then it is the size of all the members
        for (GuildCharacter mgc : members) {// and each of their character ids o_O
            p.writeInt(mgc.getId());
        }
        for (GuildCharacter mgc : members) {
            p.writeFixedString(StringUtil.getRightPaddedStr(mgc.getName(), '\0', 13));
            p.writeInt(mgc.getJobId());
            p.writeInt(mgc.getLevel());
            p.writeInt(mgc.getGuildRank());
            p.writeInt(mgc.isOnline() ? 1 : 0);
            p.writeInt(g.getSignature());
            p.writeInt(mgc.getAllianceRank());
        }
        p.writeInt(g.getCapacity());
        p.writeShort(g.getLogoBG());
        p.writeByte(g.getLogoBGColor());
        p.writeShort(g.getLogo());
        p.writeByte(g.getLogoColor());
        p.writeString(g.getNotice());
        p.writeInt(g.getGP());
        p.writeInt(g.getAllianceId());
        return p;
    }

    public static Packet guildMemberOnline(int guildId, int chrId, boolean bOnline) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
        p.writeByte(0x3d);
        p.writeInt(guildId);
        p.writeInt(chrId);
        p.writeBool(bOnline);
        return p;
    }

    public static Packet guildInvite(int guildId, String charName) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
        p.writeByte(0x05);
        p.writeInt(guildId);
        p.writeString(charName);
        return p;
    }

    public static Packet createGuildMessage(String masterName, String guildName) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
        p.writeByte(0x3);
        p.writeInt(0);
        p.writeString(masterName);
        p.writeString(guildName);
        return p;
    }

    /**
     * Gets a Heracle/guild message packet.
     * <p>
     * Possible values for <code>code</code>:<br>
     * 28: guild name already in use<br>
     * 31: problem in locating players during agreement<br>
     * 33/40: already joined a guild<br>
     * 35: Cannot make guild<br>
     * 36: problem in player agreement<br>
     * 38: problem during forming guild<br>
     * 41: max number of players in joining guild<br>
     * 42: character can't be found this channel<br>
     * 45/48: character not in guild<br>
     * 52: problem in disbanding guild<br>
     * 56: admin cannot make guild<br>
     * 57: problem in increasing guild size<br>
     *
     * @param code The response code.
     * @return The guild message packet.
     */
    public static Packet genericGuildMessage(byte code) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
        p.writeByte(code);
        return p;
    }

    /**
     * Gets a guild message packet appended with target name.
     * <p>
     * 53: player not accepting guild invites<br>
     * 54: player already managing an invite<br>
     * 55: player denied an invite<br>
     *
     * @param code       The response code.
     * @param targetName The initial player target of the invitation.
     * @return The guild message packet.
     */
    public static Packet responseGuildMessage(byte code, String targetName) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
        p.writeByte(code);
        p.writeString(targetName);
        return p;
    }

    public static Packet newGuildMember(GuildCharacter mgc) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
        p.writeByte(0x27);
        p.writeInt(mgc.getGuildId());
        p.writeInt(mgc.getId());
        p.writeFixedString(StringUtil.getRightPaddedStr(mgc.getName(), '\0', 13));
        p.writeInt(mgc.getJobId());
        p.writeInt(mgc.getLevel());
        p.writeInt(mgc.getGuildRank()); // should be always 5 but whatevs
        p.writeInt(mgc.isOnline() ? 1 : 0); // should always be 1 too
        p.writeInt(1); // ? could be guild signature, but doesn't seem to matter
        p.writeInt(3);
        return p;
    }

    // someone leaving, mode == 0x2c for leaving, 0x2f for expelled
    public static Packet memberLeft(GuildCharacter mgc, boolean bExpelled) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
        p.writeByte(bExpelled ? 0x2f : 0x2c);
        p.writeInt(mgc.getGuildId());
        p.writeInt(mgc.getId());
        p.writeString(mgc.getName());
        return p;
    }

    // rank change
    public static Packet changeRank(GuildCharacter mgc) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
        p.writeByte(0x40);
        p.writeInt(mgc.getGuildId());
        p.writeInt(mgc.getId());
        p.writeByte(mgc.getGuildRank());
        return p;
    }

    public static Packet guildNotice(int guildId, String notice) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
        p.writeByte(0x44);
        p.writeInt(guildId);
        p.writeString(notice);
        return p;
    }

    public static Packet guildMemberLevelJobUpdate(GuildCharacter mgc) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
        p.writeByte(0x3C);
        p.writeInt(mgc.getGuildId());
        p.writeInt(mgc.getId());
        p.writeInt(mgc.getLevel());
        p.writeInt(mgc.getJobId());
        return p;
    }

    public static Packet rankTitleChange(int guildId, String[] ranks) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
        p.writeByte(0x3E);
        p.writeInt(guildId);
        for (int i = 0; i < 5; i++) {
            p.writeString(ranks[i]);
        }
        return p;
    }

    public static Packet guildDisband(int guildId) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
        p.writeByte(0x32);
        p.writeInt(guildId);
        p.writeByte(1);
        return p;
    }

    public static Packet guildQuestWaitingNotice(byte channel, int waitingPos) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
        p.writeByte(0x4C);
        p.writeByte(channel - 1);
        p.writeByte(waitingPos);
        return p;
    }

    public static Packet guildEmblemChange(int guildId, short bg, byte bgcolor, short logo, byte logoColor) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
        p.writeByte(0x42);
        p.writeInt(guildId);
        p.writeShort(bg);
        p.writeByte(bgcolor);
        p.writeShort(logo);
        p.writeByte(logoColor);
        return p;
    }

    public static Packet guildCapacityChange(int guildId, int capacity) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
        p.writeByte(0x3A);
        p.writeInt(guildId);
        p.writeByte(capacity);
        return p;
    }

    public static void addThread(final OutPacket p, ResultSet rs) throws SQLException {
        p.writeInt(rs.getInt("localthreadid"));
        p.writeInt(rs.getInt("postercid"));
        p.writeString(rs.getString("name"));
        p.writeLong(PacketCreator.getTime(rs.getLong("timestamp")));
        p.writeInt(rs.getInt("icon"));
        p.writeInt(rs.getInt("replycount"));
    }

    public static Packet BBSThreadList(ResultSet rs, int start) throws SQLException {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_BBS_PACKET);
        p.writeByte(0x06);
        if (!rs.last()) {
            p.writeByte(0);
            p.writeInt(0);
            p.writeInt(0);
            return p;
        }
        int threadCount = rs.getRow();
        if (rs.getInt("localthreadid") == 0) { // has a notice
            p.writeByte(1);
            addThread(p, rs);
            threadCount--; // one thread didn't count (because it's a notice)
        } else {
            p.writeByte(0);
        }
        if (!rs.absolute(start + 1)) { // seek to the thread before where we start
            rs.first(); // uh, we're trying to start at a place past possible
            start = 0;
        }
        p.writeInt(threadCount);
        p.writeInt(Math.min(10, threadCount - start));
        for (int i = 0; i < Math.min(10, threadCount - start); i++) {
            addThread(p, rs);
            rs.next();
        }
        return p;
    }

    public static Packet showThread(int localthreadid, ResultSet threadRS, ResultSet repliesRS)
            throws SQLException, RuntimeException {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_BBS_PACKET);
        p.writeByte(0x07);
        p.writeInt(localthreadid);
        p.writeInt(threadRS.getInt("postercid"));
        p.writeLong(PacketCreator.getTime(threadRS.getLong("timestamp")));
        p.writeString(threadRS.getString("name"));
        p.writeString(threadRS.getString("startpost"));
        p.writeInt(threadRS.getInt("icon"));
        if (repliesRS != null) {
            int replyCount = threadRS.getInt("replycount");
            p.writeInt(replyCount);
            int i;
            for (i = 0; i < replyCount && repliesRS.next(); i++) {
                p.writeInt(repliesRS.getInt("replyid"));
                p.writeInt(repliesRS.getInt("postercid"));
                p.writeLong(PacketCreator.getTime(repliesRS.getLong("timestamp")));
                p.writeString(repliesRS.getString("content"));
            }
            if (i != replyCount || repliesRS.next()) {
                throw new RuntimeException(String.valueOf(threadRS.getInt("threadid")));
            }
        } else {
            p.writeInt(0);
        }
        return p;
    }

    public static Packet showGuildRanks(int npcid, ResultSet rs) throws SQLException {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
        p.writeByte(0x49);
        p.writeInt(npcid);
        if (!rs.last()) { // no guilds o.o
            p.writeInt(0);
            return p;
        }
        p.writeInt(rs.getRow()); // number of entries
        rs.beforeFirst();
        while (rs.next()) {
            p.writeString(rs.getString("name"));
            p.writeInt(rs.getInt("GP"));
            p.writeInt(rs.getInt("logo"));
            p.writeInt(rs.getInt("logoColor"));
            p.writeInt(rs.getInt("logoBG"));
            p.writeInt(rs.getInt("logoBGColor"));
        }
        return p;
    }

    public static Packet showPlayerRanks(int npcid, List<Pair<String, Integer>> worldRanking) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
        p.writeByte(0x49);
        p.writeInt(npcid);
        if (worldRanking.isEmpty()) {
            p.writeInt(0);
            return p;
        }
        p.writeInt(worldRanking.size());
        for (Pair<String, Integer> wr : worldRanking) {
            p.writeString(wr.getLeft());
            p.writeInt(wr.getRight()); // This now dynamically holds 'reborns' as passed by Server.java
            p.writeInt(0);
            p.writeInt(0);
            p.writeInt(0);
            p.writeInt(0);
        }
        return p;
    }

    public static Packet updateGP(int guildId, int GP) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_OPERATION);
        p.writeByte(0x48);
        p.writeInt(guildId);
        p.writeInt(GP);
        return p;
    }

    public static void getGuildInfo(OutPacket p, Guild guild) {
        p.writeInt(guild.getId());
        p.writeString(guild.getName());
        for (int i = 1; i <= 5; i++) {
            p.writeString(guild.getRankTitle(i));
        }
        Collection<GuildCharacter> members = guild.getMembers();
        p.writeByte(members.size());
        for (GuildCharacter mgc : members) {
            p.writeInt(mgc.getId());
        }
        for (GuildCharacter mgc : members) {
            p.writeFixedString(StringUtil.getRightPaddedStr(mgc.getName(), '\0', 13));
            p.writeInt(mgc.getJobId());
            p.writeInt(mgc.getLevel());
            p.writeInt(mgc.getGuildRank());
            p.writeInt(mgc.isOnline() ? 1 : 0);
            p.writeInt(guild.getSignature());
            p.writeInt(mgc.getAllianceRank());
        }
        p.writeInt(guild.getCapacity());
        p.writeShort(guild.getLogoBG());
        p.writeByte(guild.getLogoBGColor());
        p.writeShort(guild.getLogo());
        p.writeByte(guild.getLogoColor());
        p.writeString(guild.getNotice());
        p.writeInt(guild.getGP());
        p.writeInt(guild.getAllianceId());
    }

    public static Packet getAllianceInfo(Alliance alliance) {
        OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
        p.writeByte(0x0C);
        p.writeByte(1);
        p.writeInt(alliance.getId());
        p.writeString(alliance.getName());
        for (int i = 1; i <= 5; i++) {
            p.writeString(alliance.getRankTitle(i));
        }
        p.writeByte(alliance.getGuilds().size());
        p.writeInt(alliance.getCapacity()); // probably capacity
        for (Integer guild : alliance.getGuilds()) {
            p.writeInt(guild);
        }
        p.writeString(alliance.getNotice());
        return p;
    }

    public static Packet updateAllianceInfo(Alliance alliance, int world) {
        OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
        p.writeByte(0x0F);
        p.writeInt(alliance.getId());
        p.writeString(alliance.getName());
        for (int i = 1; i <= 5; i++) {
            p.writeString(alliance.getRankTitle(i));
        }
        p.writeByte(alliance.getGuilds().size());
        for (Integer guild : alliance.getGuilds()) {
            p.writeInt(guild);
        }
        p.writeInt(alliance.getCapacity()); // probably capacity
        p.writeShort(0);
        for (Integer guildid : alliance.getGuilds()) {
            getGuildInfo(p, Server.getInstance().getGuild(guildid, world));
        }
        return p;
    }

    public static Packet getGuildAlliances(Alliance alliance, int worldId) {
        OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
        p.writeByte(0x0D);
        p.writeInt(alliance.getGuilds().size());
        for (Integer guild : alliance.getGuilds()) {
            getGuildInfo(p, Server.getInstance().getGuild(guild, worldId));
        }
        return p;
    }

    public static Packet addGuildToAlliance(Alliance alliance, int newGuild, Client c) {
        OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
        p.writeByte(0x12);
        p.writeInt(alliance.getId());
        p.writeString(alliance.getName());
        for (int i = 1; i <= 5; i++) {
            p.writeString(alliance.getRankTitle(i));
        }
        p.writeByte(alliance.getGuilds().size());
        for (Integer guild : alliance.getGuilds()) {
            p.writeInt(guild);
        }
        p.writeInt(alliance.getCapacity());
        p.writeString(alliance.getNotice());
        p.writeInt(newGuild);
        getGuildInfo(p, Server.getInstance().getGuild(newGuild, c.getWorld(), null));
        return p;
    }

    public static Packet allianceMemberOnline(Character mc, boolean online) {
        OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
        p.writeByte(0x0E);
        p.writeInt(mc.getGuild().getAllianceId());
        p.writeInt(mc.getGuildId());
        p.writeInt(mc.getId());
        p.writeBool(online);
        return p;
    }

    public static Packet allianceNotice(int id, String notice) {
        OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
        p.writeByte(0x1C);
        p.writeInt(id);
        p.writeString(notice);
        return p;
    }

    public static Packet changeAllianceRankTitle(int alliance, String[] ranks) {
        OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
        p.writeByte(0x1A);
        p.writeInt(alliance);
        for (int i = 0; i < 5; i++) {
            p.writeString(ranks[i]);
        }
        return p;
    }

    public static Packet updateAllianceJobLevel(Character mc) {
        OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
        p.writeByte(0x18);
        p.writeInt(mc.getGuild().getAllianceId());
        p.writeInt(mc.getGuildId());
        p.writeInt(mc.getId());
        p.writeInt(mc.getLevel());
        p.writeInt(mc.getJob().getId());
        return p;
    }

    public static Packet removeGuildFromAlliance(Alliance alliance, int expelledGuild, int worldId) {
        OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
        p.writeByte(0x10);
        p.writeInt(alliance.getId());
        p.writeString(alliance.getName());
        for (int i = 1; i <= 5; i++) {
            p.writeString(alliance.getRankTitle(i));
        }
        p.writeByte(alliance.getGuilds().size());
        for (Integer guild : alliance.getGuilds()) {
            p.writeInt(guild);
        }
        p.writeInt(alliance.getCapacity());
        p.writeString(alliance.getNotice());
        p.writeInt(expelledGuild);
        getGuildInfo(p, Server.getInstance().getGuild(expelledGuild, worldId, null));
        p.writeByte(0x01);
        return p;
    }

    public static Packet disbandAlliance(int alliance) {
        OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
        p.writeByte(0x1D);
        p.writeInt(alliance);
        return p;
    }

    public static Packet allianceInvite(int allianceid, Character chr) {
        OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
        p.writeByte(0x03);
        p.writeInt(allianceid);
        p.writeString(chr.getName());
        p.writeShort(0);
        return p;
    }

    public static Packet GuildBoss_HealerMove(short nY) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_BOSS_HEALER_MOVE);
        p.writeShort(nY); // New Y Position
        return p;
    }

    public static Packet GuildBoss_PulleyStateChange(byte nState) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_BOSS_PULLEY_STATE_CHANGE);
        p.writeByte(nState);
        return p;
    }

    /**
     * Guild Name & Mark update packet, thanks to Arnah (Vertisy)
     *
     * @param guildName The Guild name, blank for nothing.
     */
    public static Packet guildNameChanged(int chrid, String guildName) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_NAME_CHANGED);
        p.writeInt(chrid);
        p.writeString(guildName);
        return p;
    }

    public static Packet guildMarkChanged(int chrId, Guild guild) {
        OutPacket p = OutPacket.create(SendOpcode.GUILD_MARK_CHANGED);
        p.writeInt(chrId);
        p.writeShort(guild.getLogoBG());
        p.writeByte(guild.getLogoBGColor());
        p.writeShort(guild.getLogo());
        p.writeByte(guild.getLogoColor());
        return p;
    }

    public static Packet sendShowInfo(int allianceid, int playerid) {
        OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
        p.writeByte(0x02);
        p.writeInt(allianceid);
        p.writeInt(playerid);
        return p;
    }

    public static Packet sendInvitation(int allianceid, int playerid, final String guildname) {
        OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
        p.writeByte(0x05);
        p.writeInt(allianceid);
        p.writeInt(playerid);
        p.writeString(guildname);
        return p;
    }

    public static Packet sendChangeGuild(int allianceid, int playerid, int guildid, int option) {
        OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
        p.writeByte(0x07);
        p.writeInt(allianceid);
        p.writeInt(guildid);
        p.writeInt(playerid);
        p.writeByte(option);
        return p;
    }

    public static Packet sendChangeLeader(int allianceid, int playerid, int victim) {
        OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
        p.writeByte(0x08);
        p.writeInt(allianceid);
        p.writeInt(playerid);
        p.writeInt(victim);
        return p;
    }

    public static Packet sendChangeRank(int allianceid, int playerid, int int1, byte byte1) {
        OutPacket p = OutPacket.create(SendOpcode.ALLIANCE_OPERATION);
        p.writeByte(0x09);
        p.writeInt(allianceid);
        p.writeInt(playerid);
        p.writeInt(int1);
        p.writeInt(byte1);
        return p;
    }
}
