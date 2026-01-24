package server.events.gm;

public class Event {
    private final int mapid;
    private int limit;

    public Event(int mapid, int limit) {
        this.mapid = mapid;
        this.limit = limit;
    }

    public int getMapId() { return mapid; }
    public int getLimit() { return limit; }

    public void minusLimit() { this.limit--; }
    public void addLimit() { this.limit++; }
}