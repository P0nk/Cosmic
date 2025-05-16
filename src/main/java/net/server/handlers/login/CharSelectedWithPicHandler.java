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
            log.warn("Invalid host string: {}", hostString, e);
            c.sendPacket(PacketCreator.getAfterLoginError(17));
            return;
        }

        c.updateMacs(macs);
        c.updateHwid(hwid);

        if (c.hasBannedMac() || c.hasBannedHWID()) {
            System.out.println("Pin hasBannedHWID");
            SessionCoordinator.getInstance().closeSession(c, true);
            return;
        }

        Server server = Server.getInstance();
        if (!server.haveCharacterEntry(c.getAccID(), charId)) {
            System.out.println("Pin haveCharacterEntry");
            SessionCoordinator.getInstance().closeSession(c, true);
            return;
        }

        if (c.checkPic(pic)) {
            c.setWorld(server.getCharacterWorld(charId));
            World wserv = c.getWorldServer();
            if (wserv == null || wserv.isWorldCapacityFull()) {
                System.out.println("Pin isWorldCapacityFull");
                c.sendPacket(PacketCreator.getAfterLoginError(10));
                return;
            }

            String[] socket = server.getInetSocket(c, c.getWorld(), c.getChannel());
            if (socket == null) {
                System.out.println("Pin socket == null");
                c.sendPacket(PacketCreator.getAfterLoginError(10));
                return;
            }

            AntiMulticlientResult res = SessionCoordinator.getInstance().attemptGameSession(c, c.getAccID(), hwid);
            if (res != AntiMulticlientResult.SUCCESS) {
                System.out.println("Pin AntiMulticlientResult");
                c.sendPacket(PacketCreator.getAfterLoginError(parseAntiMulticlientError(res)));
                return;
            }

            server.unregisterLoginState(c);
            c.setCharacterOnSessionTransitionState(charId);

            try {
                System.out.println("Pin Try Statement");
                c.sendPacket(PacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), charId));
            } catch (UnknownHostException | NumberFormatException e) {
                System.out.println("Pin Catch Statement");
                e.printStackTrace();
            }
        } else {
            System.out.println("Pin else Statement");
            c.sendPacket(PacketCreator.wrongPic());
        }
    }
}
