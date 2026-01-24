package server.events.gm;

public class CoconutObject {
    private final int id;
    private int hits = 0;
    private boolean hittable = false;
    private long hittime = System.currentTimeMillis();

    public CoconutObject(int id) {
        this.id = id;
    }

    public void hit() {
        this.hittime = System.currentTimeMillis() + 750;
        hits++;
    }

    public int getHits() { return hits; }
    public void resetHits() { hits = 0; }

    public boolean isHittable() { return hittable; }
    public void setHittable(boolean hittable) { this.hittable = hittable; }

    public long getHitTime() { return hittime; }
}