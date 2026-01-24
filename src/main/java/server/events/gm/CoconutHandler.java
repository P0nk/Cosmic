/*
    This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
               Matthias Butz <matze@odinms.de>
               Jan Christian Meyer <vimes@odinms.de>
    ...
 */

package server.events.gm; // Moved from net.server.channel.handlers

import client.Client;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import server.maps.MapleMap;
import tools.PacketCreator;
import tools.Randomizer;

/**
 * Handles the coconut hitting mechanics for the event.
 * Located in the event package for modularity.
 * * @author kevintjuh93
 * @author Refactoring
 */
public final class CoconutHandler extends AbstractPacketHandler {

    @Override
    public void handlePacket(InPacket p, Client c) {
        /* Packet Structure:
         * Short: Coconut ID
         * Short: Position/Animation data (Unused in logic, read for offset)
         */
        int id = p.readShort();

        MapleMap map = c.getPlayer().getMap();

        // Retrieve the specific event manager
        // Ensure Coconut.java is also in server.events.gm
        Coconut event = map.getCoconut();

        // 1. Safety Check: Is there actually an event running?
        if (event == null) {
            return;
        }

        // 2. Safety Check: Does this coconut exist?
        CoconutObject nut = event.getCoconut(id);
        if (nut == null || !nut.isHittable()) {
            return;
        }

        // 3. Cooldown Check: Prevent packet spamming (macro protection)
        if (System.currentTimeMillis() < nut.getHitTime()) {
            return;
        }

        // 4. Hit Logic
        // If hit more than 2 times, 40% chance to trigger an outcome (Fall, Bomb, Stop)
        if (nut.getHits() > 2 && Randomizer.nextInt(100) < 40) {

            // Outcome A: Coconut Stops (1% Chance)
            if (Randomizer.nextInt(100) < 1 && event.getStopped() > 0) {
                nut.setHittable(false);
                event.stopCoconut();
                map.broadcastMessage(PacketCreator.hitCoconut(false, id, 1)); // 1 = Swing/Hit sound
                return;
            }

            // Prepare for Fall or Bomb
            nut.setHittable(false);
            nut.resetHits();

            // Outcome B: Bomb (5% Chance)
            if (Randomizer.nextInt(100) < 5 && event.getBombings() > 0) {
                map.broadcastMessage(PacketCreator.hitCoconut(false, id, 2)); // 2 = Bomb animation
                event.bombCoconut();
            }
            // Outcome C: Fall (Success)
            else if (event.getFalling() > 0) {
                map.broadcastMessage(PacketCreator.hitCoconut(false, id, 3)); // 3 = Fall animation
                event.fallCoconut();

                // Scoring
                if (c.getPlayer().getTeam() == 0) { // Maple Team
                    event.addMapleScore();
                    map.broadcastMessage(PacketCreator.serverNotice(5, c.getPlayer().getName() + " of Team Maple knocks down a coconut."));
                } else { // Story Team
                    event.addStoryScore();
                    map.broadcastMessage(PacketCreator.serverNotice(5, c.getPlayer().getName() + " of Team Story knocks down a coconut."));
                }
                map.broadcastMessage(PacketCreator.coconutScore(event.getMapleScore(), event.getStoryScore()));
            }
        } else {
            // Standard Hit (No fall)
            nut.hit(); // Updates internal cooldown and hit count
            map.broadcastMessage(PacketCreator.hitCoconut(false, id, 1)); // 1 = Swing animation
        }
    }
}