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
package scripting.npc;

import client.Character;
import client.*;
import client.inventory.*;
import client.inventory.manipulator.InventoryManipulator;
import client.keybind.KeyBinding;
import client.creator.MakeCharInfoValidator;
import config.YamlConfig;
import constants.game.GameConstants;
import constants.id.MapId;
import constants.id.NpcId;
import constants.inventory.ItemConstants;
import constants.string.LanguageConstants;
import net.server.Server;
import net.server.channel.Channel;
import net.server.coordinator.matchchecker.MatchCheckerListenerFactory.MatchCheckerType;
import net.server.guild.Alliance;
import net.server.guild.Guild;
import net.server.guild.GuildPackets;
import net.server.world.Party;
import net.server.world.PartyCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import provider.Data;
import provider.DataProviderFactory;
import provider.wz.WZFiles;
import scripting.AbstractPlayerInteraction;
import server.*;
import server.SkillbookInformationProvider.SkillBookEntry;
import server.events.gm.Event;
import server.expeditions.Expedition;
import server.expeditions.ExpeditionType;
import server.gachapon.Gachapon;
import server.gachapon.Gachapon.GachaponItem;
import server.life.*;
import server.maps.*;
import server.partyquest.AriantColiseum;
import server.partyquest.MonsterCarnival;
import server.partyquest.Pyramid;
import server.partyquest.Pyramid.PyramidMode;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.packets.WeddingPackets;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author Matze
 */
public class NPCConversationManager extends AbstractPlayerInteraction {
    private static final Logger log = LoggerFactory.getLogger(NPCConversationManager.class);

    private final int npc;
    private int npcOid;
    private String scriptName;
    private String getText;
    private boolean itemScript;
    private List<PartyCharacter> otherParty;

    private final Map<Integer, String> npcDefaultTalks = new HashMap<>();

    private String getDefaultTalk(int npcid) {
        String talk = npcDefaultTalks.get(npcid);
        if (talk == null) {
            talk = LifeFactory.getNPCDefaultTalk(npcid);
            npcDefaultTalks.put(npcid, talk);
        }

        return talk;
    }

    public NPCConversationManager(Client c, int npc, String scriptName) {
        this(c, npc, -1, scriptName, false);
    }

    public NPCConversationManager(Client c, int npc, List<PartyCharacter> otherParty, boolean test) {
        super(c);
        this.c = c;
        this.npc = npc;
        this.otherParty = otherParty;
    }

    public NPCConversationManager(Client c, int npc, int oid, String scriptName, boolean itemScript) {
        super(c);
        this.npc = npc;
        this.npcOid = oid;
        this.scriptName = scriptName;
        this.itemScript = itemScript;
    }

    public int getNpc() {
        return npc;
    }

    public int getNpcObjectId() {
        return npcOid;
    }

    public String getScriptName() {
        return scriptName;
    }

    public boolean isItemScript() {
        return itemScript;
    }

    public void resetItemScript() {
        this.itemScript = false;
    }


    public void dispose() {
        NPCScriptManager.getInstance().dispose(this);
        getClient().sendPacket(PacketCreator.enableActions());
    }

    public void sendNext(String text) {
        getClient().sendPacket(PacketCreator.getNPCTalk(npc, (byte) 0, text, "00 01", (byte) 0));
    }

    public void sendPrev(String text) {
        getClient().sendPacket(PacketCreator.getNPCTalk(npc, (byte) 0, text, "01 00", (byte) 0));
    }

    public void sendNextPrev(String text) {
        getClient().sendPacket(PacketCreator.getNPCTalk(npc, (byte) 0, text, "01 01", (byte) 0));
    }

    public void sendOk(String text) {
        getClient().sendPacket(PacketCreator.getNPCTalk(npc, (byte) 0, text, "00 00", (byte) 0));
    }

    public void sendDefault() {
        sendOk(getDefaultTalk(npc));
    }

    public void sendYesNo(String text) {
        getClient().sendPacket(PacketCreator.getNPCTalk(npc, (byte) 1, text, "", (byte) 0));
    }

    public void sendYesNo(String text, int npcId) {
        getClient().sendPacket(PacketCreator.getNPCTalk(npcId, (byte) 1, text, "", (byte) 0));
    }

    public void sendAcceptDecline(String text) {
        getClient().sendPacket(PacketCreator.getNPCTalk(npc, (byte) 0x0C, text, "", (byte) 0));
    }

    public void sendSimple(String text) {
        getClient().sendPacket(PacketCreator.getNPCTalk(npc, (byte) 4, text, "", (byte) 0));
    }

    public void sendNext(String text, byte speaker) {
        getClient().sendPacket(PacketCreator.getNPCTalk(npc, (byte) 0, text, "00 01", speaker));
    }

    public void sendPrev(String text, byte speaker) {
        getClient().sendPacket(PacketCreator.getNPCTalk(npc, (byte) 0, text, "01 00", speaker));
    }

    public void sendNextPrev(String text, byte speaker) {
        getClient().sendPacket(PacketCreator.getNPCTalk(npc, (byte) 0, text, "01 01", speaker));
    }

    public void sendOk(String text, byte speaker) {
        getClient().sendPacket(PacketCreator.getNPCTalk(npc, (byte) 0, text, "00 00", speaker));
    }

    public void sendYesNo(String text, byte speaker) {
        getClient().sendPacket(PacketCreator.getNPCTalk(npc, (byte) 1, text, "", speaker));
    }

    public void sendAcceptDecline(String text, byte speaker) {
        getClient().sendPacket(PacketCreator.getNPCTalk(npc, (byte) 0x0C, text, "", speaker));
    }

    public void sendSimple(String text, byte speaker) {
        getClient().sendPacket(PacketCreator.getNPCTalk(npc, (byte) 4, text, "", speaker));
    }

    public void sendStyle(String text, int[] styles) {
        if (styles.length > 0) {
            getClient().sendPacket(PacketCreator.getNPCTalkStyle(npc, text, styles));
        } else {    // thanks Conrad for noticing empty styles crashing players
            sendOk("Sorry, there are no options of cosmetics available for you here at the moment.");
            dispose();
        }
    }

    public void sendGetNumber(String text, int def, int min, int max) {
        getClient().sendPacket(PacketCreator.getNPCTalkNum(npc, text, def, min, max));
    }

    public void sendGetText(String text) {
        getClient().sendPacket(PacketCreator.getNPCTalkText(npc, text, ""));
    }

    /*
     * 0 = ariant colliseum
     * 1 = Dojo
     * 2 = Carnival 1
     * 3 = Carnival 2
     * 4 = Ghost Ship PQ?
     * 5 = Pyramid PQ
     * 6 = Kerning Subway
     */
    public void sendDimensionalMirror(String text) {
        getClient().sendPacket(PacketCreator.getDimensionalMirror(text));
    }

    public void setGetText(String text) {
        this.getText = text;
    }

    public String getText() {
        return this.getText;
    }

    @Override
    public boolean forceStartQuest(int id) {
        return forceStartQuest(id, npc);
    }

    @Override
    public boolean forceCompleteQuest(int id) {
        return forceCompleteQuest(id, npc);
    }

    @Override
    public boolean startQuest(short id) {
        return startQuest((int) id);
    }

    @Override
    public boolean completeQuest(short id) {
        return completeQuest((int) id);
    }

    @Override
    public boolean startQuest(int id) {
        return startQuest(id, npc);
    }

    @Override
    public boolean completeQuest(int id) {
        return completeQuest(id, npc);
    }

    public int getMeso() {
        return getPlayer().getMeso();
    }

    public void gainMeso(int gain) {
        getPlayer().gainMeso(gain);
    }

    public void gainMeso(int gain, boolean show) {
        getPlayer().gainMeso(gain, show);
    }

    public void gainExp(int gain) {
        getPlayer().gainExp(gain, true, true);
    }


    @Override
    public void showEffect(String effect) {
        getPlayer().getMap().broadcastMessage(PacketCreator.environmentChange(effect, 3));
    }

    public void setHair(int hair) {
        getPlayer().setHair(hair);
        getPlayer().updateSingleStat(Stat.HAIR, hair);
        getPlayer().equipChanged();
    }

    public void setFace(int face) {
        getPlayer().setFace(face);
        getPlayer().updateSingleStat(Stat.FACE, face);
        getPlayer().equipChanged();
    }

    public void setSkin(int color) {
        getPlayer().setSkinColor(SkinColor.getById(color));
        getPlayer().updateSingleStat(Stat.SKIN, color);
        getPlayer().equipChanged();
    }

    public int[] getAllHairIds() {
        return MakeCharInfoValidator.getValidHairIds()
                .stream()
                .mapToInt(Integer::intValue)
                .sorted()
                .toArray();
    }

    public int[] getAllFaceIds() {
        return MakeCharInfoValidator.getValidFaceIds()
                .stream()
                .mapToInt(Integer::intValue)
                .sorted()
                .toArray();
    }

    public int[] getAllHairColorIds() {
        return MakeCharInfoValidator.getValidHairColors()
                .stream()
                .mapToInt(Integer::intValue)
                .sorted()
                .toArray();
    }

    public int[] getAllSkinIds() {
        return MakeCharInfoValidator.getValidSkinIds()
                .stream()
                .mapToInt(Integer::intValue)
                .sorted()
                .toArray();
    }
    public int itemQuantity(int itemid) {
        return getPlayer().getInventory(ItemConstants.getInventoryType(itemid)).countById(itemid);
    }

    public void displayGuildRanks() {
        Guild.displayGuildRanks(getClient(), npc);
    }

    public boolean canSpawnPlayerNpc(int mapid) {
        Character chr = getPlayer();
        return !YamlConfig.config.server.PLAYERNPC_AUTODEPLOY && chr.getLevel() >= chr.getMaxClassLevel() && !chr.isGM() && PlayerNPC.canSpawnPlayerNpc(chr.getName(), mapid);
    }

