package tools.mapletools;

import provider.wz.WZFiles;
import server.ItemInformationProvider;
import tools.DatabaseConnection;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author RonanLana
 * <p>
 * The objective of this program is to uncover all maker data from the
 * ItemMaker.wz.xml files and generate a SQL file with every data info
 * for the Maker DB tables.
 */
// TODO: adjust to new Postgres table structure.
// New column names, and two of the columns should have null instead of 0.
public class SkillMakerFetcher {
    private static final Path INPUT_FILE = WZFiles.ETC.getFile().resolve("ItemMake.img.xml");
    private static final Path OUTPUT_FILE = ToolConstants.getOutputFile("maker-data.sql");
    private static final int INITIAL_STRING_LENGTH = 50;

    private static PrintWriter printWriter = null;
    private static BufferedReader bufferedReader = null;
    private static byte status = 0;
    private static byte state = 0;

    // maker data fields
    private static int id = -1;
    private static int itemid = -1;
    private static int reqLevel = -1;
    private static int reqMakerLevel = -1;
    private static int reqItem = -1;
    private static int reqMeso = -1;
    private static int reqEquip = -1;
    private static int catalyst = -1;
    private static int quantity = -1;
    private static int tuc = -1;

    private static int recipePos = -1;
    private static int recipeProb = -1;
    private static int recipeCount = -1;
    private static int recipeItem = -1;

    static List<int[]> recipeList = null;
    static List<int[]> randomList = null;
    static List<MakerItemEntry> makerList = new ArrayList<>(100);

    private static void resetMakerDataFields() {
        reqLevel = 0;
        reqMakerLevel = 0;
        reqItem = 0;
        reqMeso = 0;
        reqEquip = 0;
        catalyst = 0;
        quantity = 0;
        tuc = 0;

        recipePos = 0;
        recipeProb = 0;
        recipeCount = 0;
        recipeItem = 0;

        recipeList = null;
        randomList = null;
    }

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
        String s = d.trim();
        s.replaceFirst("^0+(?!$)", "");

        return (s);
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
        String s = d.trim();
        s.replaceFirst("^0+(?!$)", "");

