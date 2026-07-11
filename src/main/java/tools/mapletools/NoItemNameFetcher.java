package tools.mapletools;

import provider.*;
import provider.wz.WZFiles;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author RonanLana
 * <p>
 * This application finds itemids with inexistent name and description from
 * within the server-side XMLs, then identify them on a report file along
 * with a XML excerpt to be appended on the String.wz xml nodes. This program
 * assumes all equipids are depicted using 8 digits and item using 7 digits.
 * <p>
 * Estimated parse time: 2 minutes
 */
public class NoItemNameFetcher {
    private static final Path OUTPUT_FILE = ToolConstants.getOutputFile("no_item_name_result.txt");
    private static final Path OUTPUT_XML_FILE = ToolConstants.getOutputFile("no_item_name_xml.txt");

    private static final Map<Integer, String> itemsWzPath = new HashMap<>();
    private static final Map<Integer, EquipType> equipTypes = new HashMap<>();
    private static final Map<Integer, ItemType> itemsWithNoNameProperty = new HashMap<>();
    private static final Set<Integer> equipsWithNoCashProperty = new HashSet<>();
    private static final Map<Integer, String> nameContentCache = new HashMap<>();
    private static final Map<Integer, String> descContentCache = new HashMap<>();

    private static PrintWriter printWriter = null;
    private static ItemType curType = ItemType.UNDEF;

    private enum ItemType {
        UNDEF, CASH, CONSUME, EQP, ETC, INS, PET
    }

    private enum EquipType {
        UNDEF, ACCESSORY, CAP, CAPE, COAT, FACE, GLOVE, HAIR, LONGCOAT, PANTS, PETEQUIP, RING, SHIELD, SHOES, TAMING, WEAPON
    }

    private static void processStringSubdirectoryData(Data subdirData, String subdirPath) {
        for (Data md : subdirData.getChildren()) {
            try {
                Data nameData = md.getChildByPath("name");
                Data descData = md.getChildByPath("desc");

                int itemId = Integer.parseInt(md.getName());
                if (nameData != null && descData != null) {
                    itemsWithNoNameProperty.remove(itemId);
                } else {
                    if (nameData != null) {
                        nameContentCache.put(itemId, DataTool.getString("name", md));
                    } else if (descData != null) {
                        descContentCache.put(itemId, DataTool.getString("desc", md));
                    }

                    System.out.println("Found itemid on String.wz with no full property: " + subdirPath + subdirData.getName() + "/" + md.getName());
                }
            } catch (NumberFormatException nfe) {
                System.out.println("Error reading string image: " + subdirPath + subdirData.getName() + "/" + md.getName());
            }
        }
    }

    private static void readStringSubdirectoryData(Data subdirData, int depth, String subdirPath) {
        if (depth > 0) {
            for (Data mDir : subdirData.getChildren()) {
                readStringSubdirectoryData(mDir, depth - 1, subdirPath + mDir.getName() + "/");
            }
        } else {
            processStringSubdirectoryData(subdirData, subdirPath);
        }
    }

    private static void readStringSubdirectoryData(Data subdirData, int depth) {
        readStringSubdirectoryData(subdirData, depth, "");
    }

    private static void readStringWZData() {
        System.out.println("Parsing String.wz...");
        DataProvider stringData = DataProviderFactory.getDataProvider(WZFiles.STRING);

        Data cashStringData = stringData.getData("Cash.img");
        readStringSubdirectoryData(cashStringData, 0);

        Data consumeStringData = stringData.getData("Consume.img");
        readStringSubdirectoryData(consumeStringData, 0);

        Data eqpStringData = stringData.getData("Eqp.img");
        readStringSubdirectoryData(eqpStringData, 2);

        Data etcStringData = stringData.getData("Etc.img");
        readStringSubdirectoryData(etcStringData, 1);

        Data insStringData = stringData.getData("Ins.img");
        readStringSubdirectoryData(insStringData, 0);

        Data petStringData = stringData.getData("Pet.img");
        readStringSubdirectoryData(petStringData, 0);
    }

    private static boolean isTamingMob(int itemId) {
        int itemType = itemId / 1000;
        return itemType == 1902 || itemType == 1912;
    }

