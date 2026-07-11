package tools.mapletools;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author RonanLana
 * <p>
 * This application reads metadata for the gachapons found on the "gachapon_items.txt"
 * recipe file, then checks up the Handbook DB (installed through MapleIdRetriever)
 * and translates the item names from the recipe file into their respective itemids.
 * The translated itemids are then stored in specific gachapon files inside the
 * "lib/gachapons" folder.
 * <p>
 * Estimated parse time: 1 minute
 */
public class GachaponItemIdRetriever {
    private static final Path INPUT_FILE = ToolConstants.getInputFile("gachapon_items.txt");
    private static final Path OUTPUT_DIRECTORY = ToolConstants.getOutputFile("gachapons");
    private static final Connection con = SimpleDatabaseConnection.getConnection();
    private static final Pattern pattern = Pattern.compile("(\\d*)%");
    private static final int[] scrollsChances = new int[]{10, 15, 30, 60, 65, 70, 100};
    private static final Map<GachaponScroll, List<Integer>> scrollItemids = new HashMap<>();

    private static PrintWriter printWriter = null;

    private static void insertGachaponScrollItemid(Integer id, String name, String description, boolean both) {
        GachaponScroll gachaScroll = getGachaponScroll(name, description, both);

        List<Integer> list = scrollItemids.get(gachaScroll);
        if (list == null) {
            list = new LinkedList<>();
            scrollItemids.put(gachaScroll, list);
        }

        list.add(id);
    }

    private static void loadHandbookUseNames() throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT * FROM `handbook` WHERE `id` >= 2040000 AND `id` < 2050000 ORDER BY `id` ASC;");
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Integer id = rs.getInt("id");
            String name = rs.getString("name");

