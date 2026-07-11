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
 * This application parses the Quest.wz file inputted and generates a report showing
 * all cases where a quest requires an item, but doesn't take them, which may happen
 * because the node representing the item doesn't have a "count" clause.
 * <p>
 * Running it should generate a report file under "output" folder with the search results.
 */
public class QuestItemCountFetcher {
    private static final Path OUTPUT_FILE = ToolConstants.getOutputFile("quest_item_count_report.txt");
    private static final String ACT_NAME = WZFiles.QUEST.getFilePath() + "/Act.img.xml";
    private static final String CHECK_NAME = WZFiles.QUEST.getFilePath() + "/Check.img.xml";
    private static final int INITIAL_STRING_LENGTH = 50;

    private static final Map<Integer, Map<Integer, Integer>> checkItems = new HashMap<>();
    private static final Map<Integer, Map<Integer, Integer>> actItems = new HashMap<>();

    private static PrintWriter printWriter = null;
    private static BufferedReader bufferedReader = null;
    private static byte status = 0;
    private static int questId = -1;
    private static int isCompleteState = 0;
    private static int curItemId;
    private static int curItemCount;

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

    private static void readItemLabel(String token) {
        String name = getName(token);
        String value = getValue(token);

        switch (name) {
            case "id" -> curItemId = Integer.parseInt(value);
            case "count" -> curItemCount = Integer.parseInt(value);
        }
    }

    private static void commitQuestItemPair(Map<Integer, Map<Integer, Integer>> map) {
        Map<Integer, Integer> list = map.get(questId);
        if (list == null) {
            list = new LinkedHashMap<>();
            map.put(questId, list);
        }

        list.put(curItemId, curItemCount);
    }

    private static void translateTokenAct(String token) {
        String d;

        if (token.contains("/imgdir")) {
            status -= 1;

            if (status == 4) {
                if (curItemCount == Integer.MAX_VALUE && isCompleteState == 1) {
                    commitQuestItemPair(actItems);
                }
            }
        } else if (token.contains("imgdir")) {
            if (status == 1) {           //getting QuestId
                d = getName(token);
                questId = Integer.parseInt(d);
            } else if (status == 2) {      //start/complete
                d = getName(token);
                isCompleteState = Integer.parseInt(d);
            } else if (status == 3) {
                if (!token.contains("item")) {
                    forwardCursor(status);
                }
            } else if (status == 4) {
                curItemId = Integer.MAX_VALUE;
                curItemCount = Integer.MAX_VALUE;
            }

            status += 1;
        } else {
            if (status == 5) {
                readItemLabel(token);
            }
        }
    }

    private static void translateTokenCheck(String token) {
        String d;

        if (token.contains("/imgdir")) {
            status -= 1;

            if (status == 4) {
                Map<Integer, Integer> missedItems = actItems.get(questId);

                if (missedItems != null && missedItems.containsKey(curItemId) && isCompleteState == 1) {
                    commitQuestItemPair(checkItems);
                }
            }
        } else if (token.contains("imgdir")) {
            if (status == 1) {           //getting QuestId
                d = getName(token);
                questId = Integer.parseInt(d);
            } else if (status == 2) {      //start/complete
                d = getName(token);
                isCompleteState = Integer.parseInt(d);
            } else if (status == 3) {
                if (!token.contains("item")) {
                    forwardCursor(status);
                }
            } else if (status == 4) {
                curItemId = Integer.MAX_VALUE;
                curItemCount = Integer.MAX_VALUE;
            }

            status += 1;
        } else {
            if (status == 5) {
                readItemLabel(token);
            }
        }
    }

    private static void readQuestItemCountData() throws IOException {
        String line;

        InputStreamReader fileReader = new InputStreamReader(new FileInputStream(ACT_NAME), StandardCharsets.UTF_8);
        bufferedReader = new BufferedReader(fileReader);

        while ((line = bufferedReader.readLine()) != null) {
            translateTokenAct(line);
        }

        bufferedReader.close();
        fileReader.close();

        fileReader = new InputStreamReader(new FileInputStream(CHECK_NAME), StandardCharsets.UTF_8);
        bufferedReader = new BufferedReader(fileReader);

        while ((line = bufferedReader.readLine()) != null) {
            translateTokenCheck(line);
        }

        bufferedReader.close();
        fileReader.close();
    }

    private static void printReportFileHeader() {
        printWriter.println(" # Report File autogenerated from the MapleQuestItemCountFetcher feature by Ronan Lana.");
        printWriter.println(" # Generated data takes into account several data info from the server-side WZ.xmls.");
        printWriter.println();
    }

    private static void printReportFileResults() {
        List<Pair<Integer, Pair<Integer, Integer>>> reports = new ArrayList<>();
        List<Pair<Integer, Integer>> notChecked = new ArrayList<>();

        for (Map.Entry<Integer, Map<Integer, Integer>> actItem : actItems.entrySet()) {
            int questid = actItem.getKey();

            for (Map.Entry<Integer, Integer> actData : actItem.getValue().entrySet()) {
                int itemid = actData.getKey();

                Map<Integer, Integer> checkData = checkItems.get(questid);
                if (checkData != null) {
                    Integer count = checkData.get(itemid);
                    if (count != null) {
                        reports.add(new Pair<>(questid, new Pair<>(itemid, -count)));
                    }
                } else {
                    notChecked.add(new Pair<>(questid, itemid));
                }
            }
        }

        for (Pair<Integer, Pair<Integer, Integer>> r : reports) {
            printWriter.println("Questid " + r.left + " : Itemid " + r.right.left + " should have qty " + r.right.right);
        }

        for (Pair<Integer, Integer> r : notChecked) {
            printWriter.println("Questid " + r.left + " : Itemid " + r.right + " is unchecked");
        }
    }

    private static void reportQuestItemCountData() {
        // This will reference one line at a time

        try (PrintWriter pw = new PrintWriter(Files.newOutputStream(OUTPUT_FILE))) {
            System.out.println("Reading WZs...");
            readQuestItemCountData();

            System.out.println("Reporting results...");
            printWriter = pw;

            printReportFileHeader();
            printReportFileResults();

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
        reportQuestItemCountData();
    }
}
