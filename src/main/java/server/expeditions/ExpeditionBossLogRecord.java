package server.expeditions;

import java.sql.Timestamp;

public class ExpeditionBossLogRecord {
    private int id;
    private int characterId;
    private String characterName;
    private String bossType;
    private Timestamp attemptTime;
    private long damageDealt;
    private long duration;
    private String partyUuid;
    private boolean complete;

    public ExpeditionBossLogRecord(int id, int characterId, String characterName, String bossType, Timestamp attemptTime, long damageDealt, long duration, String partyUuid, boolean complete) {
        this.setId(id);
        this.setCharacterId(characterId);
        this.setCharacterName(characterName);
        this.setBossType(bossType);
        this.setAttemptTime(attemptTime);
        this.setDamageDealt(damageDealt);
        this.setDuration(duration);
        this.setPartyUuid(partyUuid);
        this.setComplete(complete);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCharacterId() {
        return characterId;
    }

    public void setCharacterId(int characterId) {
        this.characterId = characterId;
    }

    public String getCharacterName() {
        return characterName;
    }

    public void setCharacterName(String characterName) {
        this.characterName = characterName;
    }

    public String getBossType() {
        return bossType;
    }

    public void setBossType(String bossType) {
        this.bossType = bossType;
    }

    public Timestamp getAttemptTime() {
        return attemptTime;
    }

    public void setAttemptTime(Timestamp attemptTime) {
        this.attemptTime = attemptTime;
    }

    public long getDamageDealt() {
        return damageDealt;
    }

    public void setDamageDealt(long damageDealt) {
        this.damageDealt = damageDealt;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getPartyUuid() {
        return partyUuid;
    }

    public void setPartyUuid(String partyUuid) {
        this.partyUuid = partyUuid;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }
}
