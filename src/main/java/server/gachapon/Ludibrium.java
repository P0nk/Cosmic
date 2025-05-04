package server.gachapon;

import constants.id.ItemId;

/**
 * @author Ronan - parsed MapleSEA loots
 * <p>
 * MapleSEA-like loots thanks to AyumiLove - src: https://ayumilovemaple.wordpress.com/maplestory-gachapon-guide/
 */

public class Ludibrium extends GachaponItems {

    @Override
    public int[] getCommonItems() {
        return new int[]{
                //123 added://
                /* Chairs */
                3010002, 3010003, 3010004, 3010005, 3010006, 3010007, 3010008, 3010009,
                3010010, 3010016, 3010017, 3010045, 3010180, 3010181, 3010301, 3010302,
                3012005,

                /* Common equipment */
                1302049

                /* Beginner equipment */


                /* Warrior equipment */


                /* Mage equipment */


                /* Bowman equipment */


                /* Thief equipment */


                /* Pirate equipment */

        };
    }

    @Override
    public int[] getUncommonItems() {
        return new int[]{
                //123 added://
                /* Chairs */
                3010011, 3010012, 3010013, 3010018, 3010040, 3010041, 3010046, 3010047,
                3010072, 3010080, 3010085, 3010111, 3012010, 3012011

                /* Use Tab */


                /* Common equipment */


                /* Beginner equipment */


                /* Warrior equipment */


                /* Mage equipment */


                /* Bowman equipment */


                /* Thief equipment */


                /* Pirate equipment */

        };
    }

    @Override
    public int[] getRareItems() {
        return new int[]{
                //123 added://
                /* Chairs */
                3010022, 3010023, 3010025, 3010043, 3010057, 3010058, 3010071, 3010073,
                3010098, 3010099, 3010101, 3010106, 3010116, 3010427, 3010968

                /* Common equipment */


                /* Beginner equipment */


                /* Warrior equipment */


                /* Mage equipment */


                /* Bowman equipment */


                /* Thief equipment */


                /* Pirate equipment */

        };
    }

}
