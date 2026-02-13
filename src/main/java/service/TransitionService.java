package service;

import client.BuddyList;
import client.Client;
import client.inventory.InventoryType;
import config.YamlConfig;
import constants.id.MapId;
import database.character.CharacterSaver;
import net.netty.LoginServer;
import net.server.Server;
import net.server.coordinator.session.SessionCoordinator;
import net.server.guild.Guild;
import net.server.guild.GuildCharacter;
import net.server.guild.GuildPackets;
import net.server.world.MessengerCharacter;
import net.server.world.Party;
import net.server.world.PartyCharacter;
import net.server.world.PartyOperation;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.event.EventInstanceManager;
import server.ThreadManager;
import server.maps.FieldLimit;
import server.maps.MapleMap;
import server.maps.MiniDungeonInfo;
import tools.PacketCreator;

import java.io.IOException;
import java.net.InetAddress;

public class TransitionService {
    private static final Logger log = LoggerFactory.getLogger(TransitionService.class);
    private final Server server = Server.getInstance();
    private final CharacterSaver chrSaver;
    private final AccountService accountService;

    public TransitionService(CharacterSaver characterSaver, AccountService accountService) {
        this.chrSaver = characterSaver;
        this.accountService = accountService;
    }

    public void changeChannel(Client c, int channel) {
        var chr = c.getPlayer();
        if (chr.isBanned()) {
            disconnect(c, false);
            return;
        }

        if (!chr.isAlive() || FieldLimit.CANNOTMIGRATE.check(chr.getMap().getFieldLimit())) {
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        if (MiniDungeonInfo.isDungeonMap(chr.getMapId())) {
            c.sendPacket(PacketCreator.serverNotice(5, "Changing channels or entering Cash Shop or MTS are disabled when inside a Mini-Dungeon."));
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        String[] socket = Server.getInstance().getInetSocket(c, c.getWorld(), channel);
        if (socket == null) {
            c.sendPacket(PacketCreator.serverNotice(1, "Channel " + channel + " is currently disabled. Try another channel."));
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        chr.closePlayerInteractions();
        chr.closePartySearchInteractions();

        chr.unregisterChairBuff();
        server.getPlayerBuffStorage().addBuffsToStorage(chr.getId(), chr.getAllBuffs());
        server.getPlayerBuffStorage().addDiseasesToStorage(chr.getId(), chr.getAllDiseases());
        chr.setDisconnectedFromChannelWorld();
        chr.notifyMapTransferToPartner(-1);
        chr.removeIncomingInvites();
        chr.cancelAllBuffs(true);
        chr.cancelAllDebuffs();
        chr.cancelBuffExpireTask();
        chr.cancelDiseaseExpireTask();
        chr.cancelSkillCooldownTask();
        chr.cancelQuestExpirationTask();
        //Cancelling magicdoor? Nope
        //Cancelling mounts? Noty

        chr.getInventory(InventoryType.EQUIPPED).checked(false); //test
        chr.getMap().removePlayer(chr);
        c.getChannelServer().removePlayer(chr);

        chrSaver.save(chr);

        setInTransition(chr.getClient(), chr.getId());
        try {
            c.sendPacket(PacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setInTransition(Client c, int chrId) {
        accountService.setInTransition(c);
        Server.getInstance().setCharacteridInTransition(c, chrId);
    }

    public void disconnect(final Client c, final boolean shutdown) {
        if (c.tryDisconnect()) {
            ThreadManager.getInstance().newTask(() -> disconnectInternal(c, shutdown));
        }
    }

    public void forceDisconnect(Client c) {
        if (c.tryDisconnect()) {
            disconnectInternal(c, true);
        }
    }

    private void disconnectInternal(Client c, boolean shutdown) {
        final var chr = c.getPlayer();
        if (chr != null && chr.isLoggedin() && chr.getClient() != null) {
            chr.cancelMagicDoor();

            final World wserv = c.getWorldServer();   // obviously wserv is NOT null if this chr was online on it
            try {
                removePlayer(c, wserv, c.isInTransition());

                final int channel = c.getChannel();
                if (!(channel == LoginServer.CHANNEL_ID || shutdown)) {
                    if (!c.isInTransition()) { // meaning not changing channels
                        final int messengerid = chr.getMessenger() == null ? 0 : chr.getMessenger().getId();
                        if (messengerid > 0) {
                            wserv.leaveMessenger(messengerid, new MessengerCharacter(chr, 0));
                        }

                        chr.forfeitExpirableQuests();    //This is for those quests that you have to stay logged in for a certain amount of time

                        Guild guild = chr.getGuild();
                        if (guild != null) {
                            final Server server = Server.getInstance();
                            server.setGuildMemberOnline(chr, false, chr.getClient().getChannel());
                            chr.sendPacket(GuildPackets.showGuildInfo(chr));
                        }

                        BuddyList buddyList = chr.getBuddylist();
                        if (buddyList != null) {
                            wserv.loggedOff(chr.getName(), chr.getId(), channel, chr.getBuddylist().getBuddyIds());
                        }
                    }
                }
            } catch (final Exception e) {
                log.error("Account stuck", e);
            } finally {
                if (!c.isInTransition()) {
                    final GuildCharacter guildChr = chr.getMGC();
                    if (guildChr != null) {
                        guildChr.setCharacter(null);
                    }
                    wserv.removePlayer(chr);
                    //getChannelServer().removePlayer(player); already being done

                    chr.cancelAllDebuffs();
                    chrSaver.save(chr);

                    chr.logOff();
                    if (YamlConfig.config.server.INSTANT_NAME_CHANGE) {
                        chr.doPendingNameChange();
                    }
                    c.clear();
                } else {
                    c.getChannelServer().removePlayer(chr);

                    chr.cancelAllDebuffs();
                    chrSaver.save(chr);
                }
            }
        }

        SessionCoordinator.getInstance().closeSession(c, shutdown);


        if (!c.isInTransition() && c.isLoggedIn()) {
            accountService.setLoggedOut(c);
            c.clear();
        } else {
            if (!Server.getInstance().hasCharacteridInTransition(c)) {
                accountService.setLoggedOut(c);
            }

            c.clearEngines();
        }
    }

    private void removePlayer(Client c, World world, boolean serverTransition) {
        var chr = c.getPlayer();
        try {
            chr.setDisconnectedFromChannelWorld();
            chr.notifyMapTransferToPartner(-1);
            chr.removeIncomingInvites();
            chr.cancelAllBuffs(true);

            chr.closePlayerInteractions();
            chr.closePartySearchInteractions();

            if (!serverTransition) {    // thanks MedicOP for detecting an issue with party leader change on changing channels
                removePartyPlayer(c, world);

                EventInstanceManager eim = chr.getEventInstance();
                if (eim != null) {
                    eim.playerDisconnected(chr);
                }

                if (chr.getMonsterCarnival() != null) {
                    chr.getMonsterCarnival().playerDisconnected(chr.getId());
                }

                if (chr.getAriantColiseum() != null) {
                    chr.getAriantColiseum().playerDisconnected(chr);
                }
            }

            if (chr.getMap() != null) {
                int mapId = chr.getMapId();
                chr.getMap().removePlayer(chr);
                if (MapId.isDojo(mapId)) {
                    c.getChannelServer().freeDojoSectionIfEmpty(mapId);
                }

                if (chr.getMap().getHPDec() > 0) {
                    world.removePlayerHpDecrease(chr);
                }
            }

        } catch (final Throwable t) {
            log.error("Account stuck", t);
        }
    }

    private void removePartyPlayer(Client c, World world) {
        var chr = c.getPlayer();
        MapleMap map = chr.getMap();
        final Party party = chr.getParty();
        final int idz = chr.getId();

        if (party != null) {
            final PartyCharacter chrp = new PartyCharacter(chr);
            chrp.setOnline(false);
            world.updateParty(party.getId(), PartyOperation.LOG_ONOFF, chrp);
            if (party.getLeader().getId() == idz && map != null) {
                PartyCharacter lchr = null;
                for (PartyCharacter pchr : party.getMembers()) {
                    if (pchr != null && pchr.getId() != idz && (lchr == null || lchr.getLevel() <= pchr.getLevel()) && map.getCharacterById(pchr.getId()) != null) {
                        lchr = pchr;
                    }
                }
                if (lchr != null) {
                    world.updateParty(party.getId(), PartyOperation.CHANGE_LEADER, lchr);
                }
            }
        }
    }
}
