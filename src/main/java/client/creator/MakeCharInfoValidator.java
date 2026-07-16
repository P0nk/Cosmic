package client.creator;

import client.Character;
import provider.Data;
import provider.DataProviderFactory;
import provider.wz.WZFiles;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class MakeCharInfoValidator {
    private static final MakeCharInfo charFemale;
    private static final MakeCharInfo charMale;
    private static final MakeCharInfo orientCharFemale;
    private static final MakeCharInfo orientCharMale;
    private static final MakeCharInfo premiumCharFemale;
    private static final MakeCharInfo premiumCharMale;

    static {
        Data data = DataProviderFactory.getDataProvider(WZFiles.ETC).getData("MakeCharInfo.img");
        charFemale = new MakeCharInfo(data.getChildByPath("Info/CharFemale"));
        charMale = new MakeCharInfo(data.getChildByPath("Info/CharMale"));
        orientCharFemale = new MakeCharInfo(data.getChildByPath("OrientCharFemale"));
        orientCharMale = new MakeCharInfo(data.getChildByPath("OrientCharMale"));
        premiumCharFemale = new MakeCharInfo(data.getChildByPath("PremiumCharFemale"));
        premiumCharMale = new MakeCharInfo(data.getChildByPath("PremiumCharMale"));
    }

    private static MakeCharInfo getMakeCharInfo(Character character) {
        return switch (character.getJob()) {
            case BEGINNER, WARRIOR, MAGICIAN, BOWMAN, THIEF, PIRATE -> character.isMale() ? charMale : charFemale;

            case NOBLESSE -> character.isMale() ? premiumCharMale : premiumCharFemale;

            case LEGEND -> character.isMale() ? orientCharMale : orientCharFemale;

            default -> null;
        };
    }

    public static Set<Integer> getValidHairIds() {
        Set<Integer> ids = new HashSet<>();

        ids.addAll(charFemale.getValidHairIds());
        ids.addAll(charMale.getValidHairIds());
        ids.addAll(orientCharFemale.getValidHairIds());
        ids.addAll(orientCharMale.getValidHairIds());
        ids.addAll(premiumCharFemale.getValidHairIds());
        ids.addAll(premiumCharMale.getValidHairIds());

        return Set.copyOf(ids);
    }

    public static Set<Integer> getValidFaceIds() {
        Set<Integer> ids = new HashSet<>();

        ids.addAll(charFemale.getValidFaceIds());
        ids.addAll(charMale.getValidFaceIds());
        ids.addAll(orientCharFemale.getValidFaceIds());
        ids.addAll(orientCharMale.getValidFaceIds());
        ids.addAll(premiumCharFemale.getValidFaceIds());
        ids.addAll(premiumCharMale.getValidFaceIds());

        return Set.copyOf(ids);
    }

    public static Set<Integer> getValidHairColors() {
        Set<Integer> ids = new HashSet<>();

        ids.addAll(charFemale.getValidHairColors());
        ids.addAll(charMale.getValidHairColors());
        ids.addAll(orientCharFemale.getValidHairColors());
        ids.addAll(orientCharMale.getValidHairColors());
        ids.addAll(premiumCharFemale.getValidHairColors());
        ids.addAll(premiumCharMale.getValidHairColors());

        return Set.copyOf(ids);
    }

    public static Set<Integer> getValidSkinIds() {
        Set<Integer> ids = new HashSet<>();

        ids.addAll(charFemale.getValidSkinIds());
        ids.addAll(charMale.getValidSkinIds());
        ids.addAll(orientCharFemale.getValidSkinIds());
        ids.addAll(orientCharMale.getValidSkinIds());
        ids.addAll(premiumCharFemale.getValidSkinIds());
        ids.addAll(premiumCharMale.getValidSkinIds());

        return Set.copyOf(ids);
    }

    public static boolean isNewCharacterValid(Character character) {
        MakeCharInfo makeCharInfo = getMakeCharInfo(character);
        if (makeCharInfo == null) return false;

        return makeCharInfo.verifyCharacter(character);
    }

}