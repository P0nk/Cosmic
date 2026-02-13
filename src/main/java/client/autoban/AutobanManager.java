/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package client.autoban;

import client.Character;
import config.YamlConfig;
import net.netty.GameViolationException;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kevintjuh93
 */
public class AutobanManager {
    private static final Logger log = LoggerFactory.getLogger(AutobanManager.class);

    private final Character chr;
    private final Map<AutobanFactory, Integer> points = new HashMap<>();
    private final Map<AutobanFactory, Long> lastTime = new HashMap<>();
    private int misses = 0;
    private int lastmisses = 0;
    private int samemisscount = 0;
    private final long[] spam = new long[20];
    private final int[] timestamp = new int[20];
    private final byte[] timestampcounter = new byte[20];


    public AutobanManager(Character chr) {
        this.chr = chr;
    }

    /**
     * @return true if the added point should result in an autoban
     */
    public boolean addPoint(AutobanFactory fac) {
        if (lastTime.containsKey(fac)) {
            if (lastTime.get(fac) < (Server.getInstance().getCurrentTime() - fac.getExpire())) {
                points.put(fac, points.get(fac) / 2); //So the points are not completely gone.
            }
        }

        if (fac.getExpire() != -1) {
            lastTime.put(fac, Server.getInstance().getCurrentTime());
        }

        if (points.containsKey(fac)) {
            points.put(fac, points.get(fac) + 1);
        } else {
            points.put(fac, 1);
        }

        return points.get(fac) >= fac.getMaximum();
    }

    public void addMiss() {
        this.misses++;
    }

    public void resetMisses() {
        if (lastmisses == misses && misses > 6) {
            samemisscount++;
        }
        if (samemisscount > 4) {
            chr.sendPolice("You will be disconnected for miss godmode.");
        }
        //chr.autoban("Autobanned for : " + misses + " Miss godmode", 1);
        else if (samemisscount > 0) {
            this.lastmisses = misses;
        }
        this.misses = 0;
    }

    //Don't use the same type for more than 1 thing
    public void spam(int type) {
        this.spam[type] = Server.getInstance().getCurrentTime();
    }

    public void spam(int type, int timestamp) {
        this.spam[type] = timestamp;
    }

    public long getLastSpam(int type) {
        return spam[type];
    }

    /**
     * Timestamp checker
     *
     * <code>type</code>:<br>
     * 1: Pet Food<br>
     * 2: InventoryMerge<br>
     * 3: InventorySort<br>
     * 4: SpecialMove<br>
     * 5: UseCatchItem<br>
     * 6: Item Drop<br>
     * 7: Chat<br>
     * 8: HealOverTimeHP<br>
     * 9: HealOverTimeMP<br>
     *
     * @param type type
     * @return Timestamp checker
     */
    public void setTimestamp(int type, int time, int times) {
        if (this.timestamp[type] == time) {
            this.timestampcounter[type]++;
            if (this.timestampcounter[type] >= times) {
                log.info("Autoban - Chr {} was caught spamming TYPE {} and has been disconnected", chr, type);
                if (YamlConfig.config.server.USE_AUTOBAN) {
                    throw new GameViolationException("Auto ban");
                }
            }
        } else {
            this.timestamp[type] = time;
            this.timestampcounter[type] = 0;
        }
    }
}
