package tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

public class HairsAndFacesHandler {
    private static final Set<Integer> validHair = new HashSet<>();
    private static final Set<Integer> validEyes = new HashSet<>();
    private static final String pathToCharacterWzDir = System.getProperty("user.dir") + "\\wz\\Character.wz\\";
    private static final String outputPath = "";

    public static void genericCollectIds(boolean isHair) {
        File dir = new File(pathToCharacterWzDir + (isHair ? "/Hair" : "/Face"));
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName().replace(".img.xml", "");
                int id = Integer.parseInt(fileName);
                if(isHair) {
                    validHair.add(id);
                } else {
                    validEyes.add(id);
                }
            }
        }
    }

    public static void handleValidHairs() {
        genericCollectIds(true);
    }

    public static void handleValidFaces() {
        genericCollectIds(false);
    }

    public static void createHairAndFaceListsFile() {
        System.out.println("Start");
        handleValidHairs();
        handleValidFaces();
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath + "rulaxHairAndEyes.txt"));
            writer.write("Hair:");
            writer.newLine();
            writer.write(validHair.toString());
            writer.newLine();
            writer.write("Eyes:");
            writer.newLine();
            writer.write(validEyes.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("End");
    }
}

