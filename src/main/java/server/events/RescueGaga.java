package server.events;

import client.Character;
import client.SkillFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author kevintjuh93
 */
public class RescueGaga extends MapleEventEntry {

    private int completed;

    public RescueGaga(int completed) {
        super();
        this.completed = completed;
    }

    public int getCompleted() {
        return completed;
    }

    public void complete() {
        completed++;
    }

    @Override
    public int getInfo() {
        return getCompleted();
    }

    public void giveSkill(Character chr) {
        int skillid = 0;
        switch (chr.getJobType()) {
            case 0: // Beginner
                skillid = 1013;
                break;
            case 1: // Legend
            case 2: // Evan
                skillid = 10001014;
                break;
        }

        if (skillid == 0) return; // Safety check

        long expiration = (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(20));

        if (completed < 20) {
            chr.changeSkillLevel(SkillFactory.getSkill(skillid), (byte) 1, 1, expiration);
            chr.changeSkillLevel(SkillFactory.getSkill(skillid + 1), (byte) 1, 1, expiration);
            chr.changeSkillLevel(SkillFactory.getSkill(skillid + 2), (byte) 1, 1, expiration);
        } else {
            chr.changeSkillLevel(SkillFactory.getSkill(skillid), (byte) 2, 2, chr.getSkillExpiration(skillid));
        }
    }
}