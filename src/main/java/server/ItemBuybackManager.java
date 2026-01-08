package server;

import client.Character;
import client.Client;
import client.inventory.Item;
import client.inventory.InventoryType;
import client.inventory.manipulator.InventoryManipulator;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemBuybackManager {
    private static final Logger log = LoggerFactory.getLogger(ItemBuybackManager.class);
    private static ItemBuybackManager instance = null;
    private final Map<Integer, List<BuybackEntry>> playerBuybacks = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    // Configuration constants
    private static final int MAX_BUYBACK_ITEMS = 120;
    private static final long BUYBACK_EXPIRY = 60 * 60 * 1000 * 24; // 24 hour
    // private static final int MAX_TOTAL_BUYBACK_VALUE = 500_000_000; // Optional feature

    private Timer cleanupTimer;

    public static ItemBuybackManager getInstance() {
        if (instance == null) {
            instance = new ItemBuybackManager();
        }
        return instance;
    }

    private ItemBuybackManager() {
        initialize();
    }

    public void initialize() {
        // Schedule cleanup every 30 minutes
        cleanupTimer = new Timer("BuybackCleanup", true);
        cleanupTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cleanupAllExpiredEntries();
            }
        }, BUYBACK_EXPIRY / 2, BUYBACK_EXPIRY / 2);
    }

    public void addBuybackItem(Character chr, Item item, int price, short quantity) {
        if (chr == null || item == null || quantity <= 0) return;

        int itemId = item.getItemId();
        if (itemId / 10000 == 207) {
            // Throwing star — force quantity to 1
            quantity = 1;
        }

        // Copy item safely
        Item itemCopy = item.copy();
        if (itemCopy == null || itemCopy.getItemId() <= 0 || quantity <= 0) return;
        itemCopy.setQuantity(quantity);

        int charId = chr.getId();
        long expiry = System.currentTimeMillis() + BUYBACK_EXPIRY;

        lock.lock();
        try {
            List<BuybackEntry> buybacks = playerBuybacks.computeIfAbsent(charId, k -> new ArrayList<>());

            // Add new entry to front
            buybacks.add(0, new BuybackEntry(itemCopy, price, quantity, expiry));

            // Optional: Log buyback action
            //log.info("Buyback added: {} | itemId={} qty={} price={} meso", chr.getName(), itemCopy.getItemId(), quantity, price);

            // Trim if exceeding size
            if (buybacks.size() > MAX_BUYBACK_ITEMS) {
                buybacks = new ArrayList<>(buybacks.subList(0, MAX_BUYBACK_ITEMS));
                playerBuybacks.put(charId, buybacks);
            }

            // Optional: enforce total meso value cap (future enhancement)
            /*
            int totalValue = buybacks.stream().mapToInt(BuybackEntry::getPrice).sum();
            if (totalValue > MAX_TOTAL_BUYBACK_VALUE) {
                // Implement cleanup strategy if needed
            }
            */
        } finally {
            lock.unlock();
        }
    }

    public List<BuybackEntry> getBuybackItems(Character chr) {
        if (chr == null) return new ArrayList<>();

        lock.lock();
        try {
            removeExpiredEntries(chr.getId());
            List<BuybackEntry> result = playerBuybacks.get(chr.getId());
            return result != null ? new ArrayList<>(result) : new ArrayList<>();
        } finally {
            lock.unlock();
        }
    }

    public boolean buybackItem(Character chr, int index) {
        if (chr == null) return false;

        lock.lock();
        try {
            List<BuybackEntry> buybacks = playerBuybacks.get(chr.getId());
            if (buybacks == null || index < 0 || index >= buybacks.size()) {
                return false;
            }

            BuybackEntry entry = buybacks.get(index);
            if (entry.getExpiry() < System.currentTimeMillis()) {
                buybacks.remove(index);
                return false;
            }

            // Check space
            Item item = entry.getItem();
            InventoryType type = InventoryType.getByType(item.getItemType());
            if (chr.getInventory(type).getNumFreeSlot() < 1) {
                return false;
            }

            // Restore item and remove from list
            InventoryManipulator.addFromDrop(chr.getClient(), item, false);
            buybacks.remove(index);

            return true;
        } finally {
            lock.unlock();
        }
    }

    public static class BuybackEntry {
        private final Item item;
        private final int price;
        private final short quantity;
        private final long expiry;

        public BuybackEntry(Item item, int price, short quantity, long expiry) {
            this.item = item;
            this.price = price;
            this.quantity = quantity;
            this.expiry = expiry;
        }

        public Item getItem() { return item; }
        public int getPrice() { return price; }
        public short getQuantity() { return quantity; }
        public long getExpiry() { return expiry; }
    }

    private void removeExpiredEntries(int charId) {
        List<BuybackEntry> buybacks = playerBuybacks.get(charId);
        if (buybacks != null) {
            buybacks.removeIf(entry -> entry.getExpiry() < System.currentTimeMillis());
            if (buybacks.isEmpty()) {
                playerBuybacks.remove(charId);
            }
        }
    }

    private void cleanupAllExpiredEntries() {
        lock.lock();
        try {
            for (Integer charId : new ArrayList<>(playerBuybacks.keySet())) {
                removeExpiredEntries(charId);
            }
        } finally {
            lock.unlock();
        }
    }
}
