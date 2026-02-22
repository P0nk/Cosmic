package server.trade;

import client.inventory.Item;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UntradableTradeManager {
    private static final UntradableTradeManager instance = new UntradableTradeManager();
    private AtomicInteger idCounter = new AtomicInteger(1);
    private ConcurrentHashMap<Integer, TradeRequest> activeTrades = new ConcurrentHashMap<>();

    // Map of CharacterID -> Daily Sends Today
    private ConcurrentHashMap<Integer, Integer> dailySends = new ConcurrentHashMap<>();

    public static UntradableTradeManager getInstance() {
        return instance;
    }

    public int addTradeRequest(int senderId, String senderName, int receiverId, String receiverName, Item item,
            long fee) {
        int id = idCounter.getAndIncrement();
        TradeRequest req = new TradeRequest(id, senderId, senderName, receiverId, receiverName, item, fee);
        activeTrades.put(id, req);
        return id;
    }

    public List<TradeRequest> getRequestsForReceiver(int receiverId) {
        List<TradeRequest> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (TradeRequest req : activeTrades.values()) {
            if (now - req.getCreationTime() > 172800000L) { // 2 days
                activeTrades.remove(req.getTradeId());
                continue;
            }
            if (req.getReceiverId() == receiverId && !req.isRefunding()) {
                list.add(req);
            }
        }
        return list;
    }

    public List<TradeRequest> getRefundsForSender(int senderId) {
        List<TradeRequest> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (TradeRequest req : activeTrades.values()) {
            if (now - req.getCreationTime() > 172800000L) { // 2 days
                activeTrades.remove(req.getTradeId());
                continue;
            }
            if (req.getSenderId() == senderId && req.isRefunding()) {
                list.add(req);
            }
        }
        return list;
    }

    public TradeRequest getRequest(int tradeId) {
        TradeRequest req = activeTrades.get(tradeId);
        if (req != null && System.currentTimeMillis() - req.getCreationTime() > 172800000L) {
            activeTrades.remove(tradeId);
            return null;
        }
        return req;
    }

    public void removeRequest(int tradeId) {
        activeTrades.remove(tradeId);
    }

    public void purgeExpired() {
        long now = System.currentTimeMillis();
        for (TradeRequest req : activeTrades.values()) {
            if (now - req.getCreationTime() > 172800000L) { // 2 days
                activeTrades.remove(req.getTradeId());
            }
        }
    }

    public void markRefunding(int tradeId) {
        TradeRequest req = activeTrades.get(tradeId);
        if (req != null) {
            req.setRefunding(true);
        }
    }

    // A simple daily limit tracker for the session.
    // In a production environment this should be backed by a DB or reset at a
    // specific time.
    public int getDailySends(int characterId) {
        return dailySends.getOrDefault(characterId, 0);
    }

    public void incrementDailySends(int characterId) {
        dailySends.put(characterId, getDailySends(characterId) + 1);
    }

    public long calculateFee(Item item) {
        long customFee = 10000000; // Base 10M
        if (item instanceof client.inventory.Equip) {
            client.inventory.Equip eq = (client.inventory.Equip) item;
            int totalStats = eq.getStr() + eq.getDex() + eq.getInt() + eq.getLuk() +
                    eq.getWatk() + eq.getMatk() + eq.getWdef() + eq.getMdef() +
                    eq.getAcc() + eq.getAvoid() + eq.getSpeed() + eq.getJump() +
                    eq.getHp() + eq.getMp();
            customFee += (totalStats * 10000L);
        }
        return customFee;
    }
}
