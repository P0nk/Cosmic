package client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import provider.DataProviderFactory;
import provider.wz.WZFiles;

import java.util.HashMap;
import java.util.Map;

public class DamageSkin {
    private static final Logger log = LoggerFactory.getLogger(DamageSkin.class);

    public static Map<Integer, Integer> useDamageSkins = new HashMap<>();

    public static void loadDamageSkins() {
        var effectDataWZ = DataProviderFactory.getDataProvider(WZFiles.EFFECT);
        var damageSkinsData = effectDataWZ.getData("DamageSkin.img").getChildren();

        for (var damageSkinData : damageSkinsData) {
            var damageSkinId = Integer.parseInt(damageSkinData.getName());
            //System.out.println("Damage Skin ID: " + damageSkinId);
            useDamageSkins.put(damageSkinId, damageSkinId);  // Mapping skin ID to itself
        }
    }

    /*
     * Validate if the character has a valid damage skin set and update if needed.
     * @param chr
     * @param update

    public static void validateDamageSkin(Character chr, boolean update) {
        var damageSkinId = chr.getDamageSkin();
        if (damageSkinId == 0) return;

        if (!useDamageSkins.containsKey(damageSkinId)) {
            chr.setDamageSkin(0, update);
        }
    }
    */
}
