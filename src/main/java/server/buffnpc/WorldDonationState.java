package server.buffnpc;

public class WorldDonationState {
    private int totalDonated; // Tracks total donated Maple Leaves
    private long boostEndTime; // Timestamp when the boost ends

    public WorldDonationState() {
        this.totalDonated = 0;
        this.boostEndTime = 0;
    }

    public int getTotalDonated() {
        return totalDonated;
    }

    public void addDonation(int amount) {
        this.totalDonated += amount;
    }

    public long getBoostEndTime() {
        return boostEndTime;
    }

    public void setBoostEndTime(long boostEndTime) {
        this.boostEndTime = boostEndTime;
    }

    public boolean isBoostActive() {
        return System.currentTimeMillis() < boostEndTime;
    }
}
