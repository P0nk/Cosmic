package server.maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import provider.Data;
import provider.DataTool;

import java.util.HashMap;
import java.util.Map;

public class MapPyramidInfo {

    private static final Logger log = LoggerFactory.getLogger(MapPyramidInfo.class);

    private final Map<Integer, CountEffect> countEffects = new HashMap<>();
    private int coolAdd;
    private int decrease;
    private int hitAdd;
    private int missSub;
    private int total;

    public MapPyramidInfo(Data mobMassacreData) {
        Data countEffectData = mobMassacreData.getChildByPath("countEffect");
        if (countEffectData != null) {
            for (Data data : countEffectData.getChildren()) {
                try {
                    int countNumber = Integer.parseInt(data.getName());
                    this.countEffects.put(countNumber, new CountEffect(data));
                } catch (NumberFormatException nfe) {
                    log.error("countEffect node name is not a valid number: " + data.getName(), nfe);
                }
            }
        }

        Data gaugeData = mobMassacreData.getChildByPath("gauge");
        if (gaugeData != null) {
            for (Data data : gaugeData.getChildren()) {
                switch (data.getName()) {
                    case "coolAdd":
                        this.coolAdd = DataTool.getInt(data, 5);
                        break;
                    case "decrease":
                        this.decrease = DataTool.getInt(data, 1);
                        break;
                    case "hitAdd":
                        this.hitAdd = DataTool.getInt(data, 1);
                        break;
                    case "missSub":
                        this.missSub = DataTool.getInt(data, 4);
                        break;
                    case "total":
                        this.total = DataTool.getInt(data, 100);
                        break;
                    default:
                        log.warn("Unhandled Gauge data node: " + data.getName());
                        break;
                }
            }
        }
    }

    public Map<Integer, CountEffect> getCountEffects() {
        return countEffects;
    }

    public int getCoolAdd() {
        return coolAdd;
    }

    public int getDecrease() {
        return decrease;
    }

    public int getHitAdd() {
        return hitAdd;
    }

    public int getMissSub() {
        return missSub;
    }

    public int getTotal() {
        return total;
    }

    public static class CountEffect {

        private static final Logger log = LoggerFactory.getLogger(CountEffect.class);
        private int buff = -1;
        private int skillUse = 0;

        public CountEffect(Data countEffectData) {
            for (Data data : countEffectData) {
                switch (data.getName()) {
                    case "buff":
                        this.buff = DataTool.getInt(data, -1);
                        break;
                    case "skillUse":
                        this.skillUse = DataTool.getInt(data, 0);
                        break;
                    default:
                        log.warn("Unhandled CountEffect data node: " + data.getName());
                        break;
                }
            }
        }

        public int getBuff() {
            return buff;
        }

        public int getSkillUse() {
            return skillUse;
        }
    }
}
