/*
    This file is part of the HeavenMS MapleStory Server
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
package client.creator.novice;

import client.Client;
import client.Job;
import client.creator.CharacterFactory;
import client.creator.CharacterFactoryRecipe;
import client.inventory.InventoryType;
import client.inventory.Pet;
import constants.id.ItemId;
import constants.id.MapId;

import static java.util.concurrent.TimeUnit.DAYS;

/**
 * @author RonanLana
 */
public class NoblesseCreator extends CharacterFactory {

    private static CharacterFactoryRecipe createRecipe(Job job, int level, int map, int top, int bottom, int shoes, int weapon) {
        CharacterFactoryRecipe recipe = new CharacterFactoryRecipe(job, level, map, top, bottom, shoes, weapon);
        giveItem(recipe, ItemId.BEGINNERS_GUIDE, 1, InventoryType.ETC);
        giveItem(recipe, 5180000, 1, InventoryType.CASH);
        int petid = Pet.createPet(5000529);
        long expiration = System.currentTimeMillis() + DAYS.toMillis(30);
        giveItem(recipe, 5000529, 1, InventoryType.CASH, petid, expiration );

        return recipe;
    }

    private static void giveItem(CharacterFactoryRecipe recipe, int itemid, int quantity, InventoryType itemType) {
        recipe.addStartingItem(itemid, quantity, itemType);
    }

    public static int createCharacter(Client c, String name, int face, int hair, int skin, int top, int bottom, int shoes, int weapon, int gender) {
        int status = createNewCharacter(c, name, face, hair, skin, gender, createRecipe(Job.NOBLESSE, 1, MapId.FM_ENTRANCE, top, bottom, shoes, weapon));
        return status;
    }
    private static void giveItem(CharacterFactoryRecipe recipe, int itemid, int quantity, InventoryType itemType, int petid, long expiration) {
        recipe.addStartingItem(itemid, quantity, itemType, petid, expiration);
    }
}
