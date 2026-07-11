package provider.wz;

import config.YamlConfig;

import java.io.File;
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
        this.fileName = name;
    }

    public Path getFile() {
        File bare = new File(DIRECTORY, fileName);
        if (bare.isDirectory()) {
            return bare.toPath();
        }

        File wzDir = new File(DIRECTORY, fileName + ".wz");
        if (wzDir.isDirectory()) {
            return wzDir.toPath();
        }

        return bare.toPath();
    }

    public String getFilePath() {
        return getFile().toString();
    }

    private static String getWzDirectory() {
        // Resolution order: -Ddata-path system property, then server.DATA_PATH in config.yaml, then "wz".
        String propertyPath = System.getProperty("data-path");
        if (propertyPath != null && new File(propertyPath).isDirectory()) {
            return propertyPath;
        }

        try {
            String configPath = YamlConfig.config.server.DATA_PATH;
            if (configPath != null && !configPath.isBlank() && new File(configPath).isDirectory()) {
                return configPath;
            }
        } catch (Throwable ignored) {
            // Standalone tools may run without a loadable config.yaml; fall through to default.
        }

        return "data";
    }
}
