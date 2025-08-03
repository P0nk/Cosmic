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
package server.shop;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import constants.id.ItemId;
import constants.inventory.ItemConstants;
import database.shop.ShopDao;
import database.shop.ShopItem;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Matze
 * @author Ponk
 */
public class ShopFactory {
    private static final short MAX_QUANTITY_PER_PURCHASE = 1000; // Should really use max stack size for the given item
    private static final Set<Integer> rechargeableItemIds = rechargeableItemIds();
    private final Cache<Integer, Optional<Shop>> shops = Caffeine.newBuilder().build();
    private final ShopDao shopDao;

    public ShopFactory(ShopDao shopDao) {
        this.shopDao = shopDao;
    }

    private static Set<Integer> rechargeableItemIds() {
        IntStream stars = ItemId.allThrowingStarIds();
        IntStream ammo = IntStream.concat(ItemId.allBulletIds(), ItemId.allBulletCapsuleIds());
        return IntStream.concat(stars, ammo)
                .boxed()
                .collect(Collectors.toUnmodifiableSet());
    }

    public Optional<Shop> getShop(int shopId) {
        return shops.get(shopId, this::loadShop);
    }

    private Optional<Shop> loadShop(int shopId) {
        Optional<database.shop.Shop> dbShop = shopDao.getShop(shopId);
        if (dbShop.isEmpty()) {
            return Optional.empty();
        }
        List<ShopItem> items = shopDao.getShopItems(shopId);
        return Optional.of(new Shop(dbShop.get().id(), dbShop.get().npcId(), fromDbShopItems(items)));
    }

    private List<server.shop.ShopItem> fromDbShopItems(List<ShopItem> dbItems) {
        Stream<server.shop.ShopItem> purchaseableItems = dbItems.stream()
                .map(dbItem -> {
                    short buyable = ItemConstants.isRechargeable(dbItem.itemId()) ? (short) 1 : MAX_QUANTITY_PER_PURCHASE;
                    int pitch = dbItem.pitch() == null ? 0 : dbItem.pitch();
                    return new server.shop.ShopItem(buyable, dbItem.itemId(), dbItem.price(), pitch);
                });
        Stream<server.shop.ShopItem> rechargeableItems = rechargeableItemIds.stream()
                .map(rechItem -> new server.shop.ShopItem((short) 0, rechItem, 0, 0));
        return Stream.concat(purchaseableItems, rechargeableItems).toList();
    }

    public void reloadShops() {
        shops.invalidateAll();
    }
}
