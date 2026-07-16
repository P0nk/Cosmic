/*
    This file is part of the HeavenMS MapleStory Server, commands OdinMS-based
    Copyleft (L) 2016 - 2019 RonanLana

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

/*
   @Author: Arthur L - Refactored command content into modules
*/
package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import net.server.Server;
import net.server.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GmCommand extends Command {

    private static final Logger log =
            LoggerFactory.getLogger(GmCommand.class);

    private static final String DISCORD_INVITE =
            "https://discord.gg/vET9mGczw8";

    private static final int MINIMUM_MESSAGE_WORDS = 4;

    public GmCommand() {
        setDescription("List available ForgeMS staff or request GM assistance.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        List<Character> availableStaff =
                getAvailableStaff(player.getWorld());

        /*
         * No message:
         * Display all visible and available ForgeMS staff.
         */
        if (params.length == 0) {
            displayAvailableStaff(player, availableStaff);
            return;
        }

        /*
         * Message supplied:
         * Send a detailed assistance request to all visible staff.
         */
        String message = player.getLastCommandMessage();

        if (message == null) {
            showMessageUsage(player);
            return;
        }

        message = message.trim();

        if (countWords(message) < MINIMUM_MESSAGE_WORDS) {
            player.dropMessage(
                    5,
                    "Your GM request must contain at least "
                            + MINIMUM_MESSAGE_WORDS
                            + " words."
            );

            player.dropMessage(
                    5,
                    "Usage: @gm <your message>"
            );

            player.dropMessage(
                    5,
                    "Example: @gm I am stuck after rebirth"
            );

            return;
        }

        if (availableStaff.isEmpty()) {
            showNoStaffNotice(player);
            return;
        }

        sendRequestToStaff(player, availableStaff, message);

        player.dropMessage(
                5,
                "Your request has been sent to the available ForgeMS staff."
        );

        player.dropMessage(
                5,
                "Please remain available in case a staff member contacts you."
        );

        log.info(
                "GM request from {} on channel {} in map {} ({}): {}",
                Character.makeMapleReadable(player.getName()),
                c.getChannel(),
                player.getMap().getMapName(),
                player.getMapId(),
                message
        );
    }

    /**
     * Finds all visible ForgeMS staff members in the player's world.
     *
     * GM level mapping:
     * 2 = Spark
     * 3 = Ember
     * 4 = Kindling
     * 5 = FlameKeeper
     * 6 = Hearthkeeper
     */
    private List<Character> getAvailableStaff(int worldId) {
        List<Character> availableStaff = new ArrayList<>();

        for (Channel channel :
                Server.getInstance().getChannelsFromWorld(worldId)) {

            for (Character character :
                    channel.getPlayerStorage().getAllCharacters()) {

                if (!character.isGM()) {
                    continue;
                }

                if (character.isHidden()) {
                    continue;
                }

                int gmLevel = character.getClient().getGMLevel();

                if (gmLevel < 2 || gmLevel > 6) {
                    continue;
                }

                availableStaff.add(character);
            }
        }

        availableStaff.sort(
                Comparator
                        .comparingInt(
                                (Character staff) -> staff.getClient().getGMLevel()
                        )
                        .reversed()
                        .thenComparing(
                                Character::getName,
                                String.CASE_INSENSITIVE_ORDER
                        )
        );

        return availableStaff;
    }

    /**
     * Displays the public staff list when a player enters @gm
     * without a message.
     */
    private void displayAvailableStaff(
            Character player,
            List<Character> availableStaff
    ) {
        if (availableStaff.isEmpty()) {
            showNoStaffNotice(player);
            return;
        }

        player.dropMessage(
                5,
                "========== ForgeMS Staff Available =========="
        );

        for (Character staffMember : availableStaff) {
            player.dropMessage(
                    5,
                    "["
                            + getForgeRank(staffMember.getClient().getGMLevel())
                            + "] "
                            + Character.makeMapleReadable(
                            staffMember.getName()
                    )
            );
        }

        player.dropMessage(
                5,
                "============================================="
        );

        player.dropMessage(
                5,
                "Need assistance? Use: @gm <your message>"
        );

        player.dropMessage(
                5,
                "Example: @gm I am stuck after rebirth"
        );
    }

    /**
     * Sends the player's request to every visible available GM.
     */
    private void sendRequestToStaff(
            Character player,
            List<Character> availableStaff,
            String message
    ) {
        String playerName =
                Character.makeMapleReadable(player.getName());

        String mapName = player.getMap().getMapName();
        int mapId = player.getMapId();
        int channelId = player.getClient().getChannel();

        for (Character staffMember : availableStaff) {
            staffMember.dropMessage(
                    6,
                    "========== ForgeMS GM Request =========="
            );

            staffMember.dropMessage(
                    6,
                    "Player: " + playerName
            );

            staffMember.dropMessage(
                    6,
                    "Channel: " + channelId
            );

            staffMember.dropMessage(
                    6,
                    "Map: " + mapName + " (" + mapId + ")"
            );

            staffMember.dropMessage(
                    6,
                    "Message: " + message
            );

            staffMember.dropMessage(
                    6,
                    "========================================="
            );
        }
    }

    /**
     * Shows the offline-staff Discord notice.
     */
    private void showNoStaffNotice(Character player) {
        player.dropMessage(
                5,
                "No GM is online at the moment. Please visit the Discord "
                        + "to notify one of your needs."
        );

        player.dropMessage(
                5,
                "ForgeMS Discord: " + DISCORD_INVITE
        );
    }

    /**
     * Shows correct command usage.
     */
    private void showMessageUsage(Character player) {
        player.dropMessage(
                5,
                "Usage: @gm"
        );

        player.dropMessage(
                5,
                "Usage: @gm <message of at least four words>"
        );

        player.dropMessage(
                5,
                "Example: @gm I am stuck after rebirth"
        );
    }

    /**
     * Counts words separated by one or more spaces.
     */
    private int countWords(String message) {
        if (message == null || message.trim().isEmpty()) {
            return 0;
        }

        return message.trim().split("\\s+").length;
    }

    /**
     * Converts Cosmic GM levels into ForgeMS staff ranks.
     */
    private String getForgeRank(int gmLevel) {
        switch (gmLevel) {
            case 2:
                return "Spark";

            case 3:
                return "Ember";

            case 4:
                return "Kindling";

            case 5:
                return "FlameKeeper";

            case 6:
                return "Hearthkeeper";

            default:
                return "Staff";
        }
    }
}