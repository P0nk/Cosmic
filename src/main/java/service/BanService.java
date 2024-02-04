package service;

import client.Character;
import client.autoban.AutobanFactory;
import config.YamlConfig;
import net.packet.Packet;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.TimerManager;
import tools.PacketCreator;

import java.util.concurrent.TimeUnit;

public class BanService {
    private static final Logger log = LoggerFactory.getLogger(BanService.class);
    private final TransitionService transitionService;

    public BanService(TransitionService transitionService) {
        this.transitionService = transitionService;
    }

    public void autoban(Character chr, AutobanFactory type, String reason) {
        autoban(chr, "Autobanned for (" + type.name() + ": " + reason + ")");
    }

    private void autoban(Character chr, String reason) {
        if (isExempt(chr)) {
            return;
        }

        chr.ban(reason);

        chr.sendPacket(PacketCreator.sendPolice("You have been blocked by the#b %s Police for HACK reason.#k".formatted("Cosmic")));
        TimerManager.getInstance().schedule(() -> transitionService.disconnect(chr.getClient(), false, false),
                TimeUnit.SECONDS.toMillis(5));

        var bannedName = Character.makeMapleReadable(chr.getName());
        Packet autobanGmNotice = PacketCreator.serverNotice(6, bannedName + " was autobanned for " + reason);
        Server.getInstance().broadcastGMMessage(chr.getWorld(), autobanGmNotice);
    }

    public void addPoint(Character chr, AutobanFactory type, String reason) {
        if (isExempt(chr)) {
            return;
        }

        var autobanManager = chr.getAutobanManager();
        boolean shouldAutoban = autobanManager.addPoint(type);
        if (shouldAutoban) {
            autoban(chr, type, reason);
        }

        if (YamlConfig.config.server.USE_AUTOBAN_LOG) {
            log.info("Autoban - chr {} caused {} {}", Character.makeMapleReadable(chr.getName()), type.name(), reason);
        }
    }

    private boolean isExempt(Character chr) {
        return !YamlConfig.config.server.USE_AUTOBAN || chr.isGM() || chr.isBanned();
    }
}
