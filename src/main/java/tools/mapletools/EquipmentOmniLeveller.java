package tools.mapletools;

import provider.wz.WZFiles;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/**
 * @author RonanLana
 * <p>
 * This application parses the Character.wz folder inputted and adds/updates the "info/level"
 * node on every known equipment id. This addition enables client-side view of the equipment
 * level attribute on every equipment in the game, given proper item visibility, be it from
 * own equipments or from other players.
 * <p>
 * Estimated parse time: 7 minutes
 */
public class EquipmentOmniLeveller {
    private static final Path INPUT_DIRECTORY = WZFiles.CHARACTER.getFile();
    private static final Path OUTPUT_DIRECTORY = ToolConstants.getOutputFile("equips-with-levels");
    private static final int INITIAL_STRING_LENGTH = 250;
    private static final int FIXED_EXP = 10000;
    private static final int MAX_EQP_LEVEL = 30;

    private static PrintWriter printWriter = null;
    private static BufferedReader bufferedReader = null;

    private static int infoTagState = -1;
    private static int infoTagExpState = -1;
    private static boolean infoTagLevel;
    private static boolean infoTagLevelExp;
    private static boolean infoTagLevelInfo;
    private static int parsedLevels = 0;
    private static byte status;
    private static boolean upgradeable;
    private static boolean cash;

    private static String getName(String token) {
        int i, j;
        char[] dest;
        String d;

        i = token.lastIndexOf("name");
        i = token.indexOf("\"", i) + 1; //lower bound of the string
        j = token.indexOf("\"", i);     //upper bound

        dest = new char[INITIAL_STRING_LENGTH];
        try {
            token.getChars(i, j, dest, 0);
        } catch (StringIndexOutOfBoundsException e) {
            // do nothing
            return "";
        } catch (Exception e) {
            System.out.println("error in: " + token + "");
            e.printStackTrace();
            try {
                Thread.sleep(100000000);
            } catch (Exception ex) {
            }
        }


        d = new String(dest);
        return (d.trim());
    }

    private static String getValue(String token) {
        int i, j;
        char[] dest;
        String d;

        i = token.lastIndexOf("value");
        i = token.indexOf("\"", i) + 1; //lower bound of the string
        j = token.indexOf("\"", i);     //upper bound

        dest = new char[INITIAL_STRING_LENGTH];
        token.getChars(i, j, dest, 0);

        d = new String(dest);
        return (d.trim());
    }