            if (isUpgradeScroll(name)) {
                String description = rs.getString("description");
                insertGachaponScrollItemid(id, name, description, false);
                insertGachaponScrollItemid(id, name, description, true);
            }
        }

        rs.close();
        ps.close();

        /*
        for (Entry<GachaponScroll, List<Integer>> e : scrollItemids.entrySet()) {
            System.out.println(e);
        }
        System.out.println("------------");
        */
    }

    private static class GachaponScroll {
        private String header;
        private String target;
        private String buff;
        private int prop;

        private GachaponScroll(GachaponScroll from, int prop) {
            this.header = from.header;
            this.target = from.target;
            this.buff = from.buff;
            this.prop = prop;
        }

        private GachaponScroll(String name, String description, boolean both) {
            String[] params = name.split(" for ");
            if (params.length < 3) {
                return;
            }

            String header = both ? "scroll" : " " + params[0];
            String target = params[1];

            int prop = 0;
            String buff = params[2];

            Matcher m = pattern.matcher(buff);
            if (m.find()) {
                prop = Integer.parseInt(m.group(1));
                buff = buff.substring(0, m.start() - 1).trim();
            } else {
                m = pattern.matcher(description);

                if (m.find()) {
                    prop = Integer.parseInt(m.group(1));
                }
            }

            int idx = buff.indexOf(" (");   // remove percentage & dots from name checking
            if (idx > -1) {
                buff = buff.substring(0, idx);
            }
            buff = buff.replace(".", "");

            this.header = header;
            this.target = target;
            this.buff = buff;
            this.prop = prop;
        }

        @Override
        public int hashCode() {
            int result = prop ^ (prop >>> 32);
            result = 31 * result + (header != null ? header.hashCode() : 0);
            result = 31 * result + (target != null ? target.hashCode() : 0);
            result = 31 * result + (buff != null ? buff.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            GachaponScroll sc = (GachaponScroll) o;
            if (header != null ? !header.equals(sc.header) : sc.header != null) {
                return false;
            }
            if (target != null ? !target.equals(sc.target) : sc.target != null) {
                return false;
            }
            if (buff != null ? !buff.equals(sc.buff) : sc.buff != null) {
                return false;
            }
            return prop == sc.prop;
        }

        @Override
        public String toString() {
            return header + " for " + target + " for " + buff + " - " + prop + "%";
        }

    }

    private static String getGachaponScrollResults(String line, boolean both) {
        String str = "";
        List<GachaponScroll> gachaScrollList;

        GachaponScroll gachaScroll = getGachaponScroll(line, "", both);
        if (gachaScroll.prop != 0) {
            gachaScrollList = Collections.singletonList(gachaScroll);
        } else {
            gachaScrollList = new ArrayList<>(scrollsChances.length);

            for (int prop : scrollsChances) {
                gachaScrollList.add(new GachaponScroll(gachaScroll, prop));
            }
        }

        for (GachaponScroll gs : gachaScrollList) {
            List<Integer> gachaItemids = scrollItemids.get(gs);
            if (gachaItemids != null) {
                String listStr = "";
                for (Integer id : gachaItemids) {
                    listStr += id.toString();
                    listStr += " ";
                }

                if (gachaItemids.size() > 1) {
                    str += "[" + listStr + "]";
                } else {
                    str += listStr;
                }
            }
        }

        return str;
    }

    private static GachaponScroll getGachaponScroll(String name, String description, boolean both) {
        name = name.toLowerCase();
        name = name.replace("for acc ", "for accuracy ");
        name = name.replace("blunt weapon", "bw");
        name = name.replace("eye eqp.", "eye accessory");
        name = name.replace("face eqp.", "face accessory");
        name = name.replace("for attack", "for att");
        name = name.replace("1-handed", "one-handed");
        name = name.replace("2-handed", "two-handed");

        return new GachaponScroll(name, description, both);
    }

    private static boolean isUpgradeScroll(String name) {
        return name.matches("^(([D|d]ark )?[S|s]croll for).*");
    }

    private static void fetchLineOnMapleHandbook(String line, String rarity) throws SQLException {
        String str = "";
        if (!isUpgradeScroll(line)) {
            PreparedStatement ps = con.prepareStatement("SELECT `id` FROM `handbook` WHERE `name` LIKE ? ORDER BY `id` ASC;");
            ps.setString(1, line);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");

                str += Integer.toString(id);
                str += " ";
            }

            rs.close();
            ps.close();
        } else {
            str += getGachaponScrollResults(line, false);
            if (str.isEmpty()) {
                str += getGachaponScrollResults(line, true);

                if (str.isEmpty()) {
                    System.out.println("NONE for '" + line + "' : " + getGachaponScroll(line, "", false));
                }
            }
        }

        if (str.isEmpty()) {
            str += line;
        }

        if (rarity != null) {
            str += ("- " + rarity);
        }

        printWriter.println(str);
    }

    private static void fetchDataOnMapleHandbook() throws SQLException {
        String line;
        try (BufferedReader bufferedReader = Files.newBufferedReader(INPUT_FILE)) {
            int skip = 0;
            boolean lineHeader = false;
            while ((line = bufferedReader.readLine()) != null) {
                if (skip > 0) {
                    skip--;

                    if (lineHeader) {
                        if (!line.isEmpty()) {
                            lineHeader = false;
                            printWriter.println();
                            printWriter.println(line + ":");
                        }
                    }
                } else if (line.isEmpty()) {
                    printWriter.println("");
                } else if (line.startsWith("Gachapon ")) {
                    String[] s = line.split("� ");
                    String gachaponName = s[s.length - 1];
                    gachaponName = gachaponName.replace(" ", "_");
                    gachaponName = gachaponName.toLowerCase();

                    if (printWriter != null) {
                        printWriter.close();
                    }
                    Path outputFile = OUTPUT_DIRECTORY.resolve(gachaponName + ".txt");
                    setupDirectories(outputFile);

                    printWriter = new PrintWriter(Files.newOutputStream(outputFile));

                    skip = 2;
                    lineHeader = true;
                } else if (line.startsWith(".")) {
                    skip = 1;
                    lineHeader = true;
                } else {
                    line = line.replace("�", "'");
                    for (String item : line.split("\\s\\|\\s")) {
                        item = item.trim();
                        if (!item.contentEquals("n/a")) {
                            String[] itemInfo = item.split(" - ");
                            fetchLineOnMapleHandbook(itemInfo[0], itemInfo.length > 1 ? itemInfo[1] : null);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static void setupDirectories(Path file) {
        if (!Files.exists(file.getParent())) {
            try {
                Files.createDirectories(file.getParent());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try (con) {
            loadHandbookUseNames();
            fetchDataOnMapleHandbook();
        } catch (SQLException e) {
            System.out.println("Error: invalid SQL syntax");
            System.out.println(e.getMessage());
        }
    }
}

