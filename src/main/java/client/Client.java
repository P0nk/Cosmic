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
package client;

import config.YamlConfig;
import database.account.Account;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import net.PacketHandler;
import net.PacketProcessor;
import net.netty.DisconnectException;
import net.netty.GameViolationException;
import net.netty.InvalidPacketHeaderException;
import net.packet.InPacket;
import net.packet.Packet;
import net.packet.logging.LoggingUtil;
import net.packet.logging.MonitoredChrLogger;
import net.server.Server;
import net.server.channel.Channel;
import net.server.coordinator.login.LoginBypassCoordinator;
import net.server.coordinator.session.Hwid;
import net.server.coordinator.session.SessionCoordinator;
import net.server.world.Party;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.AbstractPlayerInteraction;
import scripting.event.EventManager;
import scripting.npc.NPCConversationManager;
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestActionManager;
import scripting.quest.QuestScriptManager;
import server.TimerManager;
import server.life.Monster;
import tools.PacketCreator;

import javax.script.ScriptEngine;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.SECONDS;

public class Client extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(Client.class);
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final int MAX_CHR_SLOTS = 15;

    private final Type type;
    private final long sessionId;
    private final PacketProcessor packetProcessor;

    private Hwid hwid;
    private String remoteAddress;

    private io.netty.channel.Channel ioChannel;
    private Account account;
    private Character player;
    private int channel = 1;
    private int accId = -4;
    private boolean loggedIn = false;
    private boolean inServerTransition = false;
    private LocalDate birthday = null;
    private String accountName = null;
    private int world;
    private volatile long lastPong;
    private int gmlevel; // TODO: remove? There's a gmlevel in Character too.
    private Set<String> macs = new HashSet<>();
    private Map<String, ScriptEngine> engines = new HashMap<>();
    private byte characterSlots = 3;
    private byte failedLoginAttempts = 0;
    private String pin = "";
    private int pinattempt = 0;
    private String pic = "";
    private int picattempt = 0;
    private byte csattempt = 0;
    private byte gender = -1;
    private boolean disconnecting = false;
    private final Semaphore actionsSemaphore = new Semaphore(7);
    private final Lock lock = new ReentrantLock(true);
    private final Lock announcerLock = new ReentrantLock(true);
    // thanks Masterrulax & try2hack for pointing out a bottleneck issue with shared locks, shavit for noticing an opportunity for improvement
    private long lastNpcClick;
    private long lastPacket = System.currentTimeMillis();

    public enum Type {
        LOGIN,
        CHANNEL
    }

    public Client(Type type, long sessionId, String remoteAddress, PacketProcessor packetProcessor, int world, int channel) {
        this.type = type;
        this.sessionId = sessionId;
        this.remoteAddress = remoteAddress;
        this.packetProcessor = packetProcessor;
        this.world = world;
        this.channel = channel;
    }

    public static Client createLoginClient(long sessionId, String remoteAddress, PacketProcessor packetProcessor,
                                           int world, int channel) {
        return new Client(Type.LOGIN, sessionId, remoteAddress, packetProcessor, world, channel);
    }

    public static Client createChannelClient(long sessionId, String remoteAddress, PacketProcessor packetProcessor,
                                             int world, int channel) {
        return new Client(Type.CHANNEL, sessionId, remoteAddress, packetProcessor, world, channel);
    }

    public static Client createMock() {
        return new Client(null, -1, null, null, -123, -123);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        final io.netty.channel.Channel channel = ctx.channel();
        if (!Server.getInstance().isOnline()) {
            channel.close();
            return;
        }

        this.remoteAddress = getRemoteAddress(channel);
        this.ioChannel = channel;
    }

    private static String getRemoteAddress(io.netty.channel.Channel channel) {
        String remoteAddress = "null";
        try {
            remoteAddress = ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
        } catch (NullPointerException npe) {
            log.warn("Unable to get remote address for client", npe);
        }

        return remoteAddress;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof InPacket packet)) {
            log.warn("Received invalid message: {}", msg);
            return;
        }

        short opcode = packet.readShort();
        final PacketHandler handler = packetProcessor.getHandler(opcode);

        if (YamlConfig.config.server.USE_DEBUG_SHOW_RCVD_PACKET && !LoggingUtil.isIgnoredRecvPacket(opcode)) {
            log.debug("Received packet id {}", opcode);
        }

        if (handler != null && handler.validateState(this)) {
            try {
                MonitoredChrLogger.logPacketIfMonitored(this, opcode, packet.getBytes());
                handler.handlePacket(packet, this);
            } catch (GameViolationException gve) {
                log.warn("Game violation (disconnecting): {}", gve.getMessage());
                throw new DisconnectException(this, true);
            } catch (final Throwable t) {
                final String chrInfo = player != null ? player.getName() + " on map " + player.getMapId() : "?";
                log.warn("Error in packet handler {}. Chr {}, account {}. Packet: {}", handler.getClass().getSimpleName(),
                        chrInfo, getAccountName(), packet, t);
                //client.sendPacket(PacketCreator.enableActions());//bugs sometimes
            }
        }

        updateLastPacket();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) {
        if (event instanceof IdleStateEvent idleEvent) {
            checkIfIdle(idleEvent);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (player != null) {
            log.warn("Exception caught by {}", player, cause);
        }

        if (cause instanceof InvalidPacketHeaderException) {
            SessionCoordinator.getInstance().closeSession(this, true);
        } else {
            ctx.fireExceptionCaught(cause);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        switch (type) {
            case LOGIN -> SessionCoordinator.getInstance().closeLoginSession(this);
            case CHANNEL -> SessionCoordinator.getInstance().closeSession(this, false);
        }

        try {
            // client freeze issues on session transition states found thanks to yolinlin, Omo Oppa, Nozphex
            if (!inServerTransition) {
                ctx.fireExceptionCaught(new DisconnectException(this, false));
            }
        } catch (Throwable t) {
            log.warn("Account stuck", t);
        } finally {
            closeSession();
        }
    }

    public void updateLastPacket() {
        lastPacket = System.currentTimeMillis();
    }

    public long getLastPacket() {
        return lastPacket;
    }

    public void closeSession() {
        ioChannel.close();
    }

    public void disconnectSession() {
        ioChannel.disconnect();
    }

    public Hwid getHwid() {
        return hwid;
    }

    public void setHwid(Hwid hwid) {
        this.hwid = hwid;
    }

    public void setMacs(String macs) {
        if (macs == null || macs.isEmpty()) {
            throw new IllegalArgumentException("macs cannot be empty");
        }
        this.macs.addAll(Arrays.asList(macs.split(", ")));
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public EventManager getEventManager(String event) {
        return getChannelServer().getEventSM().getEventManager(event);
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        Objects.requireNonNull(account);
        this.account = account;
        this.accId = account.id();
        this.accountName = account.name();
        this.characterSlots = account.chrSlots();
        this.pin = account.pin();
        this.pic = account.pic();
        this.gender = Objects.requireNonNullElse(account.gender(), Gender.NOT_SET);
        this.birthday = account.birthdate();
        loggedIn = account.loginState() == LoginState.LOGGED_IN;
        inServerTransition = account.loginState() == LoginState.SERVER_TRANSITION;
    }

    public Character getPlayer() {
        return player;
    }

    public void setPlayer(Character player) {
        this.player = player;
    }

    public AbstractPlayerInteraction getAbstractPlayerInteraction() {
        return new AbstractPlayerInteraction(this);
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public boolean isInTransition() {
        return inServerTransition;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getPin() {
        return pin;
    }

    public boolean checkPin(String other) {
        if (!(YamlConfig.config.server.ENABLE_PIN && !canBypassPin())) {
            return true;
        }

        if (++pinattempt >= MAX_FAILED_LOGIN_ATTEMPTS) {
            SessionCoordinator.getInstance().closeSession(this, false);
        }
        if (pin.equals(other)) {
            pinattempt = 0;
            LoginBypassCoordinator.getInstance().registerLoginBypassEntry(hwid, accId, false);
            return true;
        }
        return false;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public String getPic() {
        return pic;
    }

    public boolean checkPic(String other) {
        if (!(YamlConfig.config.server.ENABLE_PIC && !canBypassPic())) {
            return true;
        }

        if (++picattempt >= MAX_FAILED_LOGIN_ATTEMPTS) {
            SessionCoordinator.getInstance().closeSession(this, false);
        }
        if (pic.equals(other)) {    // thanks ryantpayton (HeavenClient) for noticing null pics being checked here
            picattempt = 0;
            LoginBypassCoordinator.getInstance().registerLoginBypassEntry(hwid, accId, true);
            return true;
        }
        return false;
    }

    public boolean attemptLogin() {
        return ++failedLoginAttempts < MAX_FAILED_LOGIN_ATTEMPTS;
    }

    public boolean wasRecentlyBanned() {
        Instant recentThreshold = Instant.now().minus(Duration.ofDays(30));
        return account != null && account.bannedUntil() != null && account.bannedUntil().isAfter(recentThreshold);
    }

    public void setAccID(int id) {
        this.accId = id;
    }

    public int getAccID() {
        return accId;
    }

    public void onChangedLoginState(LoginState newState) {
        switch (newState) {
            case LoginState.LOGGED_OUT -> {
                loggedIn = false;
                inServerTransition = false;
            }
            case LoginState.SERVER_TRANSITION -> {
                loggedIn = false;
                inServerTransition = true;
            }
            case LoginState.LOGGED_IN -> {
                loggedIn = true;
                inServerTransition = false;
            }
        }
    }

    public boolean checkBirthDate(Calendar date) {
        LocalDate toCheck = LocalDate.of(date.get(Calendar.YEAR), date.get(Calendar.MONTH),
                date.get(Calendar.DAY_OF_MONTH));
        return Objects.equals(birthday, toCheck);
    }

    public synchronized boolean tryDisconnect() {
        if (disconnecting) {
            return false;
        }

        disconnecting = true;
        return true;
    }

    public void clear() {
        // player hard reference removal thanks to Steve (kaito1410)
        if (this.player != null) {
            this.player.empty(true); // clears schedules and stuff
        }

        Server.getInstance().unregisterLoginState(this);

        this.accountName = null;
        this.macs = null;
        this.hwid = null;
        this.birthday = null;
        this.engines = null;
        this.player = null;
    }

    public void clearEngines() {
        if (engines != null) {
            engines.clear();
        }
    }

    public int getChannel() {
        return channel;
    }

    public Channel getChannelServer() {
        return Server.getInstance().getChannel(world, channel);
    }

    public World getWorldServer() {
        return Server.getInstance().getWorld(world);
    }

    public Channel getChannelServer(byte channel) {
        return Server.getInstance().getChannel(world, channel);
    }

    public boolean deleteCharacter(int cid, int senderAccId) {
        try {
            Character chr = Character.loadCharFromDB(cid, this, false);

            Integer partyid = chr.getWorldServer().getCharacterPartyid(cid);
            if (partyid != null) {
                this.setPlayer(chr);

                Party party = chr.getWorldServer().getParty(partyid);
                chr.setParty(party);
                chr.getMPC();
                chr.leaveParty();   // thanks Vcoc for pointing out deleted characters would still stay in a party

                this.setPlayer(null);
            }

            return Character.deleteCharFromDB(chr, senderAccId);
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String a) {
        this.accountName = a;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getWorld() {
        return world;
    }

    public void setWorld(int world) {
        this.world = world;
    }

    public void pongReceived() {
        lastPong = System.currentTimeMillis();
    }

    public void checkIfIdle(final IdleStateEvent event) {
        final long pingedAt = System.currentTimeMillis();
        sendPacket(PacketCreator.getPing());
        TimerManager.getInstance().schedule(() -> {
            try {
                if (lastPong < pingedAt) {
                    if (ioChannel.isActive()) {
                        log.info("Disconnected {} due to idling. Reason: {}", remoteAddress, event.state());
                        disconnectSession();
                    }
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }, SECONDS.toMillis(15));
    }

    public Set<String> getMacs() {
        return Collections.unmodifiableSet(macs);
    }

    public int getGMLevel() {
        return gmlevel;
    }

    public void setGMLevel(int level) {
        gmlevel = level;
    }

    public void setScriptEngine(String name, ScriptEngine e) {
        engines.put(name, e);
    }

    public ScriptEngine getScriptEngine(String name) {
        return engines.get(name);
    }

    public void removeScriptEngine(String name) {
        engines.remove(name);
    }

    public NPCConversationManager getCM() {
        return NPCScriptManager.getInstance().getCM(this);
    }

    public QuestActionManager getQM() {
        return QuestScriptManager.getInstance().getQM(this);
    }

    public void lockClient() {
        lock.lock();
    }

    public void unlockClient() {
        lock.unlock();
    }

    public boolean tryacquireClient() {
        if (actionsSemaphore.tryAcquire()) {
            lockClient();
            return true;
        } else {
            return false;
        }
    }

    public void releaseClient() {
        unlockClient();
        actionsSemaphore.release();
    }

    public short getAvailableCharacterSlots() {
        return (short) Math.max(0, characterSlots - Server.getInstance().getAccountCharacterCount(accId));
    }

    public short getAvailableCharacterWorldSlots() {
        return (short) Math.max(0, characterSlots - Server.getInstance().getAccountWorldCharacterCount(accId, world));
    }

    public short getAvailableCharacterWorldSlots(int world) {
        return (short) Math.max(0, characterSlots - Server.getInstance().getAccountWorldCharacterCount(accId, world));
    }

    public short getCharacterSlots() {
        return characterSlots;
    }

    public void setCharacterSlots(byte slots) {
        characterSlots = slots;
    }

    public boolean canGainCharacterSlot() {
        return characterSlots < MAX_CHR_SLOTS;
    }

    public boolean gainCharacterSlot() {
        if (canGainCharacterSlot()) {
            characterSlots++;
            return true;
        }
        return false;
    }

    public byte getGender() {
        return gender;
    }

    public void setGender(byte gender) {
        if (gender != Gender.MALE && gender != Gender.FEMALE) {
            throw new IllegalArgumentException("Invalid gender: " + gender);
        }
        this.gender = gender;
    }

    private void announceDisableServerMessage() {
        if (!this.getWorldServer().registerDisabledServerMessage(player.getId())) {
            sendPacket(PacketCreator.serverMessage(""));
        }
    }

    public void announceServerMessage() {
        sendPacket(PacketCreator.serverMessage(this.getChannelServer().getServerMessage()));
    }

    public synchronized void announceBossHpBar(Monster mm, final int mobHash, Packet packet) {
        long timeNow = System.currentTimeMillis();
        int targetHash = player.getTargetHpBarHash();

        if (mobHash != targetHash) {
            if (timeNow - player.getTargetHpBarTime() >= SECONDS.toMillis(5)) {
                // is there a way to INTERRUPT this annoying thread running on the client that drops the boss bar after some time at every attack?
                announceDisableServerMessage();
                sendPacket(packet);

                player.setTargetHpBarHash(mobHash);
                player.setTargetHpBarTime(timeNow);
            }
        } else {
            announceDisableServerMessage();
            sendPacket(packet);

            player.setTargetHpBarTime(timeNow);
        }
    }

    public void sendPacket(Packet packet) {
        announcerLock.lock();
        try {
            ioChannel.writeAndFlush(packet);
        } finally {
            announcerLock.unlock();
        }
    }

    public void announceHint(String msg, int length) {
        sendPacket(PacketCreator.sendHint(msg, length, 10));
        sendPacket(PacketCreator.enableActions());
    }

    public long getSessionId() {
        return this.sessionId;
    }

    public boolean canClickNPC() {
        return lastNpcClick + 500 < Server.getInstance().getCurrentTime();
    }

    public void setClickedNPC() {
        lastNpcClick = Server.getInstance().getCurrentTime();
    }

    public void removeClickedNPC() {
        lastNpcClick = 0;
    }

    public void closePlayerScriptInteractions() {
        this.removeClickedNPC();
        NPCScriptManager.getInstance().dispose(this);
        QuestScriptManager.getInstance().dispose(this);
    }

    public boolean attemptCsCoupon() {
        if (csattempt > 2) {
            resetCsCoupon();
            return false;
        }

        csattempt++;
        return true;
    }

    public void resetCsCoupon() {
        csattempt = 0;
    }

    public void enableCSActions() {
        sendPacket(PacketCreator.enableCSUse(player));
    }

    public boolean canBypassPin() {
        return LoginBypassCoordinator.getInstance().canLoginBypass(hwid, accId, false);
    }

    public boolean canBypassPic() {
        return LoginBypassCoordinator.getInstance().canLoginBypass(hwid, accId, true);
    }
}