    private static void forwardCursor(int st) {
        String line = null;

        try {
            while (status >= st && (line = bufferedReader.readLine()) != null) {
                simpleToken(line);
                printWriter.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void translateLevelCursor(int st) {
        String line = null;

        try {
            infoTagLevelInfo = false;
            while (status >= st && (line = bufferedReader.readLine()) != null) {
                translateLevelToken(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void translateInfoTag(int st) {
        infoTagLevel = false;
        String line = null;

        try {
            while (status >= st && (line = bufferedReader.readLine()) != null) { // skipping directory & canvas definition
                translateInfoToken(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!upgradeable || cash) {
            throw new RuntimeException();
        }
    }

    private static void simpleToken(String token) {
        if (token.contains("/imgdir")) {
            status -= 1;
        } else if (token.contains("imgdir")) {
            status += 1;
        }
    }

    private static void printUpdatedLevelExp() {
        printWriter.println("          <int name=\"exp\" value=\"" + FIXED_EXP + "\"/>");
    }

    private static void printDefaultLevel(int level) {
        printWriter.println("        <imgdir name=\"" + level + "\">");
        printUpdatedLevelExp();
        printWriter.println("        </imgdir>");
    }

    private static void printDefaultLevelInfoTag() {
        printWriter.println("      <imgdir name=\"info\">");
        for (int i = 1; i <= MAX_EQP_LEVEL; i++) {
            printDefaultLevel(i);
        }
        printWriter.println("      </imgdir>");
    }

    private static void printDefaultLevelTag() {
        printWriter.println("    <imgdir name=\"level\">");
        printDefaultLevelInfoTag();
        printWriter.println("    </imgdir>");
    }

    private static void processLevelInfoTag(int st) {
        String line;
        try {
            while (status >= st && (line = bufferedReader.readLine()) != null) {
                translateLevelExpToken(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processLevelInfoSet(int st) {
        parsedLevels = (1 << MAX_EQP_LEVEL) - 1;

        String line;
        try {
            while (status >= st && (line = bufferedReader.readLine()) != null) {
                translateLevelInfoSetToken(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void translateLevelToken(String token) {
        if (token.contains("/imgdir")) {
            if (status == 3) {
                if (!infoTagLevelInfo) {
                    printDefaultLevelInfoTag();
                }
            }
            printWriter.println(token);

            status -= 1;
        } else if (token.contains("imgdir")) {
            printWriter.println(token);
            status += 1;

            if (status == 4) {
                String d = getName(token);
                if (d.contentEquals("info")) {
                    infoTagLevelInfo = true;
                    processLevelInfoSet(status);
                } else {
                    forwardCursor(status);
                }
            }
        } else {
            printWriter.println(token);
        }
    }

    private static void translateLevelInfoSetToken(String token) {
        if (token.contains("/imgdir")) {
            status -= 1;

            if (status == 3) {
                if (parsedLevels != 0) {
                    for (int i = 0; i < MAX_EQP_LEVEL; i++) {
                        if ((parsedLevels >> i) % 2 != 0) {
                            int level = i + 1;
                            printDefaultLevel(level);
                        }
                    }
                }
            }

            printWriter.println(token);
        } else if (token.contains("imgdir")) {
            printWriter.println(token);
            status += 1;

            if (status == 5) {
                int level = Integer.parseInt(getName(token)) - 1;
                parsedLevels ^= (1 << level);

                infoTagLevelExp = false;
                infoTagExpState = status;  // status: 5
                processLevelInfoTag(status);
                infoTagExpState = -1;
            }
        } else {
            printWriter.println(token);
        }
    }

    private static void translateLevelExpToken(String token) {
        if (token.contains("/imgdir")) {
            status -= 1;

            if (status < infoTagExpState) {
                if (!infoTagLevelExp) {
                    printUpdatedLevelExp();
                }
            }

            printWriter.println(token);
        } else if (token.contains("imgdir")) {
            printWriter.println(token);
            status += 1;

            forwardCursor(status);
        } else {
            String name = getName(token);
            if (name.contentEquals("exp")) {
                infoTagLevelExp = true;
                printUpdatedLevelExp();
            } else {
                printWriter.println(token);
            }
        }
    }

    private static void translateInfoToken(String token) {
        if (token.contains("/imgdir")) {
            status -= 1;

            if (status < infoTagState) {
                if (!infoTagLevel) {
                    printDefaultLevelTag();
                }
            }

            printWriter.println(token);
        } else if (token.contains("imgdir")) {
            status += 1;
            printWriter.println(token);

            String d = getName(token);
            if (d.contentEquals("level")) {
                infoTagLevel = true;
                translateLevelCursor(status);
            } else {
                forwardCursor(status);
            }
        } else {
            String name = getName(token);

            switch (name) {
                case "cash":
                    if (!getValue(token).contentEquals("0")) {
                        cash = true;
                    }
                    break;

                case "tuc":
                case "incPAD":
                case "incMAD":
                case "incPDD":
                case "incMDD":
                case "incACC":
                case "incEVA":
                case "incSpeed":
                case "incJump":
                case "incMHP":
                case "incMMP":
                case "incSTR":
                case "incDEX":
                case "incINT":
                case "incLUK":
                    if (!getValue(token).contentEquals("0")) {
                        upgradeable = true;
                    }
                    break;
            }

            printWriter.println(token);
        }
    }

    private static boolean translateToken(String token) {
        boolean accessInfoTag = false;

        if (token.contains("/imgdir")) {
            status -= 1;
            printWriter.println(token);
        } else if (token.contains("imgdir")) {
            printWriter.println(token);
            status += 1;

            if (status == 2) {
                String d = getName(token);
                if (!d.contentEquals("info")) {
                    forwardCursor(status);
                } else {
                    accessInfoTag = true;
                }
            } else if (status > 2) {
                forwardCursor(status);
            }
        } else {
            printWriter.println(token);
        }

        return accessInfoTag;
    }

    private static void copyCashItemData(Path file, String curPath) throws IOException {
        try (PrintWriter pw = new PrintWriter(
                Files.newOutputStream(OUTPUT_DIRECTORY.resolve(curPath).resolve(file.getFileName())));
                BufferedReader br = Files.newBufferedReader(file);) {
            printWriter = pw;
            bufferedReader = br;
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                printWriter.println(line);
            }
        }
    }

    private static void parseEquipData(Path file, String curPath) throws IOException {
        try (PrintWriter pw = new PrintWriter(
                Files.newOutputStream(OUTPUT_DIRECTORY.resolve(curPath).resolve(file.getFileName())));
                BufferedReader br = Files.newBufferedReader(file);) {
            printWriter = pw;
            bufferedReader = br;
            status = 0;
            upgradeable = false;
            cash = false;

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (translateToken(line)) {
                    infoTagState = status; // status: 2
                    translateInfoTag(status);
                    infoTagState = -1;
                }
            }
            printFileFooter();
        } catch (RuntimeException e) {
            copyCashItemData(file, curPath);
        }
    }

    private static void printFileFooter() {
        printWriter.println("<!--");
        printWriter.println(" # WZ XML File parsed by the MapleEquipmentOmnilever feature by Ronan Lana.");
        printWriter.println(" # Generated data takes into account info from the server-side WZ.xmls.");
        printWriter.println("-->");
    }

    private static void parseDirectoryEquipData(String curPath) {
        Path folder = OUTPUT_DIRECTORY.resolve(curPath);
        if (!Files.exists(folder)) {
            try {
                Files.createDirectory(folder);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                System.out.println("Unable to create folder " + folder.toAbsolutePath() + ".");
                e.printStackTrace();
            }
        }

        System.out.println("Parsing directory '" + curPath + "'");
        folder = INPUT_DIRECTORY.resolve(curPath);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    try {
                        parseEquipData(path, curPath);
                    } catch (FileNotFoundException ex) {
                        System.out.println("Unable to open dojo file " + path.toAbsolutePath() + ".");
                    } catch (IOException ex) {
                        System.out.println("Error reading dojo file " + path.toAbsolutePath() + ".");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    parseDirectoryEquipData(curPath + path.getFileName() + "/");
                }
            }
        } catch (IOException e1) {
            System.out.println("Unable to read folder " + folder.toAbsolutePath() + ".");
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Instant instantStarted = Instant.now();
        parseDirectoryEquipData("");
        Instant instantStopped = Instant.now();
        Duration durationBetween = Duration.between(instantStarted, instantStopped);
        System.out.println("Get elapsed time in milliseconds: " + durationBetween.toMillis());
        System.out.println("Get elapsed time in seconds: " + durationBetween.toSeconds());
    }
}
