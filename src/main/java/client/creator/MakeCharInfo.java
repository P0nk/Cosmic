package client.creator;

import client.Character;
import client.Job;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import client.inventory.InventoryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import provider.Data;
import provider.DataTool;



public class MakeCharInfo {
    private static final Logger log = LoggerFactory.getLogger(MakeCharInfo.class);
    private static final String FACE_ID = "0";
    private static final String HAIR_ID = "1";
    private static final String HAIR_COLOR_ID = "2";
    private static final String SKIN_ID = "3";
    private static final String TOP_ID = "4";
    private static final String BOTTOM_ID = "5";
    private static final String SHOE_ID = "6";
    private static final String WEAPON_ID = "7";

    private final Set<Integer> charFaces = new HashSet<>();
    private final Set<Integer> charHairs = new HashSet<>();
    private final Set<Integer> charHairColors = new HashSet<>();
    private final Set<Integer> charSkins = new HashSet<>();
    private final Set<Integer> charTops = new HashSet<>();
    private final Set<Integer> charBottoms = new HashSet<>();
    private final Set<Integer> charShoes = new HashSet<>();
    private final Set<Integer> charWeapons = new HashSet<>();

    public MakeCharInfo(Data charInfoData) {
        for (Data data : charInfoData.getChildren()) {
            switch (data.getName()) {
                case FACE_ID -> {
                    for (Data faceData : data) {
                        charFaces.add(DataTool.getInt(faceData));
                    }
                }
                case HAIR_ID -> {
                    for (Data hairData : data) {
                        charHairs.add(DataTool.getInt(hairData));
                    }
                }
                case HAIR_COLOR_ID -> {
                    for (Data hairColorData : data) {
                        charHairColors.add(DataTool.getInt(hairColorData));
                    }
                }
                case SKIN_ID -> {
                    for (Data skinData : data) {
                        charSkins.add(DataTool.getInt(skinData));
                    }
                }
                case TOP_ID -> {
                    for (Data topData : data) {
                        charTops.add(DataTool.getInt(topData));
                    }
                }
                case BOTTOM_ID -> {
                    for (Data bottomData : data) {
                        charBottoms.add(DataTool.getInt(bottomData));
                    }
                }
                case SHOE_ID -> {
                    for (Data shoeData : data) {
                        charShoes.add(DataTool.getInt(shoeData));
                    }
                }
                case WEAPON_ID -> {
                    for (Data weaponData : data) {
                        charWeapons.add(DataTool.getInt(weaponData));
                    }
                }
                default -> log.error("Unhandled node inside MakeCharInfo.img.xml: '" + data.getName() + "'");
            }
        }
    }

    public boolean verifyFaceId(int id) {
        return this.charFaces.contains(id);
    }

    public boolean verifyHairId(int id) {
        if (id % 10 != 0) {
            return this.charHairs.contains(id - (id % 10));
        }
        return this.charHairs.contains(id);
    }

    public boolean verifyHairColorId(int id) {
        return this.charHairColors.contains(id % 10);
    }

    public boolean verifySkinId(int id) {
        return this.charSkins.contains(id);
    }

    public boolean verifyTopId(int id) {
        return this.charTops.contains(id);
    }

    public boolean verifyBottomId(int id) {
        return this.charBottoms.contains(id);
    }

    public boolean verifyShoeId(int id) {
        return this.charShoes.contains(id);
    }

    public boolean verifyWeaponId(int id) {
        return this.charWeapons.contains(id);
    }

    public Set<Integer> getValidHairIds() {
        return Set.copyOf(charHairs);
    }

    public Set<Integer> getValidFaceIds() {
        return Set.copyOf(charFaces);
    }

    public Set<Integer> getValidHairColors() {
        return Set.copyOf(charHairColors);
    }

    public Set<Integer> getValidSkinIds() {
        return Set.copyOf(charSkins);
    }


    public boolean verifyCharacter(Character character) {
        if (!verifyFaceId(character.getFace())) return false;
        if (!verifyHairId(character.getHair())) return false;
        if (!verifyHairColorId(character.getHair())) return false;
        if (!verifySkinId(character.getSkinColor().getId())) return false;

        // Here we only verify the equipment if the character that's being created is of type 'Beginner'
        // This is because when the Maple Life A or Maple Life B items are used, the client does not send any data
        // regarding what equipment the character should be wearing (as it's all handled server-side)
        Job characterJob = character.getJob();
        if (characterJob == Job.BEGINNER || characterJob == Job.NOBLESSE || characterJob == Job.LEGEND) {
            if (!verifyTopId(character.getInventory(InventoryType.EQUIPPED).getItem((short) -5).getItemId()))
                return false;
            if (!verifyBottomId(character.getInventory(InventoryType.EQUIPPED).getItem((short) -6).getItemId()))
                return false;
            if (!verifyShoeId(character.getInventory(InventoryType.EQUIPPED).getItem((short) -7).getItemId()))
                return false;
            if (!verifyWeaponId(character.getInventory(InventoryType.EQUIPPED).getItem((short) -11).getItemId()))
                return false;
        }

        return true;
    }


}
