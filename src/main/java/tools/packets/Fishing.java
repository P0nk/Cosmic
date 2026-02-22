/*
    This file is part of the HeavenMS MapleStory Server
    Copyleft (L) 2016 - 2019 RonanLana

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package tools.packets;

import client.Character;
import config.YamlConfig;
import constants.id.ItemId;
import constants.id.MapId;
import constants.inventory.ItemConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ItemInformationProvider;
import tools.PacketCreator;

import java.util.Calendar;

/**
 * @author FateJiki (RaGeZONE)
 * @author Ronan - timing pattern
 */
public class Fishing {
    private static final Logger log = LoggerFactory.getLogger(Fishing.class);

    private static double getFishingLikelihood(int x) {
        return 50.0 + 7.0 * (7.0 * Math.sin(x)) * (Math.cos(Math.pow(x, 0.777)));
    }

    public static double[] fetchFishingLikelihood() {
        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

        int hours = calendar.get(Calendar.HOUR);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);

        double yearLikelihood = getFishingLikelihood(dayOfYear);
        double timeLikelihood = getFishingLikelihood(hours + minutes + seconds);

        return new double[] { yearLikelihood, timeLikelihood };
    }

    private static boolean hitFishingTime(Character chr, int baitLevel, double yearLikelihood, double timeLikelihood) {
        double baitLikelihood = 0.0002 * chr.getWorldServer().getFishingRate() * baitLevel; // can improve 10.0 at "max
                                                                                            // level 50000" on rate 1x

        if (YamlConfig.config.server.USE_DEBUG) {
            chr.dropMessage(5, "----- FISHING RESULT -----");
            chr.dropMessage(5,
                    "Likelihoods - Year: " + yearLikelihood + " Time: " + timeLikelihood + " Meso: " + baitLikelihood);
            chr.dropMessage(5, "Score rolls - Year: " + (0.23 * yearLikelihood) + " Time: " + (0.77 * timeLikelihood)
                    + " Meso: " + baitLikelihood);
        }

        return (0.23 * yearLikelihood) + (0.77 * timeLikelihood) + (baitLikelihood) > 57.777;
    }

    public static void doFishing(Character chr, int baitLevel, double yearLikelihood, double timeLikelihood) {
        // thanks Fadi, Vcoc for suggesting a custom fishing system

        if (!chr.isLoggedinWorld() || !chr.isAlive()) {
            return;
        }

        if (!MapId.isFishingArea(chr.getMapId())) {
            chr.dropMessage("You are not in a fishing area!");
            return;
        }

        if (chr.getLevel() < 30) {
            chr.dropMessage(5, "You must be above level 30 to fish!");
            return;
        }

        String fishingEffect;
        if (!hitFishingTime(chr, baitLevel, yearLikelihood, timeLikelihood)) {
            fishingEffect = "Effect/BasicEff.img/Catch/Fail";
        } else {
            String rewardStr = "";
            fishingEffect = "Effect/BasicEff.img/Catch/Success";

            int rand = (int) (3.0 * Math.random());
            switch (rand) {
                case 0:
                    int mesoAward = (int) ((1400.0 * Math.random() + 1201) * chr.getMesoRate())
                            + (15 * chr.getLevel() / 5);
                    chr.gainMeso(mesoAward, true, true, true);

                    rewardStr = mesoAward + " mesos.";
                    break;
                case 1:
                    int expAward = (int) ((645.0 * Math.random() + 620.0) * chr.getExpRate()
                            + (15 * chr.getLevel() / 4));
                    chr.gainExp(expAward, true, true);

                    rewardStr = expAward + " EXP.";
                    break;
                case 2:
                    int itemid = getRandomItem();
                    rewardStr = "a(n) " + ItemInformationProvider.getInstance().getName(itemid) + ".";

                    if (chr.canHold(itemid)) {
                        chr.getAbstractPlayerInteraction().gainItem(itemid, true);
                    } else {
                        chr.showHint("Couldn't catch a(n) #r" + ItemInformationProvider.getInstance().getName(itemid)
                                + "#k due to #e#b" + ItemConstants.getInventoryType(itemid) + "#k#n inventory limit.");
                        rewardStr += ".. but has goofed up due to full inventory.";
                    }
                    break;
            }

            chr.getMap().dropMessage(6, chr.getName() + " found " + rewardStr);
        }

        chr.sendPacket(PacketCreator.showInfo(fishingEffect));
        chr.getMap().broadcastMessage(chr, PacketCreator.showForeignInfo(chr.getId(), fishingEffect), false);
    }

    public static int getRandomItem() {
        int rand = (int) (100.0 * Math.random());
        int[] commons = { 1002851, 2002020, 2002020, ItemId.MANA_ELIXIR, 2000018, 2002018, 2002024, 2002027, 2002027,
                2000018, 2000018, 2000018, 2000018, 2002030, 2002018, 2000016 }; // filler' up
        int[] uncommons = { 1000025, 1002662, 1002812, 1002850, 1002881, 1002880, 1012072, 4020009, 2043220, 2043022,
                2040543, 2044420, 2040943, 2043713, 2044220, 2044120, 2040429, 2043220, 2040943 }; // filler' uptoo
        int[] rares = { 1002859, 1002553, 1002762, 1002763, 1002764, 1002765, 1002766, 1002663, 1002788, 1002949,
                2049100, 2340000, 2040822, 2040822, 2040822, 2040822 }; // filler' uplast

        if (rand >= 25) {
            return commons[(int) (commons.length * Math.random())];
        } else if (rand <= 7 && rand >= 4) {
            return uncommons[(int) (uncommons.length * Math.random())];
        } else {
            return rares[(int) (rares.length * Math.random())];
        }
    }

    private static void debugFishingLikelihood() {
        long[] a = new long[365], b = new long[365];
        long hits = 0, hits10 = 0, total = 0;

        for (int i = 0; i < 365; i++) {
            double yearLikelihood = getFishingLikelihood(i);

            int dayHits = 0, dayHits10 = 0;
            for (int k = 0; k < 24; k++) {
                for (int l = 0; l < 60; l++) {
                    for (int m = 0; m < 60; m++) {
                        double timeLikelihood = getFishingLikelihood(k + l + m);

                        if ((0.23 * yearLikelihood) + (0.77 * timeLikelihood) > 57.777) {
                            hits++;
                            dayHits++;
                        }

                        if ((0.23 * yearLikelihood) + (0.77 * timeLikelihood) + 10.0 > 57.777) {
                            hits10++;
                            dayHits10++;
                        }

                        total++;
                    }
                }
            }

            a[i] = dayHits;
            b[i] = dayHits10;
        }

        long maxhit = 0, minhit = Long.MAX_VALUE;
        for (int i = 0; i < 365; i++) {
            if (maxhit < a[i]) {
                maxhit = a[i];
            }

            if (minhit > a[i]) {
                minhit = a[i];
            }
        }

        long maxhit10 = 0, minhit10 = Long.MAX_VALUE;
        for (int i = 0; i < 365; i++) {
            if (maxhit10 < b[i]) {
                maxhit10 = b[i];
            }

            if (minhit10 > b[i]) {
                minhit10 = b[i];
            }
        }

        log.debug("Diary   min {} max {}", minhit, maxhit);
        log.debug("Diary10 min {} max {}", minhit10, maxhit10);
        log.debug("Hits: {}, Hits10: {}, Total: {} -- %1000 {}, +10 %1000: {}", hits, hits10, total,
                (hits * 1000 / total), (hits10 * 1000 / total));
    }
}
