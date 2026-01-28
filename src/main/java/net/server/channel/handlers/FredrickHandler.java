/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>
 */
package net.server.channel.handlers;

import client.Character;
import client.Client;
import client.processor.npc.FredrickProcessor;
import net.AbstractPacketHandler;
import net.packet.InPacket;

/**
 * @author kevintjuh93
 */
public class FredrickHandler extends AbstractPacketHandler {
    private final FredrickProcessor fredrickProcessor;

    public FredrickHandler(FredrickProcessor fredrickProcessor) {
        this.fredrickProcessor = fredrickProcessor;
    }

    @Override
    public void handlePacket(InPacket p, Client c) {
        Character chr = c.getPlayer();
        byte operation = p.readByte();

        System.out.println("[FredrickHandler-DEBUG] Received packet from: " + chr.getName() + " | Operation: 0x" + String.format("%02X", operation));

        switch (operation) {
            case 0x19: // Will never come...
                System.out.println("[FredrickHandler-DEBUG] Operation 0x19 (Unused/Check) triggered.");
                //c.sendPacket(PacketCreator.getFredrick((byte) 0x24));
                break;
            case 0x1A: // Retrieve Items
                System.out.println("[FredrickHandler-DEBUG] Operation 0x1A (Retrieve) triggered. Calling processor...");
                fredrickProcessor.fredrickRetrieveItems(c);
                break;
            case 0x1C: // Exit
                System.out.println("[FredrickHandler-DEBUG] Operation 0x1C (Exit) triggered.");
                break;
            default:
                System.out.println("[FredrickHandler-DEBUG] Unknown Operation: 0x" + String.format("%02X", operation));
        }
    }
}