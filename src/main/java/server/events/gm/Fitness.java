package server.events.gm;

import client.Character;
import constants.id.MapId;
import server.TimerManager;
import tools.PacketCreator;
import java.util.concurrent.ScheduledFuture;

public class Fitness {
    private final Character chr;
    private long timeStarted = 0;
    private ScheduledFuture<?> schedule = null;
    private ScheduledFuture<?> schedulemsg = null;
    private static final long DURATION = 900000; // 15 minutes

    public Fitness(final Character chr) {
        this.chr = chr;
        this.schedule = TimerManager.getInstance().schedule(() -> {
            if (MapId.isPhysicalFitness(chr.getMapId())) {
                chr.changeMap(chr.getMap().getReturnMap());
            }
        }, DURATION);
    }

    public void startFitness() {
        chr.getMap().startEvent();
        chr.getClient().sendPacket(PacketCreator.getClock(900));
        this.timeStarted = System.currentTimeMillis();

        checkAndMessage();

        chr.getMap().getPortal("join00").setPortalStatus(true);
        chr.sendPacket(PacketCreator.serverNotice(0, "The portal has now opened. Press the up arrow key at the portal to enter."));
    }

    public boolean isTimerStarted() {
        return timeStarted > 0;
    }

    public void resetTimes() {
        this.timeStarted = 0;
        if (schedule != null) schedule.cancel(false);
        if (schedulemsg != null) schedulemsg.cancel(false);
    }

    public long getTimeLeft() {
        return DURATION - (System.currentTimeMillis() - timeStarted);
    }

    public void checkAndMessage() {
        this.schedulemsg = TimerManager.getInstance().register(() -> {
            if (chr.getFitness() == null || !MapId.isPhysicalFitness(chr.getMapId())) {
                resetTimes();
                return;
            }

            long left = getTimeLeft();
            if (left > 9000 && left < 11000) {
                msg("You have 10 sec left. Those of you unable to beat the game, we hope you beat it next time! Great job everyone!! See you later~");
            } else if (left > 99000 && left < 101000) {
                msg("Alright, you don't have much time remaining. Please hurry up a little!");
            } else if (left > 239000 && left < 241000) {
                msg("The 4th stage is the last one for [The Maple Physical Fitness Test]. Please don't give up at the last minute and try your best. The reward is waiting for you at the very top!");
            } else if (left > 299000 && left < 301000) {
                msg("The 3rd stage offers traps where you may see them, but you won't be able to step on them. Please be careful of them as you make your way up.");
            } else if (left > 359000 && left < 361000) {
                msg("For those who have heavy lags, please make sure to move slowly to avoid falling all the way down because of lags.");
            } else if (left > 499000 && left < 501000) {
                msg("Please remember that if you die during the event, you'll be eliminated from the game. If you're running out of HP, either take a potion or recover HP first before moving on.");
            } else if (left > 599000 && left < 601000) {
                msg("The most important thing you'll need to know to avoid the bananas thrown by the monkeys is *Timing* Timing is everything in this!");
            } else if (left > 659000 && left < 661000) {
                msg("The 2nd stage offers monkeys throwing bananas. Please make sure to avoid them by moving along at just the right timing.");
            } else if (left > 699000 && left < 701000) {
                msg("Please remember that if you die during the event, you'll be eliminated from the game. You still have plenty of time left, so either take a potion or recover HP first before moving on.");
            } else if (left > 779000 && left < 781000) {
                msg("Everyone that clears [The Maple Physical Fitness Test] on time will be given an item, regardless of the order of finish, so just relax, take your time, and clear the 4 stages.");
            } else if (left > 839000 && left < 841000) {
                msg("There may be a heavy lag due to many users at stage 1 all at once. It won't be difficult, so please make sure not to fall down because of heavy lag.");
            } else if (left > 869000 && left < 871000) {
                msg("[MapleStory Physical Fitness Test] consists of 4 stages, and if you happen to die during the game, you'll be eliminated from the game, so please be careful of that.");
            }
        }, 5000, 29500);
    }

    private void msg(String message) {
        chr.sendPacket(PacketCreator.serverNotice(0, message));
    }
}