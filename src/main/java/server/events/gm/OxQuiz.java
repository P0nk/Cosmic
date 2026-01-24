package server.events.gm;

import client.Character;
import provider.DataProvider;
import provider.DataProviderFactory;
import provider.DataTool;
import provider.wz.WZFiles;
import server.TimerManager;
import server.maps.MapleMap;
import tools.PacketCreator;
import tools.Randomizer;

import java.util.ArrayList;
import java.util.List;

public final class OxQuiz {
    private int round = 1;
    private int question = 1;
    private final MapleMap map;
    private final int expGain = 200;
    private static final DataProvider stringData = DataProviderFactory.getDataProvider(WZFiles.ETC);

    public OxQuiz(MapleMap map) {
        this.map = map;
        this.round = Randomizer.nextInt(9);
        this.question = 1;
    }

    private boolean isCorrectAnswer(Character chr, int answer) {
        double x = chr.getPosition().getX();
        double y = chr.getPosition().getY();
        // 0 = X, 1 = O (Usually) - Check packet handling for 0/1 meaning
        if ((x > -234 && y > -26 && answer == 0) || (x < -234 && y > -26 && answer == 1)) {
            chr.dropMessage("Correct!");
            return true;
        }
        return false;
    }

    public void sendQuestion() {
        int gmCount = 0;
        for (Character mc : map.getCharacters()) {
            if (mc.gmLevel() > 1) {
                gmCount++;
            }
        }
        final int gms = gmCount;

        map.broadcastMessage(PacketCreator.showOXQuiz(round, question, true));

        TimerManager.getInstance().schedule(() -> {
            map.broadcastMessage(PacketCreator.showOXQuiz(round, question, true));
            List<Character> chars = new ArrayList<>(map.getCharacters());

            for (Character chr : chars) {
                if (chr != null) {
                    if (!isCorrectAnswer(chr, getOXAnswer(round, question)) && !chr.isGM()) {
                        chr.changeMap(chr.getMap().getReturnMap());
                    } else {
                        chr.gainExp(expGain, true, true);
                    }
                }
            }

            // Advance Question
            if (shouldEndRound(round, question)) {
                question = 100;
            } else {
                question++;
            }

            // Check if Event Ended
            if (map.getCharacters().size() - gms <= 2) {
                map.broadcastMessage(PacketCreator.serverNotice(6, "The event has ended"));
                map.getPortal("join00").setPortalStatus(true);
                map.setOx(null);
                map.setOxQuiz(false);
                return;
            }
            sendQuestion();
        }, 30000);
    }

    private boolean shouldEndRound(int r, int q) {
        if (r == 1 && q == 29) return true;
        if ((r == 2 || r == 3) && q == 17) return true;
        if ((r == 4 || r == 8) && q == 12) return true;
        if (r == 5 && q == 26) return true;
        if (r == 9 && q == 44) return true;
        if ((r == 6 || r == 7) && q == 16) return true;
        return false;
    }

    private static int getOXAnswer(int imgdir, int id) {
        return DataTool.getInt(stringData.getData("OXQuiz.img").getChildByPath("" + imgdir).getChildByPath("" + id).getChildByPath("a"));
    }
}