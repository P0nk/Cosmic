package scripting.npc;

import client.Character;
import config.YamlConfig;
import constants.inventory.ItemConstants;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NpcMakerCraftingService {
    public static boolean useMakerPermissionAtkUp() {
        return YamlConfig.config.server.USE_MAKER_PERMISSIVE_ATKUP;
    }

//    // checks and prevents hackers from PE'ing Maker operations with invalid operations
//    public static boolean sanitizeReagentQuantities(int toCreate, Map<Integer, Short> reagentids) {
//        Map<Integer, Integer> reagentType = new LinkedHashMap<>();
//        List<Integer> toRemove = new LinkedList<>();
//
//        boolean isWeapon = ItemConstants.isWeapon(toCreate) || YamlConfig.config.server.USE_MAKER_PERMISSIVE_ATKUP;  // thanks Vcoc for finding a case where a weapon wouldn't be counted as such due to a bounding on isWeapon
//
//        for (Map.Entry<Integer, Short> r : reagentids.entrySet()) {
//            int curRid = r.getKey();
//            int type = r.getKey() / 100;
//
//            if (type < 42502 && !isWeapon) {     // only weapons should gain w.att/m.att from these.
//                toRemove.add(curRid);
//            } else {
//                Integer tableRid = reagentType.get(type);
//
//                if (tableRid == null) {
//                    reagentType.put(type, curRid);
//                    continue;
//                }
//
//                if (tableRid < curRid) {
//                    toRemove.add(tableRid);
//                    reagentType.put(type, curRid);
//                    continue;
//                }
//
//                toRemove.add(curRid);
//            }
//        }
//
//        // removing less effective gems of repeated type as well as invalid gems
//        for (Integer i : toRemove) {
//            reagentids.remove(i);
//        }
//
//        // the Maker skill will use only one of each gem per item
//        reagentids.replaceAll((i, v) -> (short) 1);
//
//        return true;
//    }

    public static int getMakerSkillLevel(Character chr) {
        return chr.getSkillLevel((chr.getJob().getId() / 1000) * 10000000 + 1007);
    }
}