    private static boolean isAccessory(int itemId) {
        return itemId >= 1110000 && itemId < 1140000;
    }

    private static ItemType getItemTypeFromDirectoryName(String dirName) {
        return switch (dirName) {
            case "Cash" -> ItemType.CASH;
            case "Consume" -> ItemType.CONSUME;
            case "Etc" -> ItemType.ETC;
            case "Install" -> ItemType.INS;
            case "Pet" -> ItemType.PET;
            default -> ItemType.UNDEF;
        };
    }

    private static EquipType getEquipTypeFromDirectoryName(String dirName) {
        return switch (dirName) {
            case "Accessory" -> EquipType.ACCESSORY;
            case "Cap" -> EquipType.CAP;
            case "Cape" -> EquipType.CAPE;
            case "Coat" -> EquipType.COAT;
            case "Face" -> EquipType.FACE;
            case "Glove" -> EquipType.GLOVE;
            case "Hair" -> EquipType.HAIR;
            case "Longcoat" -> EquipType.LONGCOAT;
            case "Pants" -> EquipType.PANTS;
            case "PetEquip" -> EquipType.PETEQUIP;
            case "Ring" -> EquipType.RING;
            case "Shield" -> EquipType.SHIELD;
            case "Shoes" -> EquipType.SHOES;
            case "TamingMob" -> EquipType.TAMING;
            case "Weapon" -> EquipType.WEAPON;
            default -> EquipType.UNDEF;
        };
    }

    private static String getStringDirectoryNameFromEquipType(EquipType eType) {
        return switch (eType) {
            case ACCESSORY -> "Accessory";
            case CAP -> "Cap";
            case CAPE -> "Cape";
            case COAT -> "Coat";
            case FACE -> "Face";
            case GLOVE -> "Glove";
            case HAIR -> "Hair";
            case LONGCOAT -> "Longcoat";
            case PANTS -> "Pants";
            case PETEQUIP -> "PetEquip";
            case RING -> "Ring";
            case SHIELD -> "Shield";
            case SHOES -> "Shoes";
            case TAMING -> "Taming";
            case WEAPON -> "Weapon";
            default -> "Undefined";
        };
    }

