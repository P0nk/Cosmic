package server.questboard;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class QuestRequirementBuilder {

    public static class Requirement {
        public final int itemId;
        public final int quantity;

        public Requirement(int itemId, int quantity) {
            this.itemId = itemId;
            this.quantity = quantity;
        }
    }

    // Keyed by character ID
    private static final Map<Integer, List<Requirement>> pendingRequirements = new HashMap<>();

    public static void addRequirement(int characterId, int itemId, int quantity) {
        pendingRequirements.computeIfAbsent(characterId, k -> new ArrayList<>())
                .add(new Requirement(itemId, quantity));
    }

    public static List<Requirement> getRequirements(int characterId) {
        return pendingRequirements.getOrDefault(characterId, new ArrayList<>());
    }

    public static void clearRequirements(int characterId) {
        pendingRequirements.remove(characterId);
    }

    public static boolean hasRequirements(int characterId) {
        return pendingRequirements.containsKey(characterId);
    }
}