    public PlayerNPC getPlayerNPCByScriptid(int scriptId) {
        for (MapObject pnpcObj : getPlayer().getMap().getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.PLAYER_NPC))) {
            PlayerNPC pn = (PlayerNPC) pnpcObj;

            if (pn.getScriptId() == scriptId) {
                return pn;
            }
        }

        return null;
    }

    @Override
    public Party getParty() {
        return getPlayer().getParty();
    }

    @Override
    public void resetMap(int mapid) {
        getClient().getChannelServer().getMapFactory().getMap(mapid).resetReactors();
    }

    public void gainTameness(int tameness) {
        for (Pet pet : getPlayer().getPets()) {
            if (pet != null) {
                pet.gainTamenessFullness(getPlayer(), tameness, 0, 0);
            }
        }
    }

    public String getName() {
        return getPlayer().getName();
    }

    public int getGender() {
        return getPlayer().getGender();
    }

    public int getGiftLogCerezeth(String giftid) {
        return getPlayer().getGiftLogCerezeth(giftid);
    }

    public void setGiftLogCerezeth(String giftid) {
        getPlayer().setGiftLogCerezeth(giftid);
    }

    public void changeRebirthJobCerezeth(int jobid) {
        getPlayer().changeRebirthJobCerezeth(Job.getById(jobid));
    }

    public void changeJobById(int a) {
        getPlayer().changeJob(Job.getById(a));
    }

    public void changeJob(Job job) {
        getPlayer().changeJob(job);
    }

    public String getJobName(int id) {
        return GameConstants.getJobName(id);
    }

    public StatEffect getItemEffect(int itemId) {
        return ItemInformationProvider.getInstance().getItemEffect(itemId);
    }

    public void resetStats() {
        getPlayer().resetStats();
    }

    public void openShopNPC(int id) {
        Shop shop = ShopFactory.getInstance().getShop(id);

        if (shop != null) {
            shop.sendShop(c);
        } else {    // check for missing shopids thanks to resinate
            log.warn("Shop ID: {} is missing from database.", id);
            ShopFactory.getInstance().getShop(11000).sendShop(c);
        }
    }

    public void maxMastery() {
        for (Data skill_ : DataProviderFactory.getDataProvider(WZFiles.STRING).getData("Skill.img").getChildren()) {
            try {
                Skill skill = SkillFactory.getSkill(Integer.parseInt(skill_.getName()));
                getPlayer().changeSkillLevel(skill, (byte) 0, skill.getMaxLevel(), -1);
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
                break;
            } catch (NullPointerException npe) {
                npe.printStackTrace();
                continue;
            }
        }
    }

    public Object[] getSkillsByJob(int job) {
        List<Integer> skills = new LinkedList<Integer>();
        // for (MapleData skill_ : MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String.wz")).getData("Skill.img").getChildren()) {
        for (Data skill_ : DataProviderFactory.getDataProvider(WZFiles.STRING).getData("Skill.img").getChildren()) {
            try {
                Skill skill = SkillFactory.getSkill(Integer.parseInt(skill_.getName()));
                if (skill.getId() / 10000 == job) {
                    skills.add(skill.getId());
                }
            } catch (NumberFormatException nfe) {
                break;
            } catch (NullPointerException npe) {
                continue;
            }
        }
        return skills.toArray();
    }

    public String getSkillDesc(int skillid) {
        return SkillFactory.getSkillDesc(skillid).replaceAll("#c", "").replaceAll("#", "");
    }

    public void changeKeyBinding(int key, int type, int action) {
        getPlayer().changeKeybinding(key, new KeyBinding(type, action));
        getPlayer().sendKeymap();
    }

    public void doGachapon() {
        GachaponItem item = Gachapon.getInstance().process(npc);
        Item itemGained = gainItem(item.getId(), (short) (item.getId() / 10000 == 200 ? 100 : 1), true, true); // For normal potions, make it give 100.

        sendNext("You have obtained a #b#t" + item.getId() + "##k.");

        int[] maps = {MapId.HENESYS, MapId.ELLINIA, MapId.PERION, MapId.KERNING_CITY, MapId.SLEEPYWOOD, MapId.MUSHROOM_SHRINE,
                MapId.SHOWA_SPA_M, MapId.SHOWA_SPA_F, MapId.NEW_LEAF_CITY, MapId.NAUTILUS_HARBOR};
        final int mapId = maps[(getNpc() != NpcId.GACHAPON_NAUTILUS && getNpc() != NpcId.GACHAPON_NLC) ?
                (getNpc() - NpcId.GACHAPON_HENESYS) : getNpc() == NpcId.GACHAPON_NLC ? 8 : 9];
        String map = c.getChannelServer().getMapFactory().getMap(mapId).getMapName();

        Gachapon.log(getPlayer(), item.getId(), map);

        if (item.getTier() > 0) { //Uncommon and Rare
            Server.getInstance().broadcastMessage(c.getWorld(), PacketCreator.gachaponMessage(itemGained, map, getPlayer()));
        }
    }

    public void upgradeAlliance() {
        Alliance alliance = Server.getInstance().getAlliance(c.getPlayer().getGuild().getAllianceId());
        alliance.increaseCapacity(1);

        Server.getInstance().allianceMessage(alliance.getId(), GuildPackets.getGuildAlliances(alliance, c.getWorld()), -1, -1);
        Server.getInstance().allianceMessage(alliance.getId(), GuildPackets.allianceNotice(alliance.getId(), alliance.getNotice()), -1, -1);

        c.sendPacket(GuildPackets.updateAllianceInfo(alliance, c.getWorld()));  // thanks Vcoc for finding an alliance update to leader issue
    }

    public void disbandAlliance(Client c, int allianceId) {
        Alliance.disbandAlliance(allianceId);
    }

    public boolean canBeUsedAllianceName(String name) {
        return Alliance.canBeUsedAllianceName(name);
    }

    public Alliance createAlliance(String name) {
        return Alliance.createAlliance(getParty(), name);
    }

    public int getAllianceCapacity() {
        return Server.getInstance().getAlliance(getPlayer().getGuild().getAllianceId()).getCapacity();
    }

    public boolean hasMerchant() {
        return getPlayer().hasMerchant();
    }

    public boolean hasMerchantItems() {
        try {
            if (!ItemFactory.MERCHANT.loadItems(getPlayer().getId(), false).isEmpty()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return getPlayer().getMerchantMeso() != 0;
    }

    public void showFredrick() {
        c.sendPacket(PacketCreator.getFredrick(getPlayer()));
    }

    public int partyMembersInMap() {
        int inMap = 0;
        for (Character char2 : getPlayer().getMap().getCharacters()) {
            if (char2.getParty() == getPlayer().getParty()) {
                inMap++;
            }
        }
        return inMap;
    }

    public Event getEvent() {
        return c.getChannelServer().getEvent();
    }

    public void divideTeams() {
        if (getEvent() != null) {
            getPlayer().setTeam(getEvent().getLimit() % 2); //muhaha :D
        }
    }

    public Character getMapleCharacter(String player) {
        Character target = Server.getInstance().getWorld(c.getWorld()).getChannel(c.getChannel()).getPlayerStorage().getCharacterByName(player);
        return target;
    }

    public void logLeaf(String prize) {
        MapleLeafLogger.log(getPlayer(), true, prize);
    }

    public boolean createPyramid(String mode, boolean party) {//lol
        PyramidMode mod = PyramidMode.valueOf(mode);

        Party partyz = getPlayer().getParty();
        MapManager mapManager = c.getChannelServer().getMapFactory();

        MapleMap map = null;
        int mapid = MapId.NETTS_PYRAMID_SOLO_BASE;
        if (party) {
            mapid += 10000;
        }
        mapid += (mod.getMode() * 1000);

        for (byte b = 0; b < 5; b++) {//They cannot warp to the next map before the timer ends (:
            map = mapManager.getMap(mapid + b);
            if (map.getCharacters().size() > 0) {
                continue;
            } else {
                break;
            }
        }

        if (map == null) {
            return false;
        }

        if (!party) {
            partyz = new Party(-1, new PartyCharacter(getPlayer()));
        }
        Pyramid py = new Pyramid(partyz, mod, map.getId());
        getPlayer().setPartyQuest(py);
        py.warp(mapid);
        dispose();
        return true;
    }

    public boolean itemExists(int itemid) {
        return ItemInformationProvider.getInstance().getName(itemid) != null;
    }

    public int getCosmeticItem(int itemid) {
        if (itemExists(itemid)) {
            return itemid;
        }

        int baseid;
        if (itemid < 30000) {
            baseid = (itemid / 1000) * 1000 + (itemid % 100);
        } else {
            baseid = (itemid / 10) * 10;
        }

        return itemid != baseid && itemExists(baseid) ? baseid : -1;
    }

    private int getEquippedCosmeticid(int itemid) {
        if (itemid < 30000) {
            return getPlayer().getFace();
        } else {
            return getPlayer().getHair();
        }
    }

    public boolean isCosmeticEquipped(int itemid) {
        return getEquippedCosmeticid(itemid) == itemid;
    }

    public boolean isUsingOldPqNpcStyle() {
        return YamlConfig.config.server.USE_OLD_GMS_STYLED_PQ_NPCS && this.getPlayer().getParty() != null;
    }

    public Object[] getAvailableMasteryBooks() {
        return ItemInformationProvider.getInstance().usableMasteryBooks(this.getPlayer()).toArray();
    }

    public Object[] getAvailableSkillBooks() {
        List<Integer> ret = ItemInformationProvider.getInstance().usableSkillBooks(this.getPlayer());
        ret.addAll(SkillbookInformationProvider.getTeachableSkills(this.getPlayer()));

        return ret.toArray();
    }

    public int getNx(int type) {
        return getPlayer().getCashShop().getCash(type);
    }

    public Object[] getNamesWhoDropsItem(Integer itemId) {
        return ItemInformationProvider.getInstance().getWhoDrops(itemId).toArray();
    }

    public String getSkillBookInfo(int itemid) {
        SkillBookEntry sbe = SkillbookInformationProvider.getSkillbookAvailability(itemid);
        switch (sbe) {
            case UNAVAILABLE:
                return "";

            case REACTOR:
                return "    Obtainable through #rexploring#k (loot boxes).";

            case SCRIPT:
                return "    Obtainable through #rexploring#k (field interaction).";

            case QUEST_BOOK:
                return "    Obtainable through #rquestline#k (collecting book).";

            case QUEST_REWARD:
                return "    Obtainable through #rquestline#k (quest reward).";

            default:
                return "    Obtainable through #rquestline#k.";
        }
    }

    // (CPQ + WED wishlist) by -- Drago (Dragohe4rt)
    public int cpqCalcAvgLvl(int map) {
        int num = 0;
        int avg = 0;
        for (MapObject mmo : c.getChannelServer().getMapFactory().getMap(map).getAllPlayer()) {
            avg += ((Character) mmo).getLevel();
            num++;
        }
        avg /= num;
        return avg;
    }

    public boolean sendCPQMapLists() {
        String msg = LanguageConstants.getMessage(getPlayer(), LanguageConstants.CPQPickRoom);
        int msgLen = msg.length();
        for (int i = 0; i < 6; i++) {
            if (fieldTaken(i)) {
                if (fieldLobbied(i)) {
                    msg += "#b#L" + i + "#Carnival Field " + (i + 1) + " (Level: "  // "Carnival field" GMS-like improvement thanks to Jayd (jaydenseah)
                            + cpqCalcAvgLvl(980000100 + i * 100) + " / "
                            + getPlayerCount(980000100 + i * 100) + "x"
                            + getPlayerCount(980000100 + i * 100) + ")  #l\r\n";
                }
            } else {
                if (i >= 0 && i <= 3) {
                    msg += "#b#L" + i + "#Carnival Field " + (i + 1) + " (2x2) #l\r\n";
                } else {
                    msg += "#b#L" + i + "#Carnival Field " + (i + 1) + " (3x3) #l\r\n";
                }
            }
        }

        if (msg.length() > msgLen) {
            sendSimple(msg);
            return true;
        } else {
            return false;
        }
    }

    public boolean fieldTaken(int field) {
        if (!c.getChannelServer().canInitMonsterCarnival(true, field)) {
            return true;
        }
        if (!c.getChannelServer().getMapFactory().getMap(980000100 + field * 100).getAllPlayer().isEmpty()) {
            return true;
        }
        if (!c.getChannelServer().getMapFactory().getMap(980000101 + field * 100).getAllPlayer().isEmpty()) {
            return true;
        }
        return !c.getChannelServer().getMapFactory().getMap(980000102 + field * 100).getAllPlayer().isEmpty();
    }

    public boolean fieldLobbied(int field) {
        return !c.getChannelServer().getMapFactory().getMap(980000100 + field * 100).getAllPlayer().isEmpty();
    }

    public void cpqLobby(int field) {
        try {
            final MapleMap map, mapExit;
            Channel cs = c.getChannelServer();

            map = cs.getMapFactory().getMap(980000100 + 100 * field);
            mapExit = cs.getMapFactory().getMap(980000000);
            for (PartyCharacter mpc : c.getPlayer().getParty().getMembers()) {
                final Character mc = mpc.getPlayer();
                if (mc != null) {
                    mc.setChallenged(false);
                    mc.changeMap(map, map.getPortal(0));
                    mc.sendPacket(PacketCreator.serverNotice(6, LanguageConstants.getMessage(mc, LanguageConstants.CPQEntryLobby)));
                    TimerManager tMan = TimerManager.getInstance();
                    tMan.schedule(() -> mapClock((int) MINUTES.toSeconds(3)), 1500);

                    mc.setCpqTimer(TimerManager.getInstance().schedule(() -> mc.changeMap(mapExit, mapExit.getPortal(0)), MINUTES.toMillis(3)));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Character getChrById(int id) {
        return c.getChannelServer().getPlayerStorage().getCharacterById(id);
    }

    public void cancelCPQLobby() {
        for (PartyCharacter mpc : c.getPlayer().getParty().getMembers()) {
            Character mc = mpc.getPlayer();
            if (mc != null) {
                mc.clearCpqTimer();
            }
        }
    }

    private void warpoutCPQLobby(MapleMap lobbyMap) {
        MapleMap out = lobbyMap.getChannelServer().getMapFactory().getMap((lobbyMap.getId() < 980030000) ? 980000000 : 980030000);
        for (Character mc : lobbyMap.getAllPlayers()) {
            mc.resetCP();
            mc.setTeam(-1);
            mc.setMonsterCarnival(null);
            mc.changeMap(out, out.getPortal(0));
        }
    }

    private int isCPQParty(MapleMap lobby, Party party) {
        int cpqMinLvl, cpqMaxLvl;

        if (lobby.isCPQLobby()) {
            cpqMinLvl = 30;
            cpqMaxLvl = 200;
        } else {
            cpqMinLvl = 51;
            cpqMaxLvl = 200;
        }

        List<PartyCharacter> partyMembers = party.getPartyMembers();
        for (PartyCharacter pchr : partyMembers) {
            if (pchr.getLevel() >= cpqMinLvl && pchr.getLevel() <= cpqMaxLvl) {
                if (lobby.getCharacterById(pchr.getId()) == null) {
                    return 1;  // party member detected out of area
                }
            } else {
                return 2;  // party member doesn't fit requirements
            }
        }

        return 0;
    }

    private int canStartCPQ(MapleMap lobby, Party party, Party challenger) {
        int ret = isCPQParty(lobby, party);
        if (ret != 0) {
            return ret;
        }

        ret = isCPQParty(lobby, challenger);
        if (ret != 0) {
            return -ret;
        }

        return 0;
    }

    public void startCPQ(final Character challenger, final int field) {
        try {
            cancelCPQLobby();

            final MapleMap lobbyMap = getPlayer().getMap();
            if (challenger != null) {
                if (challenger.getParty() == null) {
                    throw new RuntimeException("No opponent found!");
                }

                for (PartyCharacter mpc : challenger.getParty().getMembers()) {
                    Character mc = mpc.getPlayer();
                    if (mc != null) {
                        mc.changeMap(lobbyMap, lobbyMap.getPortal(0));
                        TimerManager tMan = TimerManager.getInstance();
                        tMan.schedule(() -> mapClock(10), 1500);
                    }
                }
                for (PartyCharacter mpc : getPlayer().getParty().getMembers()) {
                    Character mc = mpc.getPlayer();
                    if (mc != null) {
                        TimerManager tMan = TimerManager.getInstance();
                        tMan.schedule(() -> mapClock(10), 1500);
                    }
                }
            }
            final int mapid = c.getPlayer().getMapId() + 1;
            TimerManager tMan = TimerManager.getInstance();
            tMan.schedule(() -> {
                try {
                    for (PartyCharacter mpc : getPlayer().getParty().getMembers()) {
                        Character mc = mpc.getPlayer();
                        if (mc != null) {
                            mc.setMonsterCarnival(null);
                        }
                    }
                    for (PartyCharacter mpc : challenger.getParty().getMembers()) {
                        Character mc = mpc.getPlayer();
                        if (mc != null) {
                            mc.setMonsterCarnival(null);
                        }
                    }
                } catch (NullPointerException npe) {
                    warpoutCPQLobby(lobbyMap);
                    return;
                }

                Party lobbyParty = getPlayer().getParty(), challengerParty = challenger.getParty();
                int status = canStartCPQ(lobbyMap, lobbyParty, challengerParty);
                if (status == 0) {
                    new MonsterCarnival(lobbyParty, challengerParty, mapid, true, (field / 100) % 10);
                } else {
                    warpoutCPQLobby(lobbyMap);
                }
            }, 11000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startCPQ2(final Character challenger, final int field) {
        try {
            cancelCPQLobby();

            final MapleMap lobbyMap = getPlayer().getMap();
            if (challenger != null) {
                if (challenger.getParty() == null) {
                    throw new RuntimeException("No opponent found!");
                }

                for (PartyCharacter mpc : challenger.getParty().getMembers()) {
                    Character mc = mpc.getPlayer();
                    if (mc != null) {
                        mc.changeMap(lobbyMap, lobbyMap.getPortal(0));
                        mapClock(10);
                    }
                }
            }
            final int mapid = c.getPlayer().getMapId() + 100;
            TimerManager tMan = TimerManager.getInstance();
            tMan.schedule(() -> {
                try {
                    for (PartyCharacter mpc : getPlayer().getParty().getMembers()) {
                        Character mc = mpc.getPlayer();
                        if (mc != null) {
                            mc.setMonsterCarnival(null);
                        }
                    }
                    for (PartyCharacter mpc : challenger.getParty().getMembers()) {
                        Character mc = mpc.getPlayer();
                        if (mc != null) {
                            mc.setMonsterCarnival(null);
                        }
                    }
                } catch (NullPointerException npe) {
                    warpoutCPQLobby(lobbyMap);
                    return;
                }

                Party lobbyParty = getPlayer().getParty(), challengerParty = challenger.getParty();
                int status = canStartCPQ(lobbyMap, lobbyParty, challengerParty);
                if (status == 0) {
                    new MonsterCarnival(lobbyParty, challengerParty, mapid, false, (field / 1000) % 10);
                } else {
                    warpoutCPQLobby(lobbyMap);
                }
            }, 10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean sendCPQMapLists2() {
        String msg = LanguageConstants.getMessage(getPlayer(), LanguageConstants.CPQPickRoom);
        int msgLen = msg.length();
        for (int i = 0; i < 3; i++) {
            if (fieldTaken2(i)) {
                if (fieldLobbied2(i)) {
                    msg += "#b#L" + i + "#Carnival Field " + (i + 1) + " (Level: "  // "Carnival field" GMS-like improvement thanks to Jayd
                            + cpqCalcAvgLvl(980031000 + i * 1000) + " / "
                            + getPlayerCount(980031000 + i * 1000) + "x"
                            + getPlayerCount(980031000 + i * 1000) + ")  #l\r\n";
                }
            } else {
                if (i == 0 || i == 1) {
                    msg += "#b#L" + i + "#Carnival Field " + (i + 1) + " (2x2) #l\r\n";
                } else {
                    msg += "#b#L" + i + "#Carnival Field " + (i + 1) + " (3x3) #l\r\n";
                }
            }
        }

        if (msg.length() > msgLen) {
            sendSimple(msg);
            return true;
        } else {
            return false;
        }
    }

    public boolean fieldTaken2(int field) {
        if (!c.getChannelServer().canInitMonsterCarnival(false, field)) {
            return true;
        }
        if (!c.getChannelServer().getMapFactory().getMap(980031000 + field * 1000).getAllPlayer().isEmpty()) {
            return true;
        }
        if (!c.getChannelServer().getMapFactory().getMap(980031100 + field * 1000).getAllPlayer().isEmpty()) {
            return true;
        }
        return !c.getChannelServer().getMapFactory().getMap(980031200 + field * 1000).getAllPlayer().isEmpty();
    }

    public boolean fieldLobbied2(int field) {
        return !c.getChannelServer().getMapFactory().getMap(980031000 + field * 1000).getAllPlayer().isEmpty();
    }

    public void cpqLobby2(int field) {
        try {
            final MapleMap map, mapExit;
            Channel cs = c.getChannelServer();

            mapExit = cs.getMapFactory().getMap(980030000);
            map = cs.getMapFactory().getMap(980031000 + 1000 * field);
            for (PartyCharacter mpc : c.getPlayer().getParty().getMembers()) {
                final Character mc = mpc.getPlayer();
                if (mc != null) {
                    mc.setChallenged(false);
                    mc.changeMap(map, map.getPortal(0));
                    mc.sendPacket(PacketCreator.serverNotice(6, LanguageConstants.getMessage(mc, LanguageConstants.CPQEntryLobby)));
                    TimerManager tMan = TimerManager.getInstance();
                    tMan.schedule(() -> mapClock((int) MINUTES.toSeconds(3)), 1500);

                    mc.setCpqTimer(TimerManager.getInstance().schedule(() -> mc.changeMap(mapExit, mapExit.getPortal(0)), MINUTES.toMillis(3)));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void mapClock(int time) {
        getPlayer().getMap().broadcastMessage(PacketCreator.getClock(time));
    }

    private boolean sendCPQChallenge(String cpqType, int leaderid) {
        Set<Integer> cpqLeaders = new HashSet<>();
        cpqLeaders.add(leaderid);
        cpqLeaders.add(getPlayer().getId());

        return c.getWorldServer().getMatchCheckerCoordinator().createMatchConfirmation(MatchCheckerType.CPQ_CHALLENGE, c.getWorld(), getPlayer().getId(), cpqLeaders, cpqType);
    }

    public void answerCPQChallenge(boolean accept) {
        c.getWorldServer().getMatchCheckerCoordinator().answerMatchConfirmation(getPlayer().getId(), accept);
    }

    public void challengeParty2(int field) {
        Character leader = null;
        MapleMap map = c.getChannelServer().getMapFactory().getMap(980031000 + 1000 * field);
        for (MapObject mmo : map.getAllPlayer()) {
            Character mc = (Character) mmo;
            if (mc.getParty() == null) {
                sendOk(LanguageConstants.getMessage(mc, LanguageConstants.CPQFindError));
                return;
            }
            if (mc.getParty().getLeader().getId() == mc.getId()) {
                leader = mc;
                break;
            }
        }
        if (leader != null) {
            if (!leader.isChallenged()) {
                if (!sendCPQChallenge("cpq2", leader.getId())) {
                    sendOk(LanguageConstants.getMessage(leader, LanguageConstants.CPQChallengeRoomAnswer));
                }
            } else {
                sendOk(LanguageConstants.getMessage(leader, LanguageConstants.CPQChallengeRoomAnswer));
            }
        } else {
            sendOk(LanguageConstants.getMessage(leader, LanguageConstants.CPQLeaderNotFound));
        }
    }

    public void challengeParty(int field) {
        Character leader = null;
        MapleMap map = c.getChannelServer().getMapFactory().getMap(980000100 + 100 * field);
        if (map.getAllPlayer().size() != getPlayer().getParty().getMembers().size()) {
            sendOk("An unexpected error regarding the other party has occurred.");
            return;
        }
        for (MapObject mmo : map.getAllPlayer()) {
            Character mc = (Character) mmo;
            if (mc.getParty() == null) {
                sendOk(LanguageConstants.getMessage(mc, LanguageConstants.CPQFindError));
                return;
            }
            if (mc.getParty().getLeader().getId() == mc.getId()) {
                leader = mc;
                break;
            }
        }
        if (leader != null) {
            if (!leader.isChallenged()) {
                if (!sendCPQChallenge("cpq1", leader.getId())) {
                    sendOk(LanguageConstants.getMessage(leader, LanguageConstants.CPQChallengeRoomAnswer));
                }
            } else {
                sendOk(LanguageConstants.getMessage(leader, LanguageConstants.CPQChallengeRoomAnswer));
            }
        } else {
            sendOk(LanguageConstants.getMessage(leader, LanguageConstants.CPQLeaderNotFound));
        }
    }

    private synchronized boolean setupAriantBattle(Expedition exped, int mapid) {
        MapleMap arenaMap = this.getMap().getChannelServer().getMapFactory().getMap(mapid + 1);
        if (!arenaMap.getAllPlayers().isEmpty()) {
            return false;
        }

        new AriantColiseum(arenaMap, exped);
        return true;
    }

    public String startAriantBattle(ExpeditionType expedType, int mapid) {
        if (!GameConstants.isAriantColiseumLobby(mapid)) {
            return "You cannot start an Ariant tournament from outside the Battle Arena Entrance.";
        }

        Expedition exped = this.getMap().getChannelServer().getExpedition(expedType);
        if (exped == null) {
            return "Please register on an expedition before attempting to start an Ariant tournament.";
        }

        List<Character> players = exped.getActiveMembers();

        int playersSize = players.size();
        if (!(playersSize >= exped.getMinSize() && playersSize <= exped.getMaxSize())) {
            return "Make sure there are between #r" + exped.getMinSize() + " ~ " + exped.getMaxSize() + " players#k in this room to start the battle.";
        }

        MapleMap leaderMap = this.getMap();
        for (Character mc : players) {
            if (mc.getMap() != leaderMap) {
                return "All competing players should be on this area to start the battle.";
            }

            if (mc.getParty() != null) {
                return "All competing players must not be on a party to start the battle.";
            }

            int level = mc.getLevel();
            if (!(level >= expedType.getMinLevel() && level <= expedType.getMaxLevel())) {
                return "There are competing players outside of the acceptable level range in this room. All players must be on #blevel between 20~30#k to start the battle.";
            }
        }

        if (setupAriantBattle(exped, mapid)) {
            return "";
        } else {
            return "Other players are already competing on the Ariant tournament in this room. Please wait a while until the arena becomes available again.";
        }
    }

    public void sendMarriageWishlist(boolean groom) {
        Character player = this.getPlayer();
        Marriage marriage = player.getMarriageInstance();
        if (marriage != null) {
            int cid = marriage.getIntProperty(groom ? "groomId" : "brideId");
            Character chr = marriage.getPlayerById(cid);
            if (chr != null) {
                if (chr.getId() == player.getId()) {
                    player.sendPacket(WeddingPackets.onWeddingGiftResult((byte) 0xA, marriage.getWishlistItems(groom), marriage.getGiftItems(player.getClient(), groom)));
                } else {
                    marriage.setIntProperty("wishlistSelection", groom ? 0 : 1);
                    player.sendPacket(WeddingPackets.onWeddingGiftResult((byte) 0x09, marriage.getWishlistItems(groom), marriage.getGiftItems(player.getClient(), groom)));
                }
            }
        }
    }

    public void sendMarriageGifts(List<Item> gifts) {
        this.getPlayer().sendPacket(WeddingPackets.onWeddingGiftResult((byte) 0xA, Collections.singletonList(""), gifts));
    }

    public boolean createMarriageWishlist() {
        Marriage marriage = this.getPlayer().getMarriageInstance();
        if (marriage != null) {
            Boolean groom = marriage.isMarriageGroom(this.getPlayer());
            if (groom != null) {
                String wlKey;
                if (groom) {
                    wlKey = "groomWishlist";
                } else {
                    wlKey = "brideWishlist";
                }

                if (marriage.getProperty(wlKey).contentEquals("")) {
                    getClient().sendPacket(WeddingPackets.sendWishList());
                    return true;
                }
            }
        }
        return false;
    }

    public void logMessage(String scriptName, String message) {
        log.info("[{}] - {}", scriptName, message);
    }

    /**
     * cm.letters("Hello world"); will show Christmas letters Hello world
     *
     * @param input the text to turn into christmas :)
     * @return String with item images
     */
    public String letters(String input) {
        //input = input.toLowerCase();
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == ' ') {
                str.append("\t");
            } else {
                str.append("#i").append(convert(input.charAt(i))).append("#");
            }
        }
        return str.toString();
    }

    private int convert(char in) {
        int upper = 3991000;
        int lower = 3991026;
        int output = java.lang.Character.isUpperCase(in) ? upper : lower;
        switch (java.lang.Character.toLowerCase(in)) {
            case 'a':
                output += 0;
                break;
            case 'b':
                output += 1;
                break;
            case 'c':
                output += 2;
                break;
            case 'd':
                output += 3;
                break;
            case 'e':
                output += 4;
                break;
            case 'f':
                output += 5;
                break;
            case 'g':
                output += 6;
                break;
            case 'h':
                output += 7;
                break;
            case 'i':
                output += 8;
                break;
            case 'j':
                output += 9;
                break;
            case 'k':
                output += 10;
                break;
            case 'l':
                output += 11;
                break;
            case 'm':
                output += 12;
                break;
            case 'n':
                output += 13;
                break;
            case 'o':
                output += 14;
                break;
            case 'p':
                output += 15;
                break;
            case 'q':
                output += 16;
                break;
            case 'r':
                output += 17;
                break;
            case 's':
                output += 18;
                break;
            case 't':
                output += 19;
                break;
            case 'u':
                output += 20;
                break;
            case 'v':
                output += 21;
                break;
            case 'w':
                output += 22;
                break;
            case 'x':
                output += 23;
                break;
            case 'y':
                output += 24;
                break;
            case 'z':
                output += 25;
                break;
        }
        return output;
    }

    public void gainStatItem(int itemId, short str, short dex, short luk, short int_, short matk, short watk, short acc, short avoid, short jump, short speed, short wdef, short mdef, short hp, short mp, byte upgradeSlots) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        Item item = ii.getEquipById(itemId);
        InventoryType type = ii.getInventoryType(itemId);
        if (type.equals(InventoryType.EQUIP)) {
            InventoryManipulator.addFromDrop(c, ii.statItem((Equip) item, str, dex, luk, int_, matk, watk, acc, avoid, jump, speed, wdef, mdef, hp, mp, upgradeSlots));
        } else {
            System.out.println("[NPCConversationManager] Stat item is not an equip!");
        }
    }

    public int getReqLevel(int itemid) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        return ii.getEquipLevelReq(itemid);
    }

    public String getInventoryAsString(String type) {
        StringBuilder sb = new StringBuilder();
        Inventory inv = c.getPlayer().getInventory(InventoryType.getByWZName(type));
        int i = 0;
        sb.append("\r\n");
        for (Item item : inv.list()) {
            i++;
            if (i > 4) {
                sb.append("\r\n");
                i = 1;
            }
            sb.append("#L").append(item.getPosition()).append("##i").append(item.getItemId()).append(":#").append(item.getQuantity()).append("#l");
        }
        return sb.toString();
    }

    public int getMesoStarforceCost(int nItemLevel, int nReqLevel, boolean superiorEquip) {
        if (nItemLevel < 0) {
            return 0;
        }

        int baseMesoCost = superiorEquip ? 10000000 * (nReqLevel / 10) : 8000 * (nReqLevel + 1);
        double mesoMultiplier = superiorEquip ? 0.1 * (nItemLevel + 1) : 1.0 + 0.1 * (nItemLevel + 1);

        if (!superiorEquip && nItemLevel >= 10) {
            mesoMultiplier *= 4;
        }

        return Math.min((int) (baseMesoCost * mesoMultiplier), YamlConfig.config.server.STARFORCE_MAX_MESO_COST);
    }

    public boolean isSuperior(int nItemId) {
        return (ItemConstants.SuperiorItemIds.contains(nItemId));
    }

    public int getSpellTraceCost(int nItemLevel, int nReqLevel, boolean superiorEquip) {
        if (nItemLevel < 0) {
            return 0;
        }

        if (superiorEquip) {
            return 50 + (nItemLevel * 5);
        }

        int spellTraceReq = calculateBaseSpellTraceReq(nReqLevel);
        int scaling = calculateScalingFactor(nReqLevel) + (nItemLevel >= 10 ? 3 : 0);

        return Math.min(spellTraceReq + scaling * nItemLevel, YamlConfig.config.server.STARFORCE_MAX_SPELL_TRACE_COST);
    }

    private int calculateBaseSpellTraceReq(int nReqLevel) {
        if (nReqLevel < 30) return 5;
        if (nReqLevel < 70) return 10;
        if (nReqLevel < 100) return 15;
        if (nReqLevel < 120) return 20;
        return 25;
    }

    private int calculateScalingFactor(int nReqLevel) {
        if (nReqLevel < 30) return 1;
        if (nReqLevel < 70) return 1;
        if (nReqLevel < 100) return 2;
        if (nReqLevel < 120) return 3;
        return 4;
    }

    public double getProp(Equip nEquip) {
        if(ItemConstants.SuperiorItemIds.contains(nEquip.getItemId())) {
            if (nEquip.getLevel() == 0) {
                return 100.0;
            } else if (nEquip.getLevel() < 10) {
                return 50.0;
            } else {
                return 30.0;
            }
        } else {
            double prop = 0;
            prop = switch (nEquip.getLevel()) {
                case 0 -> 100;
                case 1 -> 98;
                case 2 -> 90;
                case 3 -> 80;
                case 4 -> 70;
                case 5 -> 60;
                case 6 -> 50;
                case 7 -> 40;
                case 8 -> 30;
                case 9 -> 25;
                case 10 -> 20;
                case 11 -> 18;
                case 12 -> 16;
                case 13 -> 14;
                case 14 -> 12;
                case 15 -> 10;
                case 16 -> 9;
                case 17 -> 8;
                case 18 -> 7;
                case 19 -> 6;
                case 20 -> 5;
                case 21 -> 4;
                case 22 -> 3;
                case 23 -> 2;
                case 24 -> 1;
                case 25 -> 1;
                case 26 -> 1;
                case 27 -> 1;
                case 28 -> 1;
                case 29 -> 1;
                default -> prop;
            };
            return prop;
        }
    }

    private double getBoom(Equip nEquip) {
        if (nEquip.getLevel() < 5) {
            return 0;
        } else if (nEquip.getItemLevel() < 10) {
            return nEquip.getItemLevel();
        } else {
            return 30.0;
        }
    }

    public static void improveSuperiorEquips(Equip nEquip, boolean fail) {
        int level = nEquip.getLevel();

        int watkAdd = 4 + level;
        int matkAdd = 4 + level;
        int strAdd = 10 + level;
        int dexAdd = 10 + level;
        int lukAdd = 10 + level;
        int intAdd = 10 + level;

        if (fail) {
            strAdd = -strAdd;
            dexAdd = -dexAdd;
            lukAdd = -lukAdd;
            intAdd = -intAdd;
            watkAdd = -watkAdd;
            matkAdd = -matkAdd;
        }

        if (level >= 5) {
            nEquip.setWatk(getShortMaxIfOverflow(nEquip.getWatk() + watkAdd));
            nEquip.setMatk(getShortMaxIfOverflow(nEquip.getMatk() + matkAdd));
        }

        nEquip.setStr(getShortMaxIfOverflow(nEquip.getStr() + strAdd));
        nEquip.setDex(getShortMaxIfOverflow(nEquip.getDex() + dexAdd));
        nEquip.setInt(getShortMaxIfOverflow(nEquip.getInt() + intAdd));
        nEquip.setLuk(getShortMaxIfOverflow(nEquip.getLuk() + lukAdd));
    }

    public Item StarForceEquip(Item equip) {
        Equip.ScrollResult scrollSuccess = Equip.ScrollResult.FAIL;
        if (equip instanceof Equip nEquip) {
            if ((nEquip.getUpgradeSlots()) > 0) {
                double prop = getProp(nEquip); //success chance
                double boom = getBoom(nEquip); //boom chances
                if (ItemConstants.SuperiorItemIds.contains(nEquip.getItemId())) {
                    if (rollSuccessChance(boom)) {
                        return null;
                    }
                    if (rollSuccessChance(prop)) {
                        improveSuperiorEquips(nEquip, false);
                        nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
                        nEquip.setLevel((byte) (nEquip.getLevel() + 1));
                        scrollSuccess = Equip.ScrollResult.SUCCESS;
                    } else {
                        nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() + 1));
                        nEquip.setLevel((byte) (nEquip.getLevel() - 1));
                        improveSuperiorEquips(nEquip, true);
                    }
                }
                else {
                    if (prop <= 0) {
                        return equip;
                    }
                    if (rollSuccessChance(prop)) {
                        improveEquipStats(nEquip, false);
                        nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
                        nEquip.setLevel((byte) (nEquip.getLevel() + 1));
                        scrollSuccess = Equip.ScrollResult.SUCCESS;
                    } else if ((nEquip.getLevel() % 1) != 0) {
                        nEquip.setLevel((byte) (nEquip.getLevel() - 1));
                        nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() + 1));
                        improveEquipStats(nEquip, true);
                    }
                }
            }
        }
        final List<ModifyInventory> mods = new ArrayList<>();
        mods.add(new ModifyInventory(3, equip));
        mods.add(new ModifyInventory(0, equip));
        c.sendPacket(PacketCreator.modifyInventory(true, mods));
        c.getPlayer().getMap().broadcastMessage(PacketCreator.getScrollEffect(c.getPlayer().getId(), scrollSuccess, false, false));
        return equip;
    }

    private static void applyStatAdditions(Equip nEquip, StarBoost boost, boolean fail) {
        int strAdd = boost.getStr();
        int dexAdd = boost.getDex();
        int lukAdd = boost.getLuk();
        int intAdd = boost.getInt();
        int watkAdd = boost.getWatk();
        int matkAdd = boost.getMatk();

        if (fail) {
            strAdd = -strAdd;
            dexAdd = -dexAdd;
            lukAdd = -lukAdd;
            intAdd = -intAdd;
            watkAdd = -watkAdd;
            matkAdd = -matkAdd;
        }

        nEquip.setStr(getShortMaxIfOverflow(nEquip.getStr() + strAdd));
        nEquip.setDex(getShortMaxIfOverflow(nEquip.getDex() + dexAdd));
        nEquip.setLuk(getShortMaxIfOverflow(nEquip.getLuk() + lukAdd));
        nEquip.setInt(getShortMaxIfOverflow(nEquip.getInt() + intAdd));
        nEquip.setWatk(getShortMaxIfOverflow(nEquip.getWatk() + watkAdd));
        nEquip.setMatk(getShortMaxIfOverflow(nEquip.getMatk() + matkAdd));
    }

    public static StarBoost calculateStatBoost(int itemId, int level) {
        int catid = ((itemId / 10000) % 100);
        if (catid >= 30) {
            catid = 30;
        }

        return getStatAdditions(catid, level);
    }


    private static StarBoost getStatAdditions(int catid, int level) {
        int watkAdd = 0, matkAdd = 0, strAdd = 0, dexAdd = 0, lukAdd = 0, intAdd = 0;

        switch (catid) {
            case 0 -> {
                if (level == 0) {
                    dexAdd = strAdd = lukAdd = intAdd = 1;
                } else if (level < 8) {
                    dexAdd = strAdd = lukAdd = intAdd = 2;
                } else if (level <= 9) {
                    dexAdd = strAdd = lukAdd = intAdd = 3;
                    watkAdd = matkAdd = 1;
                } else if (level <= 11) {
                    dexAdd = strAdd = lukAdd = intAdd = 5;
                    watkAdd = matkAdd = 3;
                } else if (level <= 13) {
                    dexAdd = strAdd = lukAdd = intAdd = 10;
                    watkAdd = matkAdd = 5;
                } else {
                    dexAdd = strAdd = lukAdd = intAdd = 10;
                    watkAdd = matkAdd = 8;
                }
            }
            case 30 -> {
                if (level <= 2) {
                    dexAdd = strAdd = lukAdd = intAdd = 1;
                    watkAdd = matkAdd = 2;
                } else if (level <= 5) {
                    dexAdd = strAdd = lukAdd = intAdd = 2;
                    watkAdd = matkAdd = 2;
                } else if (level <= 7) {
                    dexAdd = strAdd = lukAdd = intAdd = 2;
                    watkAdd = matkAdd = 3;
                } else if (level <= 9) {
                    dexAdd = strAdd = lukAdd = intAdd = 3;
                    watkAdd = matkAdd = 4;
                } else if (level == 10) {
                    dexAdd = strAdd = lukAdd = intAdd = 3;
                    watkAdd = matkAdd = 5;
                } else if (level == 11) {
                    dexAdd = strAdd = lukAdd = intAdd = 5;
                    watkAdd = matkAdd = 10;
                } else if (level == 12) {
                    dexAdd = strAdd = lukAdd = intAdd = 7;
                    watkAdd = matkAdd = 15;
                } else if (level == 13) {
                    dexAdd = strAdd = lukAdd = intAdd = 10;
                    watkAdd = matkAdd = 20;
                } else {
                    dexAdd = strAdd = lukAdd = intAdd = 20;
                    watkAdd = matkAdd = 30;
                }
            }
            case 1, 2 -> {
                if (level <= 5) {
                    dexAdd = strAdd = lukAdd = intAdd = 1;
                } else if (level <= 7) {
                    dexAdd = strAdd = lukAdd = intAdd = 2;
                    watkAdd = matkAdd = 1;
                } else if (level <= 9) {
                    dexAdd = strAdd = lukAdd = intAdd = 2;
                    watkAdd = matkAdd = 2;
                } else if (level <= 11) {
                    dexAdd = strAdd = lukAdd = intAdd = 2;
                    watkAdd = matkAdd = 3;
                } else if (level == 12) {
                    dexAdd = strAdd = lukAdd = intAdd = 2;
                    watkAdd = matkAdd = 4;
                } else if (level == 13) {
                    dexAdd = strAdd = lukAdd = intAdd = 3;
                    watkAdd = matkAdd = 5;
                } else {
                    dexAdd = strAdd = lukAdd = intAdd = 7;
                    watkAdd = matkAdd = 7;
                }
            }
            case 4, 6, 7 -> {
                if (level <= 5) {
                    dexAdd = strAdd = lukAdd = intAdd = 1;
                } else if (level <= 7) {
                    dexAdd = strAdd = lukAdd = intAdd = 2;
                } else if (level <= 9) {
                    dexAdd = strAdd = lukAdd = intAdd = 3;
                    watkAdd = matkAdd = 2;
                } else if (level <= 11) {
                    dexAdd = strAdd = lukAdd = intAdd = 4;
                    watkAdd = matkAdd = 2;
                } else if (level == 12) {
                    dexAdd = strAdd = lukAdd = intAdd = 8;
                    watkAdd = matkAdd = 4;
                } else if (level == 13) {
                    dexAdd = strAdd = lukAdd = intAdd = 12;
                    watkAdd = matkAdd = 5;
                } else {
                    dexAdd = strAdd = lukAdd = intAdd = 20;
                    watkAdd = matkAdd = 7;
                }
            }
            case 5, 9, 10 -> {
                if (level <= 5) {
                    dexAdd = strAdd = lukAdd = intAdd = 2;
                } else if (level <= 7) {
                    dexAdd = strAdd = lukAdd = intAdd = 3;
                    watkAdd = matkAdd = 1;
                } else if (level <= 9) {
                    dexAdd = strAdd = lukAdd = intAdd = 4;
                    watkAdd = matkAdd = 2;
                } else if (level <= 11) {
                    dexAdd = strAdd = lukAdd = intAdd = 6;
                    watkAdd = matkAdd = 5;
                } else if (level == 12) {
                    dexAdd = strAdd = lukAdd = intAdd = 10;
                    watkAdd = matkAdd = 7;
                } else if (level == 13) {
                    dexAdd = strAdd = lukAdd = intAdd = 15;
                    watkAdd = matkAdd = 10;
                } else {
                    dexAdd = strAdd = lukAdd = intAdd = 20;
                    watkAdd = matkAdd = 12;
                }
            }
            case 8, 3, 13, 12 -> {
                if (level <= 5) {
                    dexAdd = strAdd = lukAdd = intAdd = 1;
                    watkAdd = matkAdd = 1;
                } else if (level <= 7) {
                    dexAdd = strAdd = lukAdd = intAdd = 1;
                    watkAdd = matkAdd = 2;
                } else if (level <= 9) {
                    dexAdd = strAdd = lukAdd = intAdd = 2;
                    watkAdd = matkAdd = 3;
                } else if (level <= 11) {
                    dexAdd = strAdd = lukAdd = intAdd = 4;
                    watkAdd = matkAdd = 3;
                } else if (level == 12) {
                    dexAdd = strAdd = lukAdd = intAdd = 3;
                    watkAdd = matkAdd = 5;
                } else if (level == 13) {
                    dexAdd = strAdd = lukAdd = intAdd = 5;
                    watkAdd = matkAdd = 5;
                } else {
                    dexAdd = strAdd = lukAdd = intAdd = 7;
                    watkAdd = matkAdd = 7;
                }
            }
            case 11 -> {
                if (level <= 5) {
                    dexAdd = strAdd = lukAdd = intAdd = 1;
                } else if (level <= 7) {
                    dexAdd = strAdd = lukAdd = intAdd = 1;
                    watkAdd = matkAdd = 1;
                } else if (level <= 9) {
                    dexAdd = strAdd = lukAdd = intAdd = 2;
                    watkAdd = matkAdd = 2;
                } else if (level <= 11) {
                    dexAdd = strAdd = lukAdd = intAdd = 3;
                    watkAdd = matkAdd = 2;
                } else if (level == 12) {
                    dexAdd = strAdd = lukAdd = intAdd = 4;
                    watkAdd = matkAdd = 3;
                } else if (level == 13) {
                    dexAdd = strAdd = lukAdd = intAdd = 5;
                    watkAdd = matkAdd = 5;
                } else {
                    dexAdd = strAdd = lukAdd = intAdd = 7;
                    watkAdd = matkAdd = 7;
                }
            }
            case 14 -> dexAdd = strAdd = lukAdd = intAdd = 1;
        }

        return new StarBoost(watkAdd, matkAdd, strAdd, dexAdd, lukAdd, intAdd);
    }


    public static void improveEquipStats(Equip nEquip, boolean fail) {
        int itemId = nEquip.getItemId();
        int level = nEquip.getLevel();
        StarBoost boost = calculateStatBoost(itemId, level);

        applyStatAdditions(nEquip, boost, fail);
    }

    private static double testYourLuck(double prop, int dices) {   // revamped testYourLuck author: David A.
        return Math.pow(1.0 - prop, dices);
    }

    public static boolean rollSuccessChance(double propPercent) {
        return Math.random() >= testYourLuck(propPercent / 100.0, YamlConfig.config.server.SCROLL_CHANCE_ROLLS);
    }

    private static short getMaximumShortMaxIfOverflow(int value1, int value2) {
        return (short) Math.min(Short.MAX_VALUE, Math.max(value1, value2));
    }

    private static short getShortMaxIfOverflow(int value) {
        return (short) Math.min(Short.MAX_VALUE, value);
    }


    public String getInventoryAsStringNoDuplicates(String type, Set<Integer> noShow) {
        Inventory inv = c.getPlayer().getInventory(InventoryType.getByWZName(type));
        Set<Integer> items = new HashSet<>();
        for (Item item : inv.list()) {
            if (noShow == null || !noShow.contains(item.getItemId())) {
                items.add(item.getItemId());
            }
        }

        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Integer item : items) {
            i++;
            if (i > 4) {
                sb.append("\r\n");
                i = 1;
            }
            sb.append("#L").append(item).append("##i").append(item).append(":##c").append(item).append("##l");
        }
        return sb.toString();
    }

    public String getInventoryAsStringFilter(String type, int[] filter) {
        StringBuilder sb = new StringBuilder();
        Inventory inv = c.getPlayer().getInventory(InventoryType.getByWZName(type));
        int i = 0;
        for (Item item : inv.list()) {
            if (IntStream.of(filter).anyMatch(id -> id == item.getItemId())) {
                i++;
                if (i > 4) {
                    sb.append("\r\n");
                    i = 1;
                }
                sb.append("#L").append(item.getPosition()).append("##i").append(item.getItemId()).append(":##l");
            }
        }
        return sb.toString();
    }

    public void summonMob(int mobid) {
        summonMob(mobid, 1);
    }

    public void summonMob(int mobid, int count) {
        for (int i = 0; i < count; i++) {
            Monster mob = LifeFactory.getMonster(mobid);
            getPlayer().getMap().spawnMonsterOnGroundBelow(mob, getPlayer().getPosition());
        }
    }

    public void makeNpc(int npcid, int x, int y) { // npception ecksdee
        Point p = new Point(x, y);
        NPC npc = LifeFactory.getNPC(npcid);
        npc.setPosition(p);
        npc.setCy(p.y);
        npc.setRx0(p.x + 50);
        npc.setRx1(p.x - 50);
        npc.setFh(getPlayer().getMap().getFootholds().findBelow(p).getId());
        getPlayer().getMap().addMapObject(npc);
        getPlayer().getMap().broadcastMessage(PacketCreator.spawnNPC(npc));
    }

    public void makeNpc(int npcid) {
        makeNpc(npcid, getPlayer().getPosition().x, getPlayer().getPosition().y);
    }

    public boolean isEquip(int id) {
        return ItemInformationProvider.getInstance().getInventoryType(id).equals(InventoryType.EQUIP);
    }

    public Equip getEquipById(int id) {
        return (Equip) ItemInformationProvider.getInstance().getEquipById(id);
    }

    /**
     * DO NOT TOUCH IF YOU DONT KNOW WHAT IT IS
     *
     * @param query
     * @param return_item
     * @return ANYTHING from the database
     */
    public ArrayList masterQueryRaw(String query, String return_item) {
        ArrayList<Object> queryObjects = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    queryObjects.add(rs.getObject(return_item));
                }
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        if (queryObjects.isEmpty()) {
            queryObjects.add("The query:\r\n#e" + query + "#n\r\nDid not give any result.");
            return queryObjects;
        }
        return queryObjects;
    }

    public boolean masterQueryUpdate(String query) {
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(query)) {
                ps.executeUpdate();
            }
            return true;
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return false;
    }

    public boolean masterQueryInsert(String query) {
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(query)) {
                ps.addBatch();
                ps.executeBatch();
            }
            return true;
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return false;
    }

    public void clearDrops() {
        MonsterInformationProvider.getInstance().clearDrops();
    }

    public ArrayList<NPC> getNPCs(Collection<MapObject> objects) {
        ArrayList<NPC> NPCs = new ArrayList<>();
        for (MapObject object : objects) {
            if (object instanceof NPC) {
                NPCs.add((NPC) object);
            }
        }
        return NPCs;
    }

    public ArrayList<Monster> getMonsters(Collection<MapObject> objects) {
        ArrayList<Monster> Monsters = new ArrayList<>();
        for (MapObject object : objects) {
            if (object instanceof Monster) {
                Monsters.add((Monster) object);
            }
        }
        return Monsters;
    }

    public ArrayList<MapItem> getMapItems(Collection<MapObject> objects) {
        ArrayList<MapItem> MapItems = new ArrayList<>();
        for (MapObject object : objects) {
            if (object instanceof MapItem) {
                MapItems.add((MapItem) object);
            }
        }
        return MapItems;
    }

    public List<ShopItem> getShopItems(int shopId) {
        return ShopFactory.getInstance().getShop(shopId).getItems();
    }

    public void removeShopItem(int shopId, ShopItem item) {
        ShopFactory.getInstance().getShop(shopId).getItems().remove(item);
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM `shopitems` WHERE `shopid` = ? AND `itemid` = ?")) {
                ps.setInt(1, shopId);
                ps.setInt(2, item.getItemId());
                ps.execute();
            }
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
        }
    }

    public void setShopItemPrice(int shopId, ShopItem item, int newPrice) {
        ShopFactory.getInstance().getShop(shopId).getItems().stream().filter(i -> i.equals(item)).findFirst().ifPresent(shopItem -> shopItem.setPrice(newPrice));
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE `shopitems` SET `price` = ? WHERE `shopid` = ? AND `itemid` = ?")) {
                ps.setInt(1, newPrice);
                ps.setInt(2, shopId);
                ps.setInt(3, item.getItemId());
                ps.execute();
            }
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
        }
    }

    public String spyOnPlayer(String type, String playerName) {
        StringBuilder spyInfo = new StringBuilder();
        Character player = c.getWorldServer().getPlayerStorage().getCharacterByName(playerName);
        if (player == null) {
            System.out.println("Tried spying on a null or offline player " + playerName);
            spyInfo.append(
                    "This player seems to be offline at the moment"); //incase player logs out while you're spying
        }
        assert player != null;
        Inventory equipped = player.getInventory(InventoryType.EQUIPPED);
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        switch (type) {
            case "equip":
                spyInfo.append("#b").append(playerName).append("'s Equipped Items:#k\r\n\r\n");

                for (Item item : equipped) {
                    int itemId = item.getItemId();
                    Equip equippedItem = (Equip) item;
                    spyInfo.append("Item: #v").append(itemId).append("# - #z").append(itemId).append("#\r\n")
                            .append("Stats:\r\n");

                    Map<String, Integer> cleanStats = ii.getCleanStats(itemId);

                    Map<Equip.StatUpgrade, Short> statsMap = equippedItem.getStats();
                    for (Map.Entry<Equip.StatUpgrade, Short> entry : statsMap.entrySet()) {
                        Equip.StatUpgrade statUpgrade = entry.getKey();
                        Short upgradedValue = entry.getValue();

                        Integer cleanStatValue = cleanStats.get(statUpgrade.toString().toLowerCase());
                        if (cleanStatValue == null) {
                            cleanStatValue = 0;
                        }

                        if (cleanStatValue.equals(upgradedValue.intValue())) {
                            upgradedValue = 0;
                        }

                        String statName = statUpgrade.toString().substring(3);
                        spyInfo.append(statName).append(" #r").append(cleanStatValue).append("#k / +");
                        spyInfo.append("#r").append(upgradedValue).append("#k");
                        spyInfo.append("\r\n");
                    }
                    spyInfo.append("Upgrade slots - ").append(equippedItem.getUpgradeSlots())
                            .append("\r\n\r\n");
                }

                break;

            case "inventory":
                spyInfo.append("#b").append(playerName).append("'s Inventory:#k\r\n\r\n");
                for (InventoryType invType : InventoryType.values()) {
                    if (invType != null && invType != InventoryType.EQUIPPED
                            && invType != InventoryType.UNDEFINED && invType != InventoryType.CANHOLD) {
                        for (Item item : player.getInventory(invType).sortedList()) {
                            if (item != null) {
                                String itemName = ii.getName(item.getItemId());
                                spyInfo.append("#v").append(item.getItemId()).append("# ").append(itemName)
                                        .append(" - x(#r").append(item.getQuantity()).append("#k)\r\n");
                            }
                        }
                    }
                }
                break;

            case "info":
                spyInfo.append("#b").append(playerName).append("'s Information:#k\r\n\r\n");
                //amount value
                int[] priorityItems = {2431000, 2431001, 5530009, 5530010, 5530011, 5530012, 5530013,
                        5530014, 5530015, 5530016, 5530017, 5530018, 4000302, 4000303,
                        4000304, 4000305, 4000307, 4000308, 4000309, 4000310, 5222000, 5451000};
                for (int priorityItem : priorityItems) {
                    spyInfo.append("#v").append(priorityItem).append("# - #z").append(priorityItem)
                            .append("# x(#r").append(ii.getEquipById(priorityItem).getQuantity())
                            .append("#k)\r\n");
                }

                break;

            default:
        }

        return spyInfo.toString();
    }

    public String getEligibleEquipSelectionString(int itemId) {
        StringBuilder sb = new StringBuilder();
        Character character = Objects.requireNonNull(getPlayer(), "getPlayer() returned null.");

        List<Item> equips = character.getInventory(InventoryType.EQUIP).listById(itemId);
        for (Item equip : equips) {
            Equip equipItem = (Equip) equip;
            sb.append("#L").append(equip.getPosition()).append("##i").append(equipItem.getItemId()).append(":##l\r\n");
        }

        return sb.toString();
    }

    public StarBoost getStarForceEquipCraftingBonus(int slot) {
        Equip equipItem = getStarForceEquip(slot);

        return getStarForceEquipCraftingBonus(equipItem);
    }

    public int getStarForceEquipSpellTraceBonus(int slot) {
        return getStarForceEquipSpellTraceBonus(getStarForceEquip(slot));
    }

    public int getStarForceEquipSpellTraceBonus(Equip equip) {
        int level = equip.getLevel();
        int itemId = equip.getItemId();

        int itemRequirementLevel = getReqLevel(itemId);
        boolean isSuperiorEquip = isSuperior(itemId);

        int spellTraceCost = 0;

        // less than level because it is cost up to the current level
        for (int i = 1; i < level; i++) {
            spellTraceCost += getSpellTraceCost(i, itemRequirementLevel, isSuperiorEquip);
        }

        // return 30% of Spell Trace cost rounded up to the next whole number
        return (int) Math.ceil(spellTraceCost * 0.30);
    }

    public StarBoost getStarForceEquipCraftingBonus(Equip equip) {
        int level = equip.getLevel();

        StarBoost levelStatBonus = getStarForceEquipLevelBonus(equip, level);

        StarBoost weaponStat = new StarBoost(equip.getWatk(), equip.getMatk(), equip.getStr(), equip.getDex(), equip.getLuk(), equip.getInt());

        StarBoost weaponStatBonus = weaponStat.boostPercentage();

        return levelStatBonus.add(weaponStat).add(weaponStatBonus);
    }

    public StarBoost getStarForceEquipLevelBonus(Equip equip, int level) {
        int starForceBoost = level / 2;
        int watkAdd = 2 * starForceBoost;
        int matkAdd = 2 * starForceBoost;
        int strAdd = 2 * starForceBoost;
        int dexAdd = 2 * starForceBoost;
        int lukAdd = 2 * starForceBoost;
        int intAdd = 2 * starForceBoost;

        int additionalStarForceBoost = level / 4;
        watkAdd += additionalStarForceBoost;
        matkAdd += additionalStarForceBoost;

        return new StarBoost(watkAdd, matkAdd, strAdd, dexAdd, lukAdd, intAdd);
    }

    public Equip applyStarForceEquipCraftingBonus(Equip equip, int slot) {
        Equip equipItem = getStarForceEquip(slot);

        StarBoost starBoostBonus = getStarForceEquipCraftingBonus(equipItem);

        equip.setWatk(getShortMaxIfOverflow(equip.getWatk() + starBoostBonus.getWatk()));
        equip.setMatk(getShortMaxIfOverflow(equip.getMatk() + starBoostBonus.getMatk()));
        equip.setStr(getShortMaxIfOverflow(equip.getStr() + starBoostBonus.getStr()));
        equip.setDex(getShortMaxIfOverflow(equip.getDex() + starBoostBonus.getDex()));
        equip.setLuk(getShortMaxIfOverflow(equip.getLuk() + starBoostBonus.getLuk()));
        equip.setInt(getShortMaxIfOverflow(equip.getInt() + starBoostBonus.getInt()));

        InventoryManipulator.removeFromSlot(c, equipItem.getInventoryType(), equipItem.getPosition(), equipItem.getQuantity(), false);
        gainItem(4000999, getShortMaxIfOverflow(getStarForceEquipSpellTraceBonus(equip)));

        return equip;
    }

    public String getStarForceEquipSlot() {
        StringBuilder sb = new StringBuilder();
        assert getChar() != null;
        var equip = getChar().getInventory(InventoryType.EQUIP).orderedBySlot();
        int i = 0;
        for (Item item : equip) {
            i++;
//            if (i > 4) {
//                sb.append("\r\n");
//                i = 1;
//            }
            Equip equippedItem = (Equip) item;
            if (equippedItem.getUpgradeSlots() == 0) {
                continue;
            }
            //#
            sb.append("#L").append(item.getPosition()).append("#");
            sb.append("#i").append(item.getItemId()).append("#");
            sb.append("#z").append(item.getItemId()).append("#").append(" (+" + ((Equip) item).getLevel() +")");
            sb.append("#l\r\n");
        }
        return sb.toString();
    }

    public ItemInformationProvider getMIIP() {
        return ItemInformationProvider.getInstance();
    }

    public Item getSFItem(int slot) {
        return Objects.requireNonNull(getInventory(InventoryType.EQUIP).getItem((short) slot), "Item not found in provided slow.");
    }

    public Equip getStarForceEquip(int slot) {
        Item item = getSFItem(slot);

        return (Equip) item;
    }

    public boolean doesShopExist(int shopId) {
        return ShopFactory.getInstance().getShop(shopId) != null;
    }

    public void addShopItem(int shopId, int itemId, int price) {
        ShopFactory.getInstance().getShop(shopId).getItems().add(new ShopItem((short) 1, itemId, price, 0));
        try (Connection con = DatabaseConnection.getConnection()) {
            int position = 100;
            try (PreparedStatement ps = con.prepareStatement("SELECT MAX(`position`) FROM `shopitems` WHERE `shopid` = ?")) {
                ps.setInt(1, shopId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        position = rs.getInt(1);
                        position += 1;
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO shopitems(`shopid`, `itemid`, `price`, `position`) VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, shopId);
                ps.setInt(2, itemId);
                ps.setInt(3, price);
                ps.setInt(4, position);
                ps.execute();
            }
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
        }
    }

    public void addShopToDb(int shopId, int npcId) {
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO shops(`shopid`, `npcid`) VALUES (?, ?)")) {
                ps.setInt(1, shopId);
                ps.setInt(2, npcId);
                ps.execute();
                ShopFactory.getInstance().reloadShops();
            }
        } catch (SQLException sqle) {
            log.error(sqle.getMessage());
        }
    }

    public boolean isStoredItemsEmpty() {
        List<Map.Entry<Integer, Integer>> storedItems = getStoredItemsWithAmount();
        return storedItems.isEmpty();  // Returns true if the list is empty, false otherwise
    }


    //This community is trash
    /*public List<Map.Entry<Integer, Integer>> getStoredItemsWithAmount() {
        List<Map.Entry<Integer, Integer>> items = new ArrayList<>();  // List to hold item ID and amount pairs

        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT itemid, amount FROM infinitystorage WHERE charid = ?")) {
                ps.setInt(1, getPlayer().getId());  // assuming getId() returns the charid of the player
                try (ResultSet rs = ps.executeQuery()) {
                    // Iterate over the result set to collect item ID and amount
                    while (rs.next()) {
                        int itemId = rs.getInt("itemid");  // Get the item ID
                        int amount = rs.getInt("amount");  // Get the amount
                        items.add(new AbstractMap.SimpleEntry<>(itemId, amount));  // Store as a pair (itemId, amount)
                    }
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage());  // replace with your logging mechanism
        }

        return items;  // return the list of item ID and amount pairs
    }*/

    public List<Map.Entry<Integer, Integer>>getStoredItemsWithAmount() {
        getPlayer().getInfinityFromDB();
        return getPlayer().getInfinityStorageAsList();
    }

    public String formatStoredItems(String type) {
        List<Map.Entry<Integer, Integer>> storedItems = getStoredItemsWithAmount();
        List<Map.Entry<Integer, Integer>> filteredStoredItems = new ArrayList<>();

        if(type.equalsIgnoreCase("consume")) {
            for (Map.Entry<Integer, Integer> entry : storedItems) {
                InventoryType entryType = getEquipById(entry.getKey()).getInventoryType();
                if(entryType == InventoryType.USE) filteredStoredItems.add(entry);
            }
        }
        if(type.equalsIgnoreCase("etc")) {
            for (Map.Entry<Integer, Integer> entry : storedItems) {
                InventoryType entryType = getEquipById(entry.getKey()).getInventoryType();
                if(entryType == InventoryType.ETC) filteredStoredItems.add(entry);
            }
        }
        if(type.equalsIgnoreCase("setup")) {
            for (Map.Entry<Integer, Integer> entry : storedItems) {
                InventoryType entryType = getEquipById(entry.getKey()).getInventoryType();
                if(entryType == InventoryType.SETUP) filteredStoredItems.add(entry);
            }
        }

        //for (Map.Entry<Integer, Integer> item : storedItems) {
          //  System.out.println("Stored Item ID: " + item.getKey() + ", Amount: " + item.getValue());
        //}
        int i = 0;
        int itemCounter = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("\r\n");
        for (Map.Entry<Integer, Integer> it : filteredStoredItems) {
            Item item = getEquipById(it.getKey()).copy();
            i++;
            if (i > 4) {
                sb.append("\r\n");
                i = 1;
            }
            sb.append("#L" + itemCounter).append("##i").append(item.getItemId()).append(":#(").append(it.getValue()).append(")#l");
            itemCounter++;
        }
        return sb.toString();
    }

    public Integer getSelectedStoredItemId(int index, String type) {
        List<Map.Entry<Integer, Integer>> storedItems = getStoredItemsWithAmount();
        List<Map.Entry<Integer, Integer>> filteredStoredItems = new ArrayList<>();

        if(type.equalsIgnoreCase("consume")) {
            for (Map.Entry<Integer, Integer> entry : storedItems) {
                InventoryType entryType = getEquipById(entry.getKey()).getInventoryType();
                if(entryType == InventoryType.USE) filteredStoredItems.add(entry);
            }
        }
        if(type.equalsIgnoreCase("etc")) {
            for (Map.Entry<Integer, Integer> entry : storedItems) {
                InventoryType entryType = getEquipById(entry.getKey()).getInventoryType();
                if(entryType == InventoryType.ETC) filteredStoredItems.add(entry);
            }
        }
        if(type.equalsIgnoreCase("setup")) {
            for (Map.Entry<Integer, Integer> entry : storedItems) {
                InventoryType entryType = getEquipById(entry.getKey()).getInventoryType();
                if(entryType == InventoryType.SETUP) filteredStoredItems.add(entry);
            }
        }
        return filteredStoredItems.get(index).getKey();
    }

    public Integer getSelectedStoredItemAmount(int index, String type){
        List<Map.Entry<Integer, Integer>> storedItems = getStoredItemsWithAmount();
        List<Map.Entry<Integer, Integer>> filteredStoredItems = new ArrayList<>();

        if(type.equalsIgnoreCase("consume")) {
            for (Map.Entry<Integer, Integer> entry : storedItems) {
                InventoryType entryType = getEquipById(entry.getKey()).getInventoryType();
                if(entryType == InventoryType.USE) filteredStoredItems.add(entry);
            }
        }
        if(type.equalsIgnoreCase("etc")) {
            for (Map.Entry<Integer, Integer> entry : storedItems) {
                InventoryType entryType = getEquipById(entry.getKey()).getInventoryType();
                if(entryType == InventoryType.ETC) filteredStoredItems.add(entry);
            }
        }
        if(type.equalsIgnoreCase("setup")) {
            for (Map.Entry<Integer, Integer> entry : storedItems) {
                InventoryType entryType = getEquipById(entry.getKey()).getInventoryType();
                if(entryType == InventoryType.SETUP) filteredStoredItems.add(entry);
            }
        }
        return filteredStoredItems.get(index).getValue();
    }

    public Item getItemByTypeAndPos(String type, int position, Character chr) {
        Item item = null;
        try {
            // Convert type ("Consume", "Etc") to the corresponding inventory type
            InventoryType invType = getInventoryTypeFromString(type);

            // Fetch the inventory of the player based on the type (Consume, Etc, etc.)
            Inventory inv = chr.getInventory(invType);

            // Retrieve the item from the inventory based on the position
            item = inv.getItem((short) position);

            // Verify the item is not null and the position matches
            if (item != null && item.getPosition() == position) {
                return item;  // Return the item if found at the correct position
            }
        } catch (Exception e) {
            log.error("Failed to retrieve item by type and position: " + e.getMessage());
        }
        return null;  // Return null if no item is found
    }

    // Helper function to convert the string type to an inventory type
    private InventoryType getInventoryTypeFromString(String type) {
        switch (type.toLowerCase()) {
            case "consume":
                return InventoryType.USE;  // Use the appropriate type in your system
            case "etc":
                return InventoryType.ETC;      // Use the appropriate type in your system
            case "setup":
                return InventoryType.SETUP;      // Use the appropriate type in your system
            default:
                return InventoryType.UNDEFINED;  // Handle unknown/unsupported inventory types
        }
    }

    /*public void updateStoredItem(int itemId, int amount, boolean clear) {
        try (Connection con = DatabaseConnection.getConnection()) {
            // Log the incoming parameters for debugging
            log.info("Updating stored item with parameters - ItemID: " + itemId + ", Amount: " + amount);

            if(clear) {
                String deleteQuery = "DELETE FROM infinitystorage WHERE charid = ? AND itemid = ?";
                try (PreparedStatement deletePs = con.prepareStatement(deleteQuery)) {
                    deletePs.setInt(1, getPlayer().getId());  // charid
                    deletePs.setInt(2, itemId);             // itemid
                    deletePs.executeUpdate();
                }
            } else {
                // Check if the item already exists in the database
                String checkQuery = "SELECT id FROM infinitystorage WHERE charid = ? AND itemid = ?";
                try (PreparedStatement checkPs = con.prepareStatement(checkQuery)) {
                    checkPs.setInt(1, getPlayer().getId());  // charid
                    checkPs.setInt(2, itemId);               // itemid

                    try (ResultSet rs = checkPs.executeQuery()) {
                        if (rs.next()) {
                            // Item exists, perform an update
                            String updateQuery = "UPDATE infinitystorage SET amount = ? WHERE charid = ? AND itemid = ?";
                            try (PreparedStatement updatePs = con.prepareStatement(updateQuery)) {
                                updatePs.setInt(1, amount);  // amount to set
                                updatePs.setInt(2, getPlayer().getId());  // charid
                                updatePs.setInt(3, itemId);  // itemid
                                updatePs.executeUpdate();
                            }
                        } else {
                            // Item does not exist, perform an insert
                            String insertQuery = "INSERT INTO infinitystorage (charid, itemid, amount) VALUES (?, ?, ?)";
                            try (PreparedStatement insertPs = con.prepareStatement(insertQuery)) {
                                insertPs.setInt(1, getPlayer().getId());  // charid
                                insertPs.setInt(2, itemId);               // itemid
                                insertPs.setInt(3, amount);               // amount
                                insertPs.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error updating or inserting stored item: " + e.getMessage());
        }
    }*/

    public void updateStoredItem(int itemId, int amount, boolean clear) {
        Character player = getPlayer();
        int playerId = player.getId();
        int currentAmount = player.getInfinityStorageEntry(itemId);
        int delta = currentAmount - amount;

        if (clear) {
            player.retrieveFromInfinityStorage(itemId, currentAmount);
        } else if (amount <= currentAmount) {
            player.retrieveFromInfinityStorage(itemId, delta);
        } else {
            player.storeInInfinityStorage(itemId, -delta);
        }
    }

    public int getStoredItemAmount(int charId, int itemId) {
        return getPlayer().getInfinityStorageEntry(itemId);
    }

    // Method to get the current stored amount from the database
