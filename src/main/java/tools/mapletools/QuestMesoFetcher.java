package tools.mapletools;

import provider.wz.WZFiles;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author RonanLana
 * <p>
 * This application parses the Quest.wz file inputted and generates a report showing
 * all cases where a quest takes a meso fee to complete a quest, but it doesn't
 * properly checks the player for the needed amount before completing it.
 * <p>
 * Running it should generate a report file under "output" folder with the search results.
 */
public class QuestMesoFetcher {
    private static final Path OUTPUT_FILE = ToolConstants.getOutputFile("quest_meso_report.txt");
    private static final boolean PRINT_FEES = true;    // print missing values as additional info report
    private static final int INITIAL_STRING_LENGTH = 50;

    private static final Map<Integer, Integer> checkedMesoQuests = new HashMap<>();
    private static final Map<Integer, Integer> appliedMesoQuests = new HashMap<>();
    private static final Set<Integer> checkedEndscriptQuests = new HashSet<>();

    private static PrintWriter printWriter = null;
    private static BufferedReader bufferedReader = null;
    private static byte status = 0;
    private static int questId = -1;
    private static int isCompleteState = 0;
    private static int currentMeso = 0;

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

    private static void translateTokenAct(String token) {
        String d;

        if (token.contains("/imgdir")) {
            status -= 1;
        } else if (token.contains("imgdir")) {
            if (status == 1) {           //getting QuestId
                d = getName(token);
                questId = Integer.parseInt(d);
            } else if (status == 2) {      //start/complete
                d = getName(token);
                isCompleteState = Integer.parseInt(d);
            } else if (status == 3) {
                forwardCursor(status);
            }

            status += 1;
        } else {
            if (token.contains("money")) {
                if (isCompleteState != 0) {
                    d = getValue(token);

                    currentMeso = -1 * Integer.parseInt(d);

                    if (currentMeso > 0) {
                        appliedMesoQuests.put(questId, currentMeso);
                    }
                }
            }
        }
    }

    private static void translateTokenCheck(String token) {
        String d;

        if (token.contains("/imgdir")) {
            status -= 1;
        } else if (token.contains("imgdir")) {
            if (status == 1) {           //getting QuestId
                d = getName(token);
                questId = Integer.parseInt(d);
            } else if (status == 2) {      //start/complete
                d = getName(token);
                isCompleteState = Integer.parseInt(d);
            } else if (status == 3) {
                forwardCursor(status);
            }

            status += 1;
        } else {
            if (token.contains("endmeso")) {
                d = getValue(token);
                currentMeso = Integer.parseInt(d);

                checkedMesoQuests.put(questId, currentMeso);
            } else if (token.contains("endscript")) {
                checkedEndscriptQuests.add(questId);
            }
        }
    }

    private static void readQuestMesoData() throws IOException {
        String line;

        bufferedReader = Files.newBufferedReader(WZFiles.QUEST.getFile().resolve("Act.img.xml"));

        while ((line = bufferedReader.readLine()) != null) {
            translateTokenAct(line);
        }

        bufferedReader.close();

        bufferedReader = Files.newBufferedReader(WZFiles.QUEST.getFile().resolve("Check.img.xml"));

        while ((line = bufferedReader.readLine()) != null) {
            translateTokenCheck(line);
        }

        bufferedReader.close();
    }

    private static void printReportFileHeader() {
        printWriter.println(" # Report File autogenerated from the MapleQuestMesoFetcher feature by Ronan Lana.");
        printWriter.println(" # Generated data takes into account several data info from the server-side WZ.xmls.");
        printWriter.println();
    }

    private static void printReportFileResults(Map<Integer, Integer> target, Map<Integer, Integer> base, boolean testingCheck) {
        List<Integer> result = new ArrayList<>();
        List<Integer> error = new ArrayList<>();

        Map<Integer, Integer> questFee = new HashMap<>();

        for (Map.Entry<Integer, Integer> e : base.entrySet()) {
            Integer v = target.get(e.getKey());

            if (v == null) {
                if (testingCheck || !checkedEndscriptQuests.contains(e.getKey())) {
                    result.add(e.getKey());
                    questFee.put(e.getKey(), e.getValue());
                }
            } else if (v.intValue() != e.getValue().intValue()) {
                error.add(e.getKey());
            }
        }

        if (!result.isEmpty() || !error.isEmpty()) {
            printWriter.println("MISMATCH INFORMATION ON '" + (testingCheck ? "check" : "act") + "':");
            if (!result.isEmpty()) {
                result.sort((o1, o2) -> o1 - o2);

                printWriter.println("# MISSING");

                if (!PRINT_FEES) {
                    for (Integer i : result) {
                        printWriter.println(i);
                    }
                } else {
                    for (Integer i : result) {
                        printWriter.println(i + " " + questFee.get(i));
                    }
                }

                printWriter.println();
            }

            if (!error.isEmpty() && testingCheck) {
                error.sort((o1, o2) -> o1 - o2);

                printWriter.println("# WRONG VALUE");

                for (Integer i : error) {
                    printWriter.println(i);
                }

                printWriter.println();
            }

            printWriter.println("\r\n");
        }
    }

    private static void reportQuestMesoData() {
        // This will reference one line at a time

        try (PrintWriter pw = new PrintWriter(Files.newOutputStream(OUTPUT_FILE))) {
            System.out.println("Reading WZs...");
            readQuestMesoData();

            System.out.println("Reporting results...");
            // report missing meso checks on quest completes
            printWriter = pw;

            printReportFileHeader();

            printReportFileResults(checkedMesoQuests, appliedMesoQuests, true);
            printReportFileResults(appliedMesoQuests, checkedMesoQuests, false);

            System.out.println("Done!");
        } catch (FileNotFoundException ex) {
            System.out.println("Unable to open quest file.");
        } catch (IOException ex) {
            System.out.println("Error reading quest file.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        reportQuestMesoData();
    }
}

