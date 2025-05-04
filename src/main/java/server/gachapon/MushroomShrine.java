package server.gachapon;

/**
 * @author Alan (SharpAceX) - gachapon source classes stub
 * @author Ronan - parsed MapleSEA loots
 * <p>
 * MapleSEA-like loots thanks to AyumiLove - src: https://ayumilovemaple.wordpress.com/maplestory-gachapon-guide/
 */

public class MushroomShrine extends GachaponItems {

    @Override
    public int[] getCommonItems() {
        return new int[]{
                //123 added://
                /* Use tab */
                2030009, 2030010,
                /* Common equipment */
                1022058, 1022060, 1022089, 1050018, 1051017, 1072238, 1072239, 1102040, 1102041, 1102042,
                1102084, 1102086, 1302225, 1302234, 1302261, 1302284, 1302332, 1312147, 1322093, 1322102,
                1322188, 1322196, 1432015, 1432016, 1432017, 1442039,

                /* Beginner equipment */
                1422011, 1442018

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
                /* Use tab */

                /* Common equipment */
                1072344, 1302101, 1432018

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
