package tools.mapletools;

import server.ItemInformationProvider;
import tools.DatabaseConnection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author RonanLana
 * <p>
 * This application gathers info from the WZ.XML files, fetching all cosmetic coupons and tickets from there, and then
 * searches the NPC script files, identifying the stylish NPCs that supposedly uses them. It will reports all NPCs that
 * uses up a card, as well as report those currently unused.
 * <p>
 * Estimated parse time: 10 seconds
 */
public class CashCosmeticsFetcher {
    private static final Map<Integer, String> scriptEntries = new HashMap<>(500);

    private static void listFiles(String directoryName, ArrayList<File> files) {
        File directory = new File(directoryName);

        // get all the files from a directory
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if (file.isFile()) {
                files.add(file);
            } else if (file.isDirectory()) {
                listFiles(file.getAbsolutePath(), files);
            }
        }
    }

    private static int getNpcIdFromFilename(String name) {
        try {
            return Integer.parseInt(name.substring(0, name.indexOf('.')));
        } catch (Exception e) {
            return -1;
        }
    }

    private static void loadScripts() throws Exception {
        ArrayList<File> files = new ArrayList<>();
        listFiles(ToolConstants.SCRIPTS_PATH + "/npc", files);

        for (File f : files) {
            Integer npcid = getNpcIdFromFilename(f.getName());

            //System.out.println("Parsing " + f.getAbsolutePath());
            InputStreamReader fileReader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            StringBuilder stringBuffer = new StringBuilder();
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line).append("\n");
            }

            scriptEntries.put(npcid, stringBuffer.toString());

            bufferedReader.close();
            fileReader.close();
        }
    }

    private static List<Integer> findItemidOnScript(int itemid) {
        List<Integer> files = new LinkedList<>();
        String t = String.valueOf(itemid);

        for (Map.Entry<Integer, String> text : scriptEntries.entrySet()) {
            if (text.getValue().contains(t)) {
                files.add(text.getKey());
            }
        }

        return files;
    }

    private static void reportCosmeticCouponResults() {
        final ItemInformationProvider ii = ItemInformationProvider.getInstance();
        for (int itemid = 5150000; itemid <= 5154000; itemid++) {
            String itemName = ii.getName(itemid);

            if (itemName != null) {
                List<Integer> npcids = findItemidOnScript(itemid);

                if (!npcids.isEmpty()) {
                    System.out.println("Itemid " + itemid + " found on " + npcids + ". (" + itemName + ")");
                } else {
                    System.out.println("NOT FOUND ITEMID " + itemid + " (" + itemName + ")");
                }
            }
        }
    }

    public static void main(String[] args) {
        DatabaseConnection.initializeConnectionPool(); // ItemInformationProvider loads unrelated stuff from the db
        try {
            loadScripts();
            System.out.println("Loaded scripts");

            reportCosmeticCouponResults();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
