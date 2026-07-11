package server.transactions;

import server.ItemInformationProvider;

import java.time.Instant;
import java.util.List;

public class Transaction {
    private int transactionId;
    private int userId;
    private Instant transactionDate;
    private boolean buybackUsed;
    private List<TransactionItem> items;

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Instant getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Instant transactionDate) {
        this.transactionDate = transactionDate;
    }

    public boolean isBuybackUsed() {
        return buybackUsed;
    }

    public void setBuybackUsed(boolean buybackUsed) {
        this.buybackUsed = buybackUsed;
    }

    public List<TransactionItem> getItems() {
        return items;
    }

    public void setItems(List<TransactionItem> items) {
        this.items = items;
    }

    public int getTotalPurchasePrice() {
        int total = 0;
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        for (TransactionItem item : items) {
            total += ii.getPrice(item.getItemId(), item.getQuantity());
        }
        return total;
    }
}


