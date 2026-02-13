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
package net.server.channel.handlers;

import client.BuddyList;
import client.BuddylistEntry;
import client.Character;
import client.Client;
import client.Disease;
import client.Family;
import client.FamilyEntry;
import client.LoginState;
import client.Mount;
import client.SkillFactory;
import client.inventory.Equip;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.Pet;
import client.keybind.KeyBinding;
import client.keybind.QuickslotBinding;
import config.YamlConfig;
import constants.game.GameConstants;
import database.account.Account;
import database.character.CharacterLoader;
import model.CharacterIdentity;
import net.AbstractPacketHandler;
import net.netty.GameViolationException;
import net.packet.InPacket;
import net.server.PlayerBuffValueHolder;
import net.server.Server;
import net.server.channel.Channel;
import net.server.channel.CharacterIdChannelPair;
import net.server.coordinator.session.Hwid;
import net.server.coordinator.session.SessionCoordinator;
import net.server.coordinator.world.EventRecallCoordinator;
import net.server.guild.Alliance;
import net.server.guild.Guild;
import net.server.guild.GuildPackets;
import net.server.world.PartyCharacter;
import net.server.world.PartyOperation;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.event.EventInstanceManager;
import server.life.MobSkill;
import service.AccountService;
import service.NoteService;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.Pair;
import tools.packets.WeddingPackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class PlayerLoggedinHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(PlayerLoggedinHandler.class);
    private static final Set<Integer> attemptingLoginAccounts = new HashSet<>();

    private final CharacterLoader chrLoader;
    private final AccountService accountService;
    private final NoteService noteService;

    public PlayerLoggedinHandler(CharacterLoader chrLoader, AccountService accountService, NoteService noteService) {
        this.chrLoader = chrLoader;
        this.accountService = accountService;
        this.noteService = noteService;
    }

    private boolean tryAcquireAccount(int accId) {
        synchronized (attemptingLoginAccounts) {
            if (attemptingLoginAccounts.contains(accId)) {
                return false;
            }

            attemptingLoginAccounts.add(accId);
            return true;
        }
    }

    private void releaseAccount(int accId) {
        synchronized (attemptingLoginAccounts) {
            attemptingLoginAccounts.remove(accId);
        }
    }

    @Override
    public final boolean validateState(Client c) {
        return !c.isLoggedIn();
    }

    @Override
    public final void handlePacket(InPacket p, Client c) {
        final int chrId = p.readInt();
        final Server server = Server.getInstance();

        if (!c.tryacquireClient()) {
            // thanks MedicOP for assisting on concurrency protection here
            c.sendPacket(PacketCreator.getAfterLoginError(10));
        }

        try {
            World wserv = server.getWorld(c.getWorld());
            if (wserv == null) {
                throw new GameViolationException("World not found");
            }

            Channel cserv = wserv.getChannel(c.getChannel());
            if (cserv == null) {
                c.setChannel(1);
                cserv = wserv.getChannel(c.getChannel());

                if (cserv == null) {
                    throw new GameViolationException("Channel not found");
                }
            }

            Character player = wserv.getPlayerStorage().getCharacterById(chrId);

            final Hwid hwid;
            if (player == null) {
                hwid = SessionCoordinator.getInstance().pickLoginSessionHwid(c);
                if (hwid == null) {
                    throw new GameViolationException("Unable to pick hwid");
                }
            } else {
                hwid = player.getClient().getHwid();
            }

            c.setHwid(hwid);

            if (!server.validateCharacteridInTransition(c, chrId)) {
                throw new GameViolationException("Attempt to enter game without chr id in transition");
            }

            boolean newlyLoggedIn = false;
            if (player == null) {
                Optional<Character> loadedChr = chrLoader.loadForChannel(chrId, c);
                if (loadedChr.isPresent()) {
                    player = loadedChr.get();
                    newlyLoggedIn = true;
                } else {
                    throw new GameViolationException("Unable to load chr");
                }
            }
            c.setPlayer(player);

            Optional<Account> foundAccount = accountService.getAccount(player.getAccountID());
            if (foundAccount.isEmpty()) {
                c.sendPacket(PacketCreator.getAfterLoginError(5));
                return;
            }
            Account account = foundAccount.get();
            c.setAccount(account);

            int accId = account.id();
            if (!tryAcquireAccount(accId)) { // Sync this to prevent wrong login state for double loggedin handling
                c.setPlayer(null);
                c.setAccID(0);
                c.sendPacket(PacketCreator.getAfterLoginError(10));
                return;
            }

            try {
                LoginState state = account.loginState();
                if (state != LoginState.SERVER_TRANSITION) {
                    c.setPlayer(null);
                    c.setAccID(0);

                    if (state == LoginState.LOGGED_IN) {
                        throw new GameViolationException("Attempt to log in when already logged in");
                    } else {
                        c.sendPacket(PacketCreator.getAfterLoginError(7));
                    }

                    return;
                }
                accountService.setLoggedIn(c);
            } finally {
                releaseAccount(accId);
            }

            if (!newlyLoggedIn) {
                c.setCharacterSlots((byte) player.getClient().getCharacterSlots());
                player.newClient(c);
            }

            cserv.addPlayer(player);
            wserv.addPlayer(player);
            player.setEnteredChannelWorld();

            List<PlayerBuffValueHolder> buffs = server.getPlayerBuffStorage().getBuffsFromStorage(chrId);
            if (buffs != null) {
                List<Pair<Long, PlayerBuffValueHolder>> timedBuffs = getLocalStartTimes(buffs);
                player.silentGiveBuffs(timedBuffs);
            }

            Map<Disease, Pair<Long, MobSkill>> diseases = server.getPlayerBuffStorage().getDiseasesFromStorage(chrId);
            if (diseases != null) {
                player.silentApplyDiseases(diseases);
            }

            c.sendPacket(PacketCreator.getCharInfo(player));
            if (!player.isHidden()) {
                if (player.isGM() && YamlConfig.config.server.USE_AUTOHIDE_GM) {
                    player.toggleHide(true);
                }
            }
            player.sendKeymap();
            sendQuickmap(player);
            player.sendMacros();

            // pot bindings being passed through other characters on the account detected thanks to Croosade dev team
            KeyBinding autohpPot = player.getKeymap().get(91);
            player.sendPacket(PacketCreator.sendAutoHpPot(autohpPot != null ? autohpPot.getAction() : 0));

            KeyBinding autompPot = player.getKeymap().get(92);
            player.sendPacket(PacketCreator.sendAutoMpPot(autompPot != null ? autompPot.getAction() : 0));

            player.getMap().addPlayer(player);
            player.visitMap(player.getMap());

            BuddyList bl = player.getBuddylist();
            int[] buddyIds = bl.getBuddyIds();
            wserv.loggedOn(player.getName(), player.getId(), c.getChannel(), buddyIds);
            for (CharacterIdChannelPair onlineBuddy : wserv.multiBuddyFind(player.getId(), buddyIds)) {
                BuddylistEntry ble = bl.get(onlineBuddy.getCharacterId());
                ble.setChannel(onlineBuddy.getChannel());
                bl.put(ble);
            }
            c.sendPacket(PacketCreator.updateBuddylist(bl.getBuddies()));

            c.sendPacket(PacketCreator.loadFamily(player));
            if (player.getFamilyId() > 0) {
                Family f = wserv.getFamily(player.getFamilyId());
                if (f != null) {
                    FamilyEntry familyEntry = f.getEntryByID(player.getId());
                    if (familyEntry != null) {
                        familyEntry.setCharacter(player);
                        player.setFamilyEntry(familyEntry);

                        c.sendPacket(PacketCreator.getFamilyInfo(familyEntry));
                        familyEntry.announceToSenior(PacketCreator.sendFamilyLoginNotice(player.getName(), true), true);
                    } else {
                        log.error("Chr {}'s family doesn't have an entry for them. (familyId {})", player.getName(), f.getID());
                    }
                } else {
                    log.error("Chr {} has an invalid family ID ({})", player.getName(), player.getFamilyId());
                    c.sendPacket(PacketCreator.getFamilyInfo(null));
                }
            } else {
                c.sendPacket(PacketCreator.getFamilyInfo(null));
            }

            if (player.getGuildId() > 0) {
                Guild playerGuild = server.getGuild(player.getGuildId(), player.getWorld(), player);
                if (playerGuild == null) {
                    player.deleteGuild(player.getGuildId());
                    player.getMGC().setGuildId(0);
                    player.getMGC().setGuildRank(5);
                } else {
                    playerGuild.getMGC(player.getId()).setCharacter(player);
                    player.setMGC(playerGuild.getMGC(player.getId()));
                    server.setGuildMemberOnline(player, true, c.getChannel());
                    c.sendPacket(GuildPackets.showGuildInfo(player));
                    int allianceId = player.getGuild().getAllianceId();
                    if (allianceId > 0) {
                        Alliance newAlliance = server.getAlliance(allianceId);
                        if (newAlliance == null) {
                            newAlliance = Alliance.loadAlliance(allianceId);
                            if (newAlliance != null) {
                                server.addAlliance(allianceId, newAlliance);
                            } else {
                                player.getGuild().setAllianceId(0);
                            }
                        }
                        if (newAlliance != null) {
                            c.sendPacket(GuildPackets.updateAllianceInfo(newAlliance, c.getWorld()));
                            c.sendPacket(GuildPackets.allianceNotice(newAlliance.getId(), newAlliance.getNotice()));

                            if (newlyLoggedIn) {
                                server.allianceMessage(allianceId, GuildPackets.allianceMemberOnline(player, true), player.getId(), -1);
                            }
                        }
                    }
                }
            }

            noteService.show(player);

            if (player.getParty() != null) {
                PartyCharacter pchar = player.getMPC();

                //Use this in case of enabling party HPbar HUD when logging in, however "you created a party" will appear on chat.
                //c.sendPacket(PacketCreator.partyCreated(pchar));

                pchar.setChannel(c.getChannel());
                pchar.setMapId(player.getMapId());
                pchar.setOnline(true);
                wserv.updateParty(player.getParty().getId(), PartyOperation.LOG_ONOFF, pchar);
                player.updatePartyMemberHP();
            }

            Inventory eqpInv = player.getInventory(InventoryType.EQUIPPED);
            eqpInv.lockInventory();
            try {
                for (Item it : eqpInv.list()) {
                    player.equippedItem((Equip) it);
                }
            } finally {
                eqpInv.unlockInventory();
            }

            c.sendPacket(PacketCreator.updateBuddylist(player.getBuddylist().getBuddies()));

            CharacterIdentity pendingBuddyRequest = c.getPlayer().getBuddylist().pollPendingRequest();
            if (pendingBuddyRequest != null) {
                c.sendPacket(PacketCreator.requestBuddylistAdd(pendingBuddyRequest.id(), c.getPlayer().getId(), pendingBuddyRequest.name()));
            }

            c.sendPacket(PacketCreator.updateGender(player));
            player.checkMessenger();
            c.sendPacket(PacketCreator.enableReport());
            player.changeSkillLevel(SkillFactory.getSkill(10000000 * player.getJobType() + 12), (byte) (player.getLinkedLevel() / 10), 20, -1);
            player.checkBerserk(player.isHidden());

            if (newlyLoggedIn) {
                for (Pet pet : player.getPets()) {
                    if (pet != null) {
                        wserv.registerPetHunger(player, player.getPetIndex(pet));
                    }
                }

                Mount mount = player.getMount();   // thanks Ari for noticing a scenario where Silver Mane quest couldn't be started
                if (mount.getItemId() != 0) {
                    player.sendPacket(PacketCreator.updateMount(player.getId(), mount, false));
                }

                player.reloadQuestExpirations();

                if (player.isGM()) {
                    Server.getInstance().broadcastGMMessage(c.getWorld(), PacketCreator.earnTitleMessage((player.gmLevel() < 6 ? "GM " : "Admin ") + player.getName() + " has logged in"));
                }

                if (diseases != null) {
                    for (Entry<Disease, Pair<Long, MobSkill>> e : diseases.entrySet()) {
                        final List<Pair<Disease, Integer>> debuff = Collections.singletonList(new Pair<>(e.getKey(), e.getValue().getRight().getX()));
                        c.sendPacket(PacketCreator.giveDebuff(debuff, e.getValue().getRight()));
                    }
                }
            } else {
                if (player.isRidingBattleship()) {
                    player.announceBattleshipHp();
                }
            }

            player.buffExpireTask();
            player.diseaseExpireTask();
            player.skillCooldownTask();
            player.expirationTask();
            player.questExpirationTask();
            if (GameConstants.hasSPTable(player.getJob()) && player.getJob().getId() != 2001) {
                player.createDragon();
            }

            player.commitExcludedItems();
            showDueyNotification(c, player);

            player.resetPlayerRates();
            if (YamlConfig.config.server.USE_ADD_RATES_BY_LEVEL) {
                player.setPlayerRates();
            }

            player.setWorldRates();
            player.updateCouponRates();

            player.receivePartyMemberHP();

            if (player.getPartnerId() > 0) {
                int partnerId = player.getPartnerId();
                final Character partner = wserv.getPlayerStorage().getCharacterById(partnerId);

                if (partner != null && !partner.isAwayFromWorld()) {
                    player.sendPacket(WeddingPackets.OnNotifyWeddingPartnerTransfer(partnerId, partner.getMapId()));
                    partner.sendPacket(WeddingPackets.OnNotifyWeddingPartnerTransfer(player.getId(), player.getMapId()));
                }
            }

            if (newlyLoggedIn) {
                EventInstanceManager eim = EventRecallCoordinator.getInstance().recallEventInstance(chrId);
                if (eim != null) {
                    eim.registerPlayer(player);
                }
            }

            // Tell the client to use the custom scripts available for the NPCs provided, instead of the WZ entries.
            if (YamlConfig.config.server.USE_NPCS_SCRIPTABLE) {

                // Create a copy to prevent always adding entries to the server's list.
                Map<Integer, String> npcsIds = YamlConfig.config.server.NPCS_SCRIPTABLE
                        .entrySet().stream().collect(Collectors.toMap(
                                entry -> Integer.parseInt(entry.getKey()),
                                Entry::getValue
                        ));

                c.sendPacket(PacketCreator.setNPCScriptable(npcsIds));
            }

            if (newlyLoggedIn) {
                player.setLoginTime(System.currentTimeMillis());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            c.releaseClient();
        }
    }

    private static void showDueyNotification(Client c, Character player) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT Type FROM dueypackages WHERE ReceiverId = ? AND Checked = 1 ORDER BY Type DESC")) {
            ps.setInt(1, player.getId());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement ps2 = con.prepareStatement("UPDATE dueypackages SET Checked = 0 WHERE ReceiverId = ?")) {
                        ps2.setInt(1, player.getId());
                        ps2.executeUpdate();

                        c.sendPacket(PacketCreator.sendDueyParcelNotification(rs.getInt("Type") == 1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void sendQuickmap(Character chr) {
        QuickslotBinding quickslotBinding = chr.getQuickslotBinding();
        if (quickslotBinding == null) {
            quickslotBinding = new QuickslotBinding(QuickslotBinding.DEFAULT_QUICKSLOTS);
        }

        chr.sendPacket(PacketCreator.QuickslotMappedInit(quickslotBinding));
    }

    private static List<Pair<Long, PlayerBuffValueHolder>> getLocalStartTimes(List<PlayerBuffValueHolder> lpbvl) {
        List<Pair<Long, PlayerBuffValueHolder>> timedBuffs = new ArrayList<>();
        long curtime = currentServerTime();

        for (PlayerBuffValueHolder pb : lpbvl) {
            timedBuffs.add(new Pair<>(curtime - pb.usedTime, pb));
        }

        timedBuffs.sort((p1, p2) -> p1.getLeft().compareTo(p2.getLeft()));

        return timedBuffs;
    }
}
