package server;

import java.util.concurrent.ScheduledFuture;

public class TotemEventData {
    private long timeStart;
    private ScheduledFuture<?> eventSchedule;
    private int mapId;

    public long getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(long timeStart) {
        this.timeStart = timeStart;
    }

    public ScheduledFuture<?> getEventSchedule() {
        return eventSchedule;
    }

    public void setEventSchedule(ScheduledFuture<?> eventSchedule) {
        this.eventSchedule = eventSchedule;
    }

    public int getMapId() {
        return mapId;
    }

    public void setMapId(int mapId) {
        this.mapId = mapId;
    }
}
