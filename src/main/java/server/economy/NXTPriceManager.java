package server.economy;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class NXTPriceManager {
    private static final NXTPriceManager instance = new NXTPriceManager();
    private final Random rand = new Random();

    // Configuration
    private static final int BASE_PRICE = 1000000; // 1M NX
    private static final int MIN_PRICE = 500000;   // Floor
    private static final int MAX_PRICE = 2000000;  // Ceiling
    private static final double VOLATILITY = 0.05; // 5% swing potential
    private static final double IMPACT_FACTOR = 100.0; // Price change per coin bought/sold

    // State
    private double currentMarketVal;
    private long lastUpdate;

    // Track trends (0 = stable, 1 = up, 2 = down)
    private int trendDirection = 0;

    private NXTPriceManager() {
        this.currentMarketVal = BASE_PRICE;
        this.lastUpdate = System.currentTimeMillis();
    }

    public static NXTPriceManager getInstance() {
        return instance;
    }

    /**
     * Recalculates price based on time drift (Random Walk)
     * Call this periodically or on script open if enough time passed.
     */
    public void updateMarket() {
        long now = System.currentTimeMillis();
        // Update naturally every 10 minutes if no activity
        if (now - lastUpdate > 600000) {
            simulateNaturalDrift();
            lastUpdate = now;
        }
    }

    private void simulateNaturalDrift() {
        double change = (rand.nextDouble() * VOLATILITY * 2) - VOLATILITY; // -5% to +5%
        double oldVal = currentMarketVal;
        currentMarketVal += currentMarketVal * change;
        clampPrice();
        updateTrend(oldVal);
    }

    /**
     * Adjusts price based on player transaction.
     * @param amount Number of coins
     * @param isBuy True if player is buying (drives price UP), False if selling (drives price DOWN)
     */
    public void recordTransaction(int amount, boolean isBuy) {
        double oldVal = currentMarketVal;
        double impact = amount * IMPACT_FACTOR;

        if (isBuy) {
            currentMarketVal += impact;
        } else {
            currentMarketVal -= impact;
        }

        clampPrice();
        updateTrend(oldVal);
        lastUpdate = System.currentTimeMillis(); // Reset drift timer on activity
    }

    private void clampPrice() {
        if (currentMarketVal < MIN_PRICE) currentMarketVal = MIN_PRICE;
        if (currentMarketVal > MAX_PRICE) currentMarketVal = MAX_PRICE;
    }

    private void updateTrend(double oldVal) {
        if (currentMarketVal > oldVal) trendDirection = 1; // Up
        else if (currentMarketVal < oldVal) trendDirection = 2; // Down
        else trendDirection = 0;
    }

    // Getters formatted for the script
    public int getBuyPrice() {
        updateMarket(); // Ensure fresh
        return (int) currentMarketVal;
    }

    public int getSellPrice() {
        updateMarket();
        // Sell price is 90% of Buy price (The "Spread")
        return (int) (currentMarketVal * 0.90);
    }

    public String getTrendIcon() {
        if (trendDirection == 1) return "#r▲#k"; // Red Up (Stock market style) or Green depending on region
        if (trendDirection == 2) return "#b▼#k"; // Blue Down
        return "-";
    }
}