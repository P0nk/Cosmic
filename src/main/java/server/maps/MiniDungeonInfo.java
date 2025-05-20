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
package server.maps;

import constants.id.MapId;

/**
 * @author Alan (SharpAceX)
 */

public enum MiniDungeonInfo {

    //http://bbb.hidden-street.net/search_finder/mini%20dungeon

    ZAKUM(MapId.DOOR_TO_ZAKUM, MapId.ZAKUM_ALTAR, 5), // Zakum
    CAVE_OF_MUSHROOMS(MapId.ANT_TUNNEL_2, MapId.CAVE_OF_MUSHROOMS_BASE, 5), // Horny Mushroom, Zombie Mushroom
    GOLEM_CASTLE_RUINS(MapId.SLEEPY_DUNGEON_4, MapId.GOLEMS_CASTLE_RUINS_BASE, 34), // Stone Golem, Mixed Golem
    HILL_OF_SANDSTORMS(MapId.SAHEL_2, MapId.HILL_OF_SANDSTORMS_BASE, 30), // Sand Rat
    HENESYS_PIG_FARM(MapId.RAIN_FOREST_EAST_OF_HENESYS, MapId.HENESYS_PIG_FARM_BASE, 30), // Pig, Ribbon Pig
    DRAKES_BLUE_CAVE(MapId.COLD_CRADLE, MapId.DRAKES_BLUE_CAVE_BASE, 30), // Dark Drake
    DRUMMER_BUNNYS_LAIR(MapId.EOS_TOWER_76TH_TO_90TH_FLOOR, MapId.DRUMMER_BUNNYS_LAIR_BASE, 30), // Drumming Bunny
    THE_ROUND_TABLE_OF_KENTARUS(MapId.BATTLEFIELD_OF_FIRE_AND_WATER, MapId.ROUND_TABLE_OF_KENTAURUS_BASE, 30), // Blue/Red/Black Kentaurus
    THE_RESTORING_MEMORY(MapId.DRAGON_NEST_LEFT_BEHIND, MapId.RESTORING_MEMORY_BASE, 19), // Skelegon, Skelosaurus
    NEWT_SECURED_ZONE(MapId.DESTROYED_DRAGON_NEST, MapId.NEWT_SECURED_ZONE_BASE, 19), // Jr. Newtie, Transforming Jr. Newtie
    PILLAGE_OF_TREASURE_ISLAND(MapId.RED_NOSE_PIRATE_DEN_2, MapId.PILLAGE_OF_TREASURE_ISLAND_BASE, 30), // Captain
    CRITICAL_ERROR(MapId.LAB_AREA_C1, MapId.CRITICAL_ERROR_BASE, 30), // Roid
    LONGEST_RIDE_ON_BYEBYE_STATION(MapId.FANTASY_THEME_PARK_3, MapId.LONGEST_RIDE_ON_BYEBYE_STATION, 19); // Froscola, Jester Scarlion

    private final int baseId;
    private final int dungeonId;
    private final int dungeons;

    MiniDungeonInfo(int baseId, int dungeonId, int dungeons) {
        this.baseId = baseId;
        this.dungeonId = dungeonId;
        this.dungeons = dungeons;
    }

    public int getBase() {
        return baseId;
    }

    public int getDungeonId() {
        return dungeonId;
    }

    public int getDungeons() {
        return dungeons;
    }

    public static boolean isDungeonMap(int map) {
        for (MiniDungeonInfo dungeon : MiniDungeonInfo.values()) {
            if (map >= dungeon.getDungeonId() && map <= dungeon.getDungeonId() + dungeon.getDungeons()) {
                return true;
            }
        }
        return false;
    }

    public static MiniDungeonInfo getDungeon(int map) {
        for (MiniDungeonInfo dungeon : MiniDungeonInfo.values()) {
            if (map >= dungeon.getDungeonId() && map <= dungeon.getDungeonId() + dungeon.getDungeons()) {
                return dungeon;
            }
        }
        return null;
    }
}