    private static void readEquipNodeData(DataProvider data, DataDirectoryEntry mDir, String wzFileName, String dirName) {
        EquipType eqType = getEquipTypeFromDirectoryName(dirName);

        for (DataFileEntry mFile : mDir.getFiles()) {
            String fileName = mFile.getName();

            try {
                int itemId = Integer.parseInt(fileName.substring(0, 8));
                itemsWithNoNameProperty.put(itemId, curType);
                equipTypes.put(itemId, eqType);

                itemsWzPath.put(itemId, wzFileName + "/" + dirName + "/" + fileName);

                if (!isAccessory(itemId) && !isTamingMob(itemId)) {
                    try {
                        Data fileData = data.getData(dirName + "/" + fileName);
                        Data mdinfo = fileData.getChildByPath("info");
                        if (mdinfo.getChildByPath("cash") == null) {
                            equipsWithNoCashProperty.add(itemId);
                        }
                    } catch (NullPointerException npe) {
                        System.out.println("[SEVERE] " + mFile.getName() + " failed to load. Issue: " + npe.getMessage() + "\n\n");
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    private static void readEquipWZData() {
        String wzFileName = "Character.wz";

        DataProvider data = DataProviderFactory.getDataProvider(WZFiles.CHARACTER);
        DataDirectoryEntry root = data.getRoot();

        System.out.println("Parsing " + wzFileName + "...");
        for (DataDirectoryEntry mDir : root.getSubdirectories()) {
            String dirName = mDir.getName();
            if (dirName.contentEquals("Dragon")) {
                continue;
            }

            readEquipNodeData(data, mDir, wzFileName, dirName);
        }
    }

    private static void readItemWZData() {
        String wzFileName = "Item.wz";

        DataProvider data = DataProviderFactory.getDataProvider(WZFiles.ITEM);
        DataDirectoryEntry root = data.getRoot();

        System.out.println("Parsing " + wzFileName + "...");
        for (DataDirectoryEntry mDir : root.getSubdirectories()) {
            String dirName = mDir.getName();
            if (dirName.contentEquals("Special")) {
                continue;
            }

            curType = getItemTypeFromDirectoryName(dirName);
            if (!dirName.contentEquals("Pet")) {
                for (DataFileEntry mFile : mDir.getFiles()) {
                    String fileName = mFile.getName();

                    Data fileData = data.getData(dirName + "/" + fileName);
                    for (Data mData : fileData.getChildren()) {
                        try {
                            int itemId = Integer.parseInt(mData.getName());
                            itemsWithNoNameProperty.put(itemId, curType);
                            itemsWzPath.put(itemId, wzFileName + "/" + dirName + "/" + fileName);
                        } catch (Exception e) {
                            System.out.println("EXCEPTION on '" + mData.getName() + "' " + wzFileName + "/" + dirName + "/" + fileName);
                        }
                    }
                }
            } else {
                readEquipNodeData(data, mDir, wzFileName, dirName);
            }
        }
    }

    private static void printReportFileHeader() {
        printWriter.println(" # Report File autogenerated from the MapleInvalidItemWithNoNameFetcher feature by Ronan Lana.");
        printWriter.println(" # Generated data takes into account several data info from the server-side WZ.xmls.");
        printWriter.println();
    }

    private static void printReportFileResults() {
        if (!itemsWithNoNameProperty.isEmpty()) {
            printWriter.println("Itemids with missing 'name' property: ");

            List<Integer> itemids = new ArrayList<>(itemsWithNoNameProperty.keySet());
            Collections.sort(itemids);

            for (Integer itemid : itemids) {
                printWriter.println("  " + itemid + " " + itemsWzPath.get(itemid));
            }
            printWriter.println();
        }

        if (!equipsWithNoCashProperty.isEmpty()) {
            printWriter.println("Equipids with missing 'cash' property: ");

            List<Integer> itemids = new ArrayList<>(equipsWithNoCashProperty);
            Collections.sort(itemids);

            for (Integer itemid : itemids) {
                printWriter.println("  " + itemid + " " + itemsWzPath.get(itemid));
            }
        }
    }

    private static Map<String, List<Integer>> filterMissingItemNames() {
        List<Integer> cashList = new ArrayList<>(20);
        List<Integer> consList = new ArrayList<>(20);
        List<Integer> eqpList = new ArrayList<>(20);
        List<Integer> etcList = new ArrayList<>(20);
        List<Integer> insList = new ArrayList<>(20);
        List<Integer> petList = new ArrayList<>(20);

        for (Map.Entry<Integer, ItemType> ids : itemsWithNoNameProperty.entrySet()) {
            switch (ids.getValue()) {
                case CASH -> cashList.add(ids.getKey());
                case CONSUME -> consList.add(ids.getKey());
                case EQP -> eqpList.add(ids.getKey());
                case ETC -> etcList.add(ids.getKey());
                case INS -> insList.add(ids.getKey());
                case PET -> petList.add(ids.getKey());
            }
        }

        Map<String, List<Integer>> nameTags = new HashMap<>();
        nameTags.put("Cash.img", cashList);
        nameTags.put("Consume.img", consList);
        nameTags.put("Eqp.img", eqpList);
        nameTags.put("Etc.img", etcList);
        nameTags.put("Ins.img", insList);
        nameTags.put("Pet.img", petList);

        return nameTags;
    }

    private static void printOutputFileHeader() {
        printWriter.println(" # XML File autogenerated from the MapleInvalidItemWithNoNameFetcher feature by Ronan Lana.");
        printWriter.println(" # Generated data takes into account several data info from the server-side WZ.xmls.");
        printWriter.println();
    }

    private static String getMissingEquipName(int itemid) {
        String s = nameContentCache.get(itemid);
        if (s == null) {
            s = "MISSING NAME " + itemid;
        }

        return s;
    }

    private static String getMissingEquipDesc(int itemid) {
        String s = descContentCache.get(itemid);
        if (s == null && itemid >= 2000000) {   // thanks Halcyon for noticing "missing info" on equips
            s = "MISSING INFO " + itemid;
        }

        return s;
    }

    private static void writeMissingEquipInfo(Integer itemid) {
        printWriter.println("      <imgdir name=\"" + itemid + "\">");

        String s;
        s = getMissingEquipName(itemid);
        printWriter.println("        <string name=\"name\" value=\"" + s + "\"/>");

        s = getMissingEquipDesc(itemid);
        printWriter.println("        <string name=\"desc\" value=\"" + s + "\"/>");
        printWriter.println("      </imgdir>");
    }

    private static void writeEquipSubdirectoryHeader(EquipType eType) {
        printWriter.println("    <imgdir name=\"" + getStringDirectoryNameFromEquipType(eType) + "\">");
    }

    private static void writeEquipSubdirectoryFooter() {
        printWriter.println("    </imgdir>");
    }

    private static void writeEquipXMLHeader() {
        printWriter.println("  <imgdir name=\"Eqp\">");
    }

    private static void writeEquipXMLFooter() {
        printWriter.println("  </imgdir>");
    }

    private static void writeMissingItemInfo(Integer itemid) {
        printWriter.println("  <imgdir name=\"" + itemid + "\">");
        printWriter.println("    <string name=\"name\" value=\"MISSING NAME\"/>");
        printWriter.println("    <string name=\"desc\" value=\"MISSING INFO\"/>");
        printWriter.println("  </imgdir>");
    }

    private static void writeXMLHeader(String fileName) {
        printWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        printWriter.println("<imgdir name=\"" + fileName + "\">");
    }

    private static void writeXMLFooter() {
        printWriter.println("</imgdir>");
    }

    private static void writeMissingEquipWZNode(EquipType eType, List<Integer> missingNames) {
        if (!missingNames.isEmpty()) {
            Collections.sort(missingNames);
            writeEquipSubdirectoryHeader(eType);

            for (Integer equipid : missingNames) {
                writeMissingEquipInfo(equipid);
            }

            writeEquipSubdirectoryFooter();
        }
    }

    private static void writeMissingStringWZNode(String nodePath, List<Integer> missingNames, boolean isEquip) {
        if (!missingNames.isEmpty()) {
            if (!isEquip) {
                Collections.sort(missingNames);

                printWriter.println(nodePath + ":");
                printWriter.println();

                writeXMLHeader(nodePath);

                for (Integer i : missingNames) {
                    writeMissingItemInfo(i);
                }

                writeXMLFooter();

                printWriter.println();
            } else {
                int arraySize = EquipType.values().length;

                List<Integer>[] equips = new List[arraySize];
                for (int i = 0; i < arraySize; i++) {
                    equips[i] = new ArrayList<>(42);
                }

                for (Integer itemid : missingNames) {
                    equips[equipTypes.get(itemid).ordinal()].add(itemid);
                }

                printWriter.println(nodePath + ":");
                printWriter.println();

                writeXMLHeader(nodePath);
                writeEquipXMLHeader();

                for (EquipType eType : EquipType.values()) {
                    writeMissingEquipWZNode(eType, equips[eType.ordinal()]);
                }

                writeEquipXMLFooter();
                writeXMLFooter();

                printWriter.println();
            }
        }
    }

    private static void writeMissingStringWZNames(Map<String, List<Integer>> missingNames) throws Exception {
        System.out.println("Writing remaining 'String.wz' names...");
        try (PrintWriter pw = new PrintWriter(Files.newOutputStream(OUTPUT_XML_FILE))) {
            printWriter = pw;

            printOutputFileHeader();

            String[] nodePaths = { "Cash.img", "Consume.img", "Eqp.img", "Etc.img", "Ins.img", "Pet.img" };
            for (int i = 0; i < nodePaths.length; i++) {
                writeMissingStringWZNode(nodePaths[i], missingNames.get(nodePaths[i]), i == 2);
            }

        }
    }

    public static void main(String[] args) {
        try (PrintWriter pw = new PrintWriter(Files.newOutputStream(OUTPUT_FILE))) {
            printWriter = pw;
            curType = ItemType.EQP;
            readEquipWZData();

            curType = ItemType.UNDEF;
            readItemWZData();
            readStringWZData(); // calculates the diff and effectively holds all items with no name property on the WZ

            System.out.println("Reporting results...");
            printReportFileHeader();
            printReportFileResults();

            Map<String, List<Integer>> missingNames = filterMissingItemNames();
            writeMissingStringWZNames(missingNames);

            System.out.println("Done!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
