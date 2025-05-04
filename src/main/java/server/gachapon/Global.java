package server.gachapon;

import constants.id.ItemId;

/**
 * @author Alan (SharpAceX)
 * @author Ronan - added ores and reworked global loots
 */

public class Global extends GachaponItems {

    @Override
    public int[] getCommonItems() {
        return new int[]{
                //123 added://
                /* Use Tab */
                2000005, 2012008, 2022245, 2022344, 2030000, 2030008, 2050004
        };
    }

    @Override
    public int[] getUncommonItems() {
        return new int[]{
                //123 added://
                /* Use Tab */
                2022121, 2022123, 2022179, 2022273, 2022282, 2022283, 2022439

        };
    }

    @Override
    public int[] getRareItems() {
        return new int[]{};
    }

}
