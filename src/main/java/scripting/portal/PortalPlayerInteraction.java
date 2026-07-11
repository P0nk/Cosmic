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
package scripting.portal;

import client.Client;
import scripting.AbstractPlayerInteraction;
import scripting.map.MapScriptManager;
import server.maps.Portal;
import tools.DatabaseConnection;
import tools.PacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PortalPlayerInteraction extends AbstractPlayerInteraction {
    private final Portal portal;

    public PortalPlayerInteraction(Client c, Portal portal) {
        super(c);
        this.portal = portal;
    }

    public Portal getPortal() {
        return portal;
    }

    public void runMapScript() {
        MapScriptManager msm = MapScriptManager.getInstance();
        msm.runMapScript(c, "onUserEnter/" + portal.getScriptName(), false);
    }

    public boolean hasLevel30Character() {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT `level` FROM `characters` WHERE accountid = ?")) {
            ps.setInt(1, getPlayer().getAccountID());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (rs.getInt("level") >= 30) {
                        return true;
                    }
                }
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        return getPlayer().getLevel() >= 30;
    }

    public void blockPortal() {
        c.getPlayer().blockPortal(getPortal().getScriptName());
    }

    public void unblockPortal() {
        c.getPlayer().unblockPortal(getPortal().getScriptName());
    }

    public void playPortalSound() {
        c.sendPacket(PacketCreator.playPortalSound());
    }

    // Define the cycle start hour (24-hour format)
    private static final int START_HOUR = 0; // Example: cycle starts at 9:00 AM
    private static final int CYCLE_MINUTES = 60; // 1 hour = 60 minutes
    private static final int WINDOW_DURATION = 5; // 5-minute window

    public boolean isDoorOpen() {
        // Get the current time in milliseconds
        long currentTimeMillis = System.currentTimeMillis();

        // Convert milliseconds to total minutes since Unix epoch
        long totalMinutes = currentTimeMillis / (1000 * 60);

        // Calculate the minutes at which the cycle should start (from the Unix epoch)
        // Convert START_HOUR into minutes (since midnight)
        long startTimeInMinutes = START_HOUR * 60;

        // Adjust the total minutes relative to the start time
        long adjustedMinutes = totalMinutes - startTimeInMinutes;

        // Each 3-hour interval is 180 minutes
        long minutesInCurrentCycle = adjustedMinutes % 60;

        // Return true if within the first 5 minutes of the adjusted 3-hour window
        return minutesInCurrentCycle >= 0 && minutesInCurrentCycle < 5;
    }

    // Method to calculate the time until the next door opens
    public long timeUntilNextOpen() {
        long currentTimeMillis = System.currentTimeMillis();
        long totalMinutes = currentTimeMillis / (1000 * 60); // Convert to minutes
        long startTimeInMinutes = START_HOUR * 60; // Convert START_HOUR to minutes
        long adjustedMinutes = totalMinutes - startTimeInMinutes;
        long minutesInCurrentCycle = adjustedMinutes % CYCLE_MINUTES;

        if (minutesInCurrentCycle < WINDOW_DURATION) {
            // If we're already in the 5-minute window, return 0
            return 0;
        } else {
            // Calculate the remaining time until the next cycle starts
            long remainingMinutes = CYCLE_MINUTES - minutesInCurrentCycle;
            return remainingMinutes;
        }
    }


}