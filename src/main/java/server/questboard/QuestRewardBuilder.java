package server.questboard;

import java.util.*;

public class QuestRewardBuilder {

    public enum RewardType {
        ITEM, MESO, NX
    }

    public static class Reward {
        public final RewardType type;
        public final int itemIdOrAmount;
        public final int quantity;

        public Reward(RewardType type, int itemIdOrAmount, int quantity) {
            this.type = type;
            this.itemIdOrAmount = itemIdOrAmount;
            this.quantity = quantity;
        }
    }

    private static final Map<Integer, List<Reward>> pendingRewards = new HashMap<>();

    public static void addReward(int charId, Reward reward) {
        pendingRewards.computeIfAbsent(charId, k -> new ArrayList<>()).add(reward);
    }

    public static List<Reward> getRewards(int charId) {
        return pendingRewards.getOrDefault(charId, new ArrayList<>());
    }

    public static void clearRewards(int charId) {
        pendingRewards.remove(charId);
    }

    public static boolean hasRewards(int charId) {
        return pendingRewards.containsKey(charId);
    }
}
