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
   @Author: Resinate
*/
package client.command.commands.gm2;

import client.Client;
import client.command.Command;
import client.command.CommandContext;
import server.maps.MapItem;
import server.maps.MapObject;
import server.maps.MapObjectType;

import java.util.Arrays;
import java.util.List;

public class LootCommand extends Command {

    {
        setDescription("Loots all items that belong to you.");
    }

    @Override
    public void execute(Client c, String[] params, CommandContext ctx) {
        List<MapObject> items = c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapObjectType.ITEM));
        for (MapObject item : items) {
            MapItem mapItem = (MapItem) item;
            if (mapItem.getOwnerId() == c.getPlayer().getId() || mapItem.getOwnerId() == c.getPlayer().getPartyId()) {
                c.getPlayer().pickupItem(mapItem);
            }
        }

    }
}
