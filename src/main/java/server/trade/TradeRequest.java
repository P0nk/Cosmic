package server.trade;

import client.inventory.Item;

public class TradeRequest {
    private int tradeId;
    private int senderId;
    private String senderName;
    private int receiverId;
    private String receiverName;
    private Item item;
    private long fee;
    private boolean refunding;
    private long creationTime;

    public TradeRequest(int tradeId, int senderId, String senderName, int receiverId, String receiverName, Item item,
            long fee) {
        this.tradeId = tradeId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.item = item;
        this.fee = fee;
        this.refunding = false;
        this.creationTime = System.currentTimeMillis();
    }

    public int getTradeId() {
        return tradeId;
    }

    public int getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public Item getItem() {
        return item;
    }

    public long getFee() {
        return fee;
    }

    public boolean isRefunding() {
        return refunding;
    }

    public void setRefunding(boolean refunding) {
        this.refunding = refunding;
    }

    public long getCreationTime() {
        return creationTime;
    }
}
