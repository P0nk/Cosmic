package server.expeditions;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardRecord {
    private final String partyUuid;
    private final String formattedAttemptTime;
    private final String formattedCompletionTime;
    private final List<ExpeditionBossLogRecord> records;

    public LeaderboardRecord(String partyUuid, String formattedAttemptTime, String formattedCompletionTime) {
        this.partyUuid = partyUuid;
        this.formattedAttemptTime = formattedAttemptTime;
        this.formattedCompletionTime = formattedCompletionTime;
        this.records = new ArrayList<>();
    }

    public String getPartyUuid() {
        return partyUuid;
    }

    public String getFormattedAttemptTime() {
        return formattedAttemptTime;
    }

    public String getFormattedCompletionTime() {
        return formattedCompletionTime;
    }

    public List<ExpeditionBossLogRecord> getRecords() {
        return records;
    }

    public void addRecord(ExpeditionBossLogRecord record) {
        this.records.add(record);
    }
}
