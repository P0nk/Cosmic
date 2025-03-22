package database.drop;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import server.ItemInformationProvider;
import server.life.MonsterDropEntry;
import server.life.MonsterGlobalDropEntry;

import java.util.List;
import java.util.Optional;

public class DropProvider {
    private final DropRepository dropRepository;
    private final Cache<Integer, List<MonsterDrop>> monsterDropCache = Caffeine.newBuilder().build();
    private final Cache<Integer, List<MonsterGlobalDropEntry>> globalContinentDropCache = Caffeine.newBuilder().build();
    private volatile List<GlobalMonsterDrop> globalMonsterDrops = null;

    public DropProvider(DropRepository dropRepository) {
        if (dropRepository == null) {
            throw new IllegalArgumentException("DropDao must not be null");
        }
        this.dropRepository = dropRepository;
    }

    private List<MonsterDrop> getMonsterDrops(int monsterId) {
        return monsterDropCache.get(monsterId, dropRepository::getMonsterDrops);
    }

    public List<MonsterDropEntry> getMonsterDropEntries(int monsterId) {
        return getMonsterDrops(monsterId).stream()
                .map(this::mapToDropEntry)
                .toList();
    }

    // TODO: Temporary. MonsterDropEntry should be removed.
    private MonsterDropEntry mapToDropEntry(MonsterDrop monsterDrop) {
        short questId = monsterDrop.questId() == null ? 0 : monsterDrop.questId().shortValue();
        return new MonsterDropEntry(monsterDrop.itemId(), monsterDrop.chance(), monsterDrop.minQuantity(),
                monsterDrop.maxQuantity(), questId);
    }

    public List<MonsterGlobalDropEntry> getRelevantGlobalDrops(int mapId) {
        int continentId = mapId / 100_000_000;
        return globalContinentDropCache.get(continentId, this::getContinentDrops);
    }

    private List<MonsterGlobalDropEntry> getContinentDrops(int continentId) {
        return getGlobalDropEntries().stream()
                .filter(drop -> drop.continentid < 0 || drop.continentid == continentId)
                .toList();
    }

    private List<MonsterGlobalDropEntry> getGlobalDropEntries() {
        if (this.globalMonsterDrops == null) {
            loadGlobalDrops();
        }

        return globalMonsterDrops.stream()
                .map(this::mapToDropEntry)
                .toList();
    }

    private void loadGlobalDrops() {
        this.globalMonsterDrops = dropRepository.getGlobalMonsterDrops();
    }

    // TODO: Temporary. MonsterDropEntry should be removed.
    private MonsterGlobalDropEntry mapToDropEntry(GlobalMonsterDrop globalDrop) {
        short questId = globalDrop.questId() == null ? 0 : globalDrop.questId().shortValue();
        return new MonsterGlobalDropEntry(globalDrop.itemId(), globalDrop.chance(), globalDrop.continent(),
                globalDrop.minQuantity(), globalDrop.maxQuantity(), questId);
    }

    /**
     * The chance of an item to be stolen is calculated like this: (item chance) / (sum of all item chances)
     * It works just like "lottery scheduling", but with droppable items instead of OS processes.
     */
    public Optional<MonsterDropEntry> getRandomStealDrop(int monsterId) {
        List<MonsterDrop> relevantDrops = getMonsterDrops(monsterId).stream()
                .filter(this::isNonQuestItem)
                .toList();
        if (relevantDrops.isEmpty()) {
            return Optional.empty();
        }

        final long totalChance = relevantDrops.stream()
                .mapToLong(MonsterDrop::chance)
                .sum();
        final long winningTicket = (long) Math.floor(Math.random() * totalChance);

        long remainingChance = totalChance;
        for (MonsterDrop drop : relevantDrops) {
            remainingChance -= drop.chance();
            if (winningTicket >= remainingChance) {
                return Optional.of(mapToDropEntry(drop));
            }
        }

        return Optional.empty();
    }

    private boolean isNonQuestItem(MonsterDrop drop) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        return !ii.isQuestItem(drop.itemId()) && !ii.isPartyQuestItem(drop.itemId());
    }

    public void clearCaches() {
        this.monsterDropCache.invalidateAll();
        this.globalContinentDropCache.invalidateAll();
        this.globalMonsterDrops = null;
    }
}
