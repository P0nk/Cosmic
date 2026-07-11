package net.server.channel.handlers;

import client.Character;
import client.Client;
import client.autoban.AutobanFactory;
import client.command.CommandsExecutor;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ChatLogger;
import tools.PacketCreator;

public final class GeneralChatHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(GeneralChatHandler.class);

    @Override
    public void handlePacket(InPacket p, Client c) {
        String s = p.readString();
        Character chr = c.getPlayer();
        if (chr.getAutobanManager().getLastSpam(7) + 200 > currentServerTime()) {
            c.sendPacket(PacketCreator.enableActions());
            return;
        }
        if (s.length() > Byte.MAX_VALUE && !chr.isGM()) {
            AutobanFactory.PACKET_EDIT.alert(c.getPlayer(), c.getPlayer().getName() + " tried to packet edit in General Chat.");
            log.warn("Chr {} tried to send text with length of {}", c.getPlayer().getName(), s.length());
            c.disconnect(true, false);
            return;
        }
        char heading = s.charAt(0);
        if (CommandsExecutor.isCommand(c, s)) {
            CommandsExecutor.getInstance().handle(c, s);
        } else if (heading != '/') {
            int show = p.readByte();
            if (chr.getMap().isMuted() && !chr.isGM()) {
                chr.dropMessage(5, "The map you are in is currently muted. Please try again later.");
                return;
            }
            if (!chr.isHidden()) {
                if(chr.getWorldChatOnOff()) {
                    Server.getInstance().sendGlobal(c.getWorld(), PacketCreator.serverNotice(6, chr.getName() +  " [Ch." + chr.getChannel().getId() + "]: " + s));
                    ChatLogger.log(c, "Global", s);
                } else {
                    chr.getMap().broadcastMessage(PacketCreator.getChatText(chr.getId(), s, chr.getWhiteChat(), show));
                    ChatLogger.log(c, "General", s);
                }
            } else {
                if(chr.getWorldChatOnOff()) {
                    Server.getInstance().sendGlobal(c.getWorld(), PacketCreator.serverNotice(6, chr.getName() +  " [Ch." + chr.getChannel().getId() + "]: " + s));
                    ChatLogger.log(c, "GM Global", s);
                } else {
                    chr.getMap().broadcastGMMessage(PacketCreator.getChatText(chr.getId(), s, chr.getWhiteChat(), show));
                    ChatLogger.log(c, "GM General", s);
                }
            }

            chr.getAutobanManager().spam(7);
        }
    }
}