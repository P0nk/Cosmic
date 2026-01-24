package server.events.gm;

import client.Character;
import constants.id.MapId;
import server.TimerManager;
import tools.PacketCreator;
import java.util.concurrent.ScheduledFuture;

public class Ola {
    private final Character chr;
    private long timeStarted = 0;
    private ScheduledFuture<?> schedule = null;
    private static final long DURATION = 360000; // 6 mins

    public Ola(final Character chr) {
        this.chr = chr;
        this.schedule = TimerManager.getInstance().schedule(() -> {
            if (MapId.isOlaOla(chr.getMapId())) {
                chr.changeMap(chr.getMap().getReturnMap());
            }
            resetTimes();
        }, DURATION);
    }

    public void startOla() {
        chr.getMap().startEvent();
        chr.sendPacket(PacketCreator.getClock((int) (DURATION / 1000)));
        this.timeStarted = System.currentTimeMillis();

        chr.getMap().getPortal("join00").setPortalStatus(true);
        chr.sendPacket(PacketCreator.serverNotice(0, "The portal has now opened. Press the up arrow key at the portal to enter."));
    }

    public boolean isTimerStarted() {
        return timeStarted > 0;
    }

    public void resetTimes() {
        this.timeStarted = 0;
        if (schedule != null) {
            schedule.cancel(false);
        }
    }

    public long getTimeLeft() {
        return DURATION - (System.currentTimeMillis() - timeStarted);
    }
}