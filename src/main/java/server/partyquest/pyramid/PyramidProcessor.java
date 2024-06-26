package server.partyquest.pyramid;

import client.Character;
import net.server.world.Party;

import java.util.HashMap;
import java.util.Map;

public class PyramidProcessor {

    private static final Map<Integer, Pyramid> activePyramids = new HashMap<>();

    public static Pyramid createSoloPyramidInstance(Character soloCharacter, int difficultyId) {
        return new Pyramid(soloCharacter, PyramidDifficulty.getById(difficultyId));
    }

    public static Pyramid createPartyPyramidInstance(Party party, int difficultyId) {
        return new Pyramid(party, PyramidDifficulty.getById(difficultyId));
    }

    public static void registerPyramid(int characterId, Pyramid pyramid) {
        synchronized (activePyramids) {
            activePyramids.put(characterId, pyramid);
        }
    }

    public static void closePyramid(int characterId) {
        synchronized (activePyramids) {
            activePyramids.remove(characterId);
        }
    }

    public static Pyramid getPyramidForCharacter(int characterId) {
        synchronized (activePyramids) {
            return activePyramids.get(characterId);
        }
    }
}
