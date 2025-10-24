package provider.wz;

import java.nio.file.Files;
import java.nio.file.Path;

public enum WZFiles {
    QUEST("Quest"),
    ETC("Etc"),
    ITEM("Item"),
    CHARACTER("Character"),
    STRING("String"),
    LIST("List"),
    MOB("Mob"),
    MAP("Map"),
    NPC("Npc"),
    REACTOR("Reactor"),
    SKILL("Skill"),
    SOUND("Sound"),
    UI("UI");

    public static final String DIRECTORY = getWzDirectory();

    private final String fileName;

    WZFiles(String name) {
        this.fileName = name + ".wz";
    }

    public Path getFile() {
        return Path.of(DIRECTORY, fileName);
    }

    public String getFilePath() {
        return getFile().toString();
    }

    private static String getWzDirectory() {
        String propertyPath = System.getProperty("wz-path");
        String finalPath;

        if (propertyPath != null && Files.isDirectory(Path.of(propertyPath))) {
            finalPath = propertyPath;
            System.out.println("[DEBUG][WZFiles] Using custom WZ directory (via -Dwz-path): "
                    + Path.of(propertyPath).toAbsolutePath());
        } else {
            finalPath = "wz";
            System.out.println("[DEBUG][WZFiles] Using default relative directory: "
                    + Path.of(finalPath).toAbsolutePath());
            if (propertyPath != null) {
                System.out.println("[DEBUG][WZFiles] Provided wz-path was invalid or not a directory: " + propertyPath);
            }
        }

        // Small extra check: list known WZ files
        try {
            Path dir = Path.of(finalPath);
            if (Files.isDirectory(dir)) {
                System.out.println("[DEBUG][WZFiles] Listing found WZ directories:");
                Files.list(dir).filter(Files::isDirectory).forEach(f -> {
                    System.out.println("    - " + f.getFileName());
                });
            } else {
                System.out.println("[DEBUG][WZFiles] Directory does not exist: " + dir.toAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("[DEBUG][WZFiles] Error checking directory contents: " + e.getMessage());
        }

        return finalPath;
    }

}
