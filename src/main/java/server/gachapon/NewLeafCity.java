package server.gachapon;

/**
 * @author Alan (SharpAceX) - gachapon source classes stub & pirate equipment
 * @author Ronan - parsed MapleSEA loots, thanks Vcoc for noticing somewhat unbalanced loots in NLC
 * <p>
 * MapleSEA-like loots thanks to AyumiLove - src: https://ayumilovemaple.wordpress.com/maplestory-gachapon-guide/
 */

public class NewLeafCity extends GachaponItems {

    @Override
    public int[] getCommonItems() {
        return new int[]{
                //123 added://
                /* Use tab */
                2030020,
                /* Common equipment */
                1332021, 1332218, 1332219, 1332285, 1402044, 1442030, 1442046, 1472063, 1472081,

                /* Beginner equipment */


                /* Warrior equipment */
                1402048, 1402049, 1402050, 1402051, 1442068,

                /* Mage equipment */
                1332060,

                /* Bowman equipment */
                1452060, 1462052, 1462053, 1462054, 1462055,

                /* Thief equipment */
                1332077, 1332078, 1332079, 1332080, 1472072, 1472073, 1472074, 1472075

                /* Pirate equipment */

        };
    }

    @Override
    public int[] getUncommonItems() {
        return new int[]{
                //123 added://
                /* Use tab */


                /* Common equipment */
                1072344, 1102205, 1102206

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
                /* Common equipment */
                1102337,

                /* Beginner equipment */


                /* Warrior equipment */


                /* Mage equipment */


                /* Bowman equipment */


                /* Thief equipment */
                2070016, 2070018

                /* Pirate equipment */

        };
    }
}