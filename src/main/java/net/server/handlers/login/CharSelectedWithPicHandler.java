package net.server.handlers.login;

import client.Client;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.server.Server;
import net.server.coordinator.session.Hwid;
import net.server.coordinator.session.SessionCoordinator;
import net.server.coordinator.session.SessionCoordinator.AntiMulticlientResult;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.PacketCreator;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class CharSelectedWithPicHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(CharSelectedWithPicHandler.class);

    private static int parseAntiMulticlientError(AntiMulticlientResult res) {
        return switch (res) {
            case REMOTE_PROCESSING -> 10;
            case REMOTE_LOGGEDIN -> 7;
            case REMOTE_NO_MATCH -> 17;
            case COORDINATOR_ERROR -> 8;
            default -> 9;
        };
    }

    @Override
    public void handlePacket(InPacket p, Client c) {
        String pic = p.readString();
        int charId = p.readInt();

        String macs = p.readString();
        String hostString = p.readString();

        final Hwid hwid;
        try {
            hwid = Hwid.fromHostString(hostString);
        } catch (IllegalArgumentException e) {
            log.warn("Login blocked: invalid hostString for accId={}, charId={}, ip={}. hostString={}",
                    c.getAccID(), charId, c.getRemoteIP(), hostString, e);
            c.sendPacket(PacketCreator.getAfterLoginError(17));
            return;
        }

        c.updateMacs(macs);
        c.updateHwid(hwid);

        String ip = c.getRemoteIP();
        if (ip != null && !"null".equals(ip)) {
            c.updateIP(ip);
        }

        if (c.hasBannedMac()) {
            log.warn("Login blocked: banned MAC. accId={}, charId={}, ip={}", c.getAccID(), charId, c.getRemoteIP());
            SessionCoordinator.getInstance().closeSession(c, true);
            return;
        }

        if (c.hasBannedHWID()) {
            log.warn("Login blocked: banned HWID. accId={}, charId={}, ip={}, hwid={}", c.getAccID(), charId, c.getRemoteIP(), hwid);
            SessionCoordinator.getInstance().closeSession(c, true);
            return;
        }

        Server server = Server.getInstance();
        if (!server.haveCharacterEntry(c.getAccID(), charId)) {
            log.warn("Login blocked: character not owned by account. accId={}, charId={}, ip={}", c.getAccID(), charId, c.getRemoteIP());
            SessionCoordinator.getInstance().closeSession(c, true);
            return;
        }

        if (!c.checkPic(pic)) {
            // Not an operational alert; just a normal user mistake.
            c.sendPacket(PacketCreator.wrongPic());
            return;
        }

        c.setWorld(server.getCharacterWorld(charId));
        World wserv = c.getWorldServer();
        if (wserv == null || wserv.isWorldCapacityFull()) {
            log.warn("Login blocked: world unavailable/full. accId={}, charId={}, world={}, channel={}, ip={}",
                    c.getAccID(), charId, c.getWorld(), c.getChannel(), c.getRemoteIP());
            c.sendPacket(PacketCreator.getAfterLoginError(10));
            return;
        }

        String[] socket = server.getInetSocket(c, c.getWorld(), c.getChannel());
        if (socket == null) {
            log.error("Login failed: null game socket. accId={}, charId={}, world={}, channel={}, ip={}",
                    c.getAccID(), charId, c.getWorld(), c.getChannel(), c.getRemoteIP());
            c.sendPacket(PacketCreator.getAfterLoginError(10));
            return;
        }

        AntiMulticlientResult res = SessionCoordinator.getInstance().attemptGameSession(c, c.getAccID(), hwid);
        if (res != AntiMulticlientResult.SUCCESS) {
            log.warn("Login blocked: anti-multiclient. accId={}, charId={}, ip={}, hwid={}, result={}",
                    c.getAccID(), charId, c.getRemoteIP(), hwid, res);
            c.sendPacket(PacketCreator.getAfterLoginError(parseAntiMulticlientError(res)));
            return;
        }

        server.unregisterLoginState(c);
        c.setCharacterOnSessionTransitionState(charId);

        try {
            c.sendPacket(PacketCreator.getServerIP(
                    InetAddress.getByName(socket[0]),
                    Integer.parseInt(socket[1]),
                    charId
            ));
        } catch (UnknownHostException | NumberFormatException e) {
            log.error("Login failed: invalid socket endpoint. accId={}, charId={}, world={}, channel={}, ip={}, socketHost={}, socketPort={}",
                    c.getAccID(), charId, c.getWorld(), c.getChannel(), c.getRemoteIP(),
                    (socket.length > 0 ? socket[0] : "null"),
                    (socket.length > 1 ? socket[1] : "null"),
                    e);
            c.sendPacket(PacketCreator.getAfterLoginError(10));
        }
    }
}