/*
    public int getStoredItemAmount(int charId, int itemId) {
        int amount = 0; // Default value if no entry is found
        try (Connection con = DatabaseConnection.getConnection()) {
            // Prepare SQL query to retrieve the stored amount
            String query = "SELECT amount FROM infinitystorage WHERE charid = ? AND itemid = ?";
            try (PreparedStatement ps = con.prepareStatement(query)) {
                ps.setInt(1, charId);
                ps.setInt(2, itemId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        amount = rs.getInt("amount"); // Fetch the amount
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Handle any SQL exceptions
        }
        return amount; // Return the current amount
    }
*/



    public int getFreeSlots(Integer itemId) {
        // Convert type to corresponding inventory type
        InventoryType invType = getEquipById(itemId).getInventoryType();

        // Get player's inventory of the specified type
        Inventory inv = getPlayer().getInventory(invType);


        return inv.getNumFreeSlot();
    }




    public boolean isEndChat(int mode, int type) {
        return mode == -1 || ((type == 3 || type == 4) && mode == 0);
    }

    public ArrayList<Character> getCharacters(MapleMap map) {
        return new ArrayList<>(map.getCharacters());
    }

    public ArrayList<Portal> getPortals(MapleMap map) {
        return new ArrayList<>(map.getPortals());
    }

    public int getSFGain(int itemId, int level, boolean superior, String statType) {
        int catid = ((itemId / 10000) % 100);
        if (catid >= 30) {
            catid = 30;
        }
        if(isSuperior(itemId)) {
            int watkAdd = 4 + level;
            int matkAdd = 4 + level;
            int strAdd = 10 + level;
            int dexAdd = 10 + level;
            int lukAdd = 10 + level;
            int intAdd = 10 + level;

            if(statType.equalsIgnoreCase("str")) {
                return strAdd;
            } else if(statType.equalsIgnoreCase("dex")) {
                return dexAdd;
            } else if(statType.equalsIgnoreCase("int")) {
                return intAdd;
            } else if(statType.equalsIgnoreCase("luk")) {
                return lukAdd;
            } else if(statType.equalsIgnoreCase("watk")) {
                if(level >= 5) return watkAdd;
                else return 0;
            } else if(statType.equalsIgnoreCase("matk")) {
                if(level >= 5) return matkAdd;
                else return 0;
            }
        } else {
            if(statType.equalsIgnoreCase("str")) {
                return getStatAdditions(catid, level).getStr();
            } else if(statType.equalsIgnoreCase("dex")) {
                return getStatAdditions(catid, level).getDex();
            } else if(statType.equalsIgnoreCase("int")) {
                return getStatAdditions(catid, level).getInt();
            } else if(statType.equalsIgnoreCase("luk")) {
                return getStatAdditions(catid, level).getLuk();
            } else if(statType.equalsIgnoreCase("watk")) {
                return getStatAdditions(catid, level).getWatk();
            } else if(statType.equalsIgnoreCase("matk")) {
                return getStatAdditions(catid, level).getMatk();
            }
        }
        return 0;
    }

    public void playGlobalEventSound(String name){
        Server.getInstance().broadcastMessage(c.getWorld(), PacketCreator.playEventSound(name));
    }

    public Equip itemToEquip(Item item) {
        return (Equip) item;
    }

    public void sendMessageToPlayer(String string) {
        getClient().sendPacket(PacketCreator.serverNotice(6, string));
    }

    public void debug(String string) {
        System.out.println(string);
    }


    public Saloon getStyle(int selection) {
        return getPlayer().getStyles().get(selection);
    }
}