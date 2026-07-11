package tools.mapletools;

import provider.wz.WZFiles;
import tools.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author RonanLana
 * <p>
 * This application parses the Map.wz file inputted and reports areas (mapids) that are supposed to be referenced
 * throughout the map tree (area map -> continent map -> world map) but are currently missing.
 */
public class WorldmapChecker {
    private static final Path OUTPUT_FILE = ToolConstants.getOutputFile("worldmap_report.txt");
    private static final int INITIAL_STRING_LENGTH = 50;
    private static final Map<String, Set<Integer>> worldMapids = new HashMap<>();
    private static final Map<String, String> parentWorldmaps = new HashMap<>();
    private static final Set<String> rootWorldmaps = new HashSet<>();

    private static PrintWriter printWriter = null;
    private static BufferedReader bufferedReader = null;
    private static Set<Integer> currentWorldMapids;
    private static String currentParent;
    private static byte status = 0;
    private static boolean isInfo;

    private static String getName(String token) {
        int i, j;
        char[] dest;
        String d;

        i = token.lastIndexOf("name");
        i = token.indexOf("\"", i) + 1; //lower bound of the string
        j = token.indexOf("\"", i);     //upper bound

        dest = new char[INITIAL_STRING_LENGTH];
        token.getChars(i, j, dest, 0);

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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void simpleToken(String token) {
        if (token.contains("/imgdir")) {
            status -= 1;
        } else if (token.contains("imgdir")) {
            status += 1;
        }
    }

    private static void translateToken(String token) {
        String d;

        if (token.contains("/imgdir")) {
            status -= 1;
        } else if (token.contains("imgdir")) {
            status += 1;

            if (status == 2) {
                d = getName(token);

                switch (d) {
                    case "MapList" -> isInfo = false;
                    case "info" -> isInfo = true;
                    default -> forwardCursor(status);
                }
            } else if (status == 4) {
                d = getName(token);

                if (!d.contentEquals("mapNo")) {
                    forwardCursor(status);
                }
            }
        } else {
            if (status == 4) {
                currentWorldMapids.add(Integer.valueOf(getValue(token)));
            } else if (status == 2 && isInfo) {
                try {
                    d = getName(token);
                    if (d.contentEquals("parentMap")) {
                        currentParent = (getValue(token) + ".img.xml");
                    } else {
                        forwardCursor(status);
                    }
                } catch (Exception e) {
                    System.out.println("failed '" + token + "'");

                }
            }
        }
    }

    private static void parseWorldmapFile(File worldmapFile) throws IOException {
        String line;

        InputStreamReader fileReader = new InputStreamReader(new FileInputStream(worldmapFile), StandardCharsets.UTF_8);
        bufferedReader = new BufferedReader(fileReader);

        currentParent = "";
        status = 0;

        currentWorldMapids = new HashSet<>();
        while ((line = bufferedReader.readLine()) != null) {
            translateToken(line);
        }

        String worldmapName = worldmapFile.getName();
        worldMapids.put(worldmapName, currentWorldMapids);

        if (!currentParent.isEmpty()) {
            parentWorldmaps.put(worldmapName, currentParent);
        } else {
            rootWorldmaps.add(worldmapName);
        }

        bufferedReader.close();
        fileReader.close();
    }

    private static void parseWorldmapDirectory() {
        File folder = new File(WZFiles.MAP.getFilePath(), "WorldMap");
        System.out.println("Parsing directory '" + folder.getPath() + "'");
        for (File file : folder.listFiles()) {
            if (file.isFile()) {
                try {
                    parseWorldmapFile(file);
                } catch (FileNotFoundException ex) {
                    System.out.println("Unable to open worldmap file " + file.getAbsolutePath() + ".");
                } catch (IOException ex) {
                    System.out.println("Error reading worldmap file " + file.getAbsolutePath() + ".");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void printReportFileHeader() {
        printWriter.println(" # Report File autogenerated from the MapleWorldmapChecker feature by Ronan Lana.");
        printWriter.println(" # Generated data takes into account several data info from the server-side WZ.xmls.");
        printWriter.println();
    }

    private static void printReportFileResults(List<Pair<String, List<Pair<Integer, String>>>> results) {
        printWriter.println("Missing mapid references in top hierarchy:\n");
        for (Pair<String, List<Pair<Integer, String>>> res : results) {
            printWriter.println("'" + res.getLeft() + "':");

            for (Pair<Integer, String> i : res.getRight()) {
                printWriter.println("  " + i);
            }

            printWriter.println("\n");
        }
    }

    private static void verifyWorldmapTreeMapids() {
        try (PrintWriter pw = new PrintWriter(Files.newOutputStream(OUTPUT_FILE))) {
            printWriter = pw;
            printReportFileHeader();

            if (rootWorldmaps.size() > 1) {
                printWriter.println("[WARNING] Detected several root worldmaps: " + rootWorldmaps + "\n");
            }

            Set<String> worldmaps = new HashSet<>(parentWorldmaps.keySet());
            worldmaps.addAll(rootWorldmaps);

            Map<String, Set<Integer>> tempMapids = new HashMap<>(worldMapids.size());
            for (Map.Entry<String, Set<Integer>> e : worldMapids.entrySet()) {
                tempMapids.put(e.getKey(), new HashSet<>(e.getValue()));
            }

            Map<String, List<Pair<Integer, String>>> unreferencedMapids = new HashMap<>();

            for (String s : worldmaps) {
                List<Pair<Integer, String>> currentUnreferencedMapids = new ArrayList<>();

                for (Integer i : tempMapids.get(s)) {
                    String parent = parentWorldmaps.get(s);

                    while (parent != null) {
                        Set<Integer> mapids = worldMapids.get(parent);
                        if (!mapids.contains(i)) {
                            currentUnreferencedMapids.add(new Pair<>(i, parent));
                            break;
                        } else {
                            tempMapids.get(parent).remove(i);
                        }

                        parent = parentWorldmaps.get(parent);
                    }
                }

                if (!currentUnreferencedMapids.isEmpty()) {
                    unreferencedMapids.put(s, currentUnreferencedMapids);
                }
            }

            if (!unreferencedMapids.isEmpty()) {
                List<Pair<String, List<Pair<Integer, String>>>> unreferencedEntries = new ArrayList<>(20);
                for (Map.Entry<String, List<Pair<Integer, String>>> e : unreferencedMapids.entrySet()) {
                    List<Pair<Integer, String>> list = new ArrayList<>(e.getValue());
                    list.sort((o1, o2) -> o1.getLeft().compareTo(o2.getLeft()));

                    unreferencedEntries.add(new Pair<>(e.getKey(), list));
                }

                unreferencedEntries.sort((o1, o2) -> o1.getLeft().compareTo(o2.getLeft()));

                printReportFileResults(unreferencedEntries);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        parseWorldmapDirectory();
        verifyWorldmapTreeMapids();
    }
}