        return (s);
    }

    private static int[] generateRecipeItem() {
        int[] pair = new int[2];
        pair[0] = recipeItem;
        pair[1] = recipeCount;

        return pair;
    }

    private static int[] generateRandomItem() {
        int[] tuple = new int[3];
        tuple[0] = recipeItem;
        tuple[1] = recipeCount;
        tuple[2] = recipeProb;

        return tuple;
    }

    private static void simpleToken(String token) {
        if (token.contains("/imgdir")) {
            status -= 1;
        } else if (token.contains("imgdir")) {
            status += 1;
        }
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

    private static void translateToken(String token) {
        String d;

        if (token.contains("/imgdir")) {
            status -= 1;

            if (status == 2) {   //close item maker data
                generateUpdatedItemFee();   // for equipments, this will try to update reqMeso to be conformant with the client.
                makerList.add(new MakerItemEntry(id, itemid, reqLevel, reqMakerLevel, reqItem, reqMeso, reqEquip, catalyst, quantity, tuc, recipeCount, recipeItem, recipeList, randomList));
                resetMakerDataFields();
            } else if (status == 4) {    //close recipe/random item
                if (state == 0) {
                    recipeList.add(generateRecipeItem());
                } else if (state == 1) {
                    randomList.add(generateRandomItem());
                }
            }
        } else if (token.contains("imgdir")) {
            if (status == 1) {           //getting id
                d = getName(token);
                id = Integer.parseInt(d);
                System.out.println("Parsing maker id " + id);
            } else if (status == 2) {      //getting target item id
                d = getName(token);
                itemid = Integer.parseInt(d);
            } else if (status == 3) {
                d = getName(token);

                switch (d) {
                    case "recipe" -> {
                        recipeList = new LinkedList<>();
                        state = 0;
                    }
                    case "randomReward" -> {
                        randomList = new LinkedList<>();
                        state = 1;
                    }
                    default -> forwardCursor(3);   // unused content, read until end of block
                }
            } else if (status == 4) {  // inside recipe/random
                d = getName(token);
                recipePos = Integer.parseInt(d);
            }

            status += 1;
        } else {
            if (status == 3) {
                d = getName(token);

                switch (d) {
                    case "itemNum" -> quantity = Integer.parseInt(getValue(token));
                    case "meso" -> reqMeso = Integer.parseInt(getValue(token));
                    case "reqItem" -> reqItem = Integer.parseInt(getValue(token));
                    case "reqLevel" -> reqLevel = Integer.parseInt(getValue(token));
                    case "reqSkillLevel" -> reqMakerLevel = Integer.parseInt(getValue(token));
                    case "tuc" -> tuc = Integer.parseInt(getValue(token));
                    case "catalyst" -> catalyst = Integer.parseInt(getValue(token));
                    case "reqEquip" -> reqEquip = Integer.parseInt(getValue(token));
                    default -> {
                        System.out.println("Unhandled case: '" + d + "'");
                        state = 2;
                    }
                }
            } else if (status == 5) {  // inside recipe/random item
                d = getName(token);
                if (d.equals("item")) {
                    recipeItem = Integer.parseInt(getValue(token));
                } else {
                    if (state == 0) {
                        recipeCount = Integer.parseInt(getValue(token));
                    } else {
                        if (d.equals("itemNum")) {
                            recipeCount = Integer.parseInt(getValue(token));
                        } else {
                            recipeProb = Integer.parseInt(getValue(token));
                        }
                    }
                }
            }
        }
    }

    private static void generateUpdatedItemFee() {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        float adjPrice = reqMeso;

        if (itemid < 2000000) {
            Map<String, Integer> stats = ii.getEquipStats(itemid);
            if (stats != null) {
                int val = itemid / 100000;

                if (val == 13 || val == 14) {    // is weapon-type
                    adjPrice /= 10;
                    adjPrice += reqMeso;

                    adjPrice /= 1000;
                    reqMeso = 1000 * (int) Math.floor(adjPrice);
                } else {
                    adjPrice /= ((stats.get("reqLevel") >= 108) ? 10 : 11);
                    adjPrice += reqMeso;

                    adjPrice /= 1000;
                    reqMeso = 1000 * (int) Math.ceil(adjPrice);
                }
            } else {
                System.out.println("null stats for itemid " + itemid);
            }
        } else {
            adjPrice /= 10;
            adjPrice += reqMeso;

            adjPrice /= 1000;
            reqMeso = 1000 * (int) Math.ceil(adjPrice);
        }
    }

    private static void WriteMakerTableFile() {
        printWriter.println(" # SQL File autogenerated from the MapleSkillMakerFetcher feature by Ronan Lana.");
        printWriter.println(" # Generated data is conformant with the ItemMake.img.xml file used to compile this.");
        printWriter.println();

        StringBuilder sb_create = new StringBuilder("INSERT IGNORE INTO `makercreatedata` (`id`, `itemid`, `req_level`, `req_maker_level`, `req_meso`, `req_item`, `req_equip`, `catalyst`, `quantity`, `tuc`) VALUES\r\n");
        StringBuilder sb_recipe = new StringBuilder("INSERT IGNORE INTO `makerrecipedata` (`itemid`, `req_item`, `count`) VALUES\r\n");
        StringBuilder sb_reward = new StringBuilder("INSERT IGNORE INTO `makerrewarddata` (`itemid`, `rewardid`, `quantity`, `prob`) VALUES\r\n");

        for (MakerItemEntry it : makerList) {
            sb_create.append("  (" + it.id + ", " + it.itemid + ", " + it.reqLevel + ", " + it.reqMakerLevel + ", " + it.reqMeso + ", " + it.reqItem + ", " + it.reqEquip + ", " + it.catalyst + ", " + it.quantity + ", " + it.tuc + "),\r\n");

            if (it.recipeList != null) {
                for (int[] rit : it.recipeList) {
                    sb_recipe.append("  (" + it.itemid + ", " + rit[0] + ", " + rit[1] + "),\r\n");
                }
            }

            if (it.randomList != null) {
                for (int[] rit : it.randomList) {
                    sb_reward.append("  (" + it.itemid + ", " + rit[0] + ", " + rit[1] + ", " + rit[2] + "),\r\n");
                }
            }
        }

        sb_create.setLength(sb_create.length() - 3);
        sb_create.append(";\r\n");

        sb_recipe.setLength(sb_recipe.length() - 3);
        sb_recipe.append(";\r\n");

        sb_reward.setLength(sb_reward.length() - 3);
        sb_reward.append(";");

        printWriter.println(sb_create);
        printWriter.println(sb_recipe);
        printWriter.println(sb_reward);
    }

	private static void writeMakerTableData() {
		// This will reference one line at a time
        String line = null;

        try (PrintWriter pw = new PrintWriter(Files.newOutputStream(OUTPUT_FILE));
                BufferedReader br = Files.newBufferedReader(INPUT_FILE);) {
            printWriter = pw;
            bufferedReader = br;

            resetMakerDataFields();

            while ((line = bufferedReader.readLine()) != null) {
                translateToken(line);
            }

            WriteMakerTableFile();

        } catch (FileNotFoundException ex) {
            System.out.println("Unable to open file '" + INPUT_FILE + "'");
        } catch (IOException ex) {
            System.out.println("Error reading file '" + INPUT_FILE + "'");
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

    public static void main(String[] args) {
        DatabaseConnection.initializeConnectionPool(); // Using ItemInformationProvider which loads som unrelated things from the db
        writeMakerTableData();
    }
}

