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
import tools.Randomizer;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class ViewAllCharRegisterPicHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(ViewAllCharRegisterPicHandler.class);

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
    public final void handlePacket(InPacket p, Client c) {
        p.readByte();
        int charId = p.readInt();
        p.readInt(); // please don't let the client choose which world they should login

        String mac = p.readString();
        String hostString = p.readString();

        final Hwid hwid;
        try {
            hwid = Hwid.fromHostString(hostString);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid host string: {}", hostString, e);
            c.sendPacket(PacketCreator.getAfterLoginError(17));
            return;
        }

        c.updateMacs(mac);
        c.updateHwid(hwid);

        String ip = c.getRemoteIP();
        if (ip != null && !ip.equals("null")) {
            c.updateIP(ip);
        }

        if (c.hasBannedMac() || c.hasBannedHWID()) {
            SessionCoordinator.getInstance().closeSession(c, true);
            return;
        }

        AntiMulticlientResult res = SessionCoordinator.getInstance().attemptGameSession(c, c.getAccID(), hwid);
        if (res != AntiMulticlientResult.SUCCESS) {
            c.sendPacket(PacketCreator.getAfterLoginError(parseAntiMulticlientError(res)));
            return;
        }

        Server server = Server.getInstance();
        if (!server.haveCharacterEntry(c.getAccID(), charId)) {
            SessionCoordinator.getInstance().closeSession(c, true);
            return;
        }

        c.setWorld(server.getCharacterWorld(charId));
        World wserv = c.getWorldServer();
        if (wserv == null || wserv.isWorldCapacityFull()) {
            c.sendPacket(PacketCreator.getAfterLoginError(10));
            return;
        }

        int channel = Randomizer.rand(1, server.getWorld(c.getWorld()).getChannelsSize());
        c.setChannel(channel);

        String pic = p.readString();
        c.setPic(pic);

        String[] socket = server.getInetSocket(c, c.getWorld(), channel);
        if (socket == null) {
            c.sendPacket(PacketCreator.getAfterLoginError(10));
            return;
        }

        server.unregisterLoginState(c);
        c.setCharacterOnSessionTransitionState(charId);

        try {
            c.sendPacket(PacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), charId));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
