package tools.mapletools;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author RonanLana
 * <p>
 * This application objective is to read all scripts from the event folder
 * and fill empty functions for every function name not yet present in the
 * script.
 * <p>
 * Estimated parse time: 10 seconds
 */
public class EventMethodFiller {
    private static final Collection<String> RELEVANT_FILE_EXTENSIONS = Set.of("sql", "js", "txt", "java");

    private static boolean foundMatchingDataOnFile(String fileContent, Pattern pattern) {
        Matcher matcher = pattern.matcher(fileContent);
        return matcher.find();
    }

    private static void filterDirectorySearchMatchingData(String directoryPath, Map<Pattern, String> functions)
            throws IOException {
        Files.walk(Path.of(directoryPath))
                .filter(EventMethodFiller::isRelevantFile)
                .forEach(path -> fileSearchMatchingData(path, functions));
    }

    private static boolean isRelevantFile(Path path) {
        String fileName = path.getFileName().toString();
        return RELEVANT_FILE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private static void fileSearchMatchingData(Path file, Map<Pattern, String> functions) {
        try {
            String fileContent = Files.readString(file);
            List<String> fillFunctions = new LinkedList<>();

            for (Map.Entry<Pattern, String> f : functions.entrySet()) {
                if (!foundMatchingDataOnFile(fileContent, f.getKey())) {
                    fillFunctions.add(f.getValue());
                }
            }

            if (!fillFunctions.isEmpty()) {
                System.out.println("Filling out " + file.getFileName().toString() + "...");

                try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(file, StandardOpenOption.APPEND))) {
                    printWriter.println();
                    printWriter.println();
                    printWriter.println("// ---------- FILLER FUNCTIONS ----------");
                    printWriter.println();

                    for (String s : fillFunctions) {
                        printWriter.println(s);
                        printWriter.println();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Pattern compileJsFunctionPattern(String function) {
        String jsFunction = "function(\\s)+";
        return Pattern.compile(jsFunction + function);
    }

    private static Map<Pattern, String> getFunctions() {
        Map<Pattern, String> functions = new HashMap<>();
        functions.put(compileJsFunctionPattern("playerEntry"), "function playerEntry(eim, player) {}");
        functions.put(compileJsFunctionPattern("playerExit"), "function playerExit(eim, player) {}");
        functions.put(compileJsFunctionPattern("scheduledTimeout"), "function scheduledTimeout(eim) {}");
        functions.put(compileJsFunctionPattern("playerUnregistered"), "function playerUnregistered(eim, player) {}");
        functions.put(compileJsFunctionPattern("changedLeader"), "function changedLeader(eim, leader) {}");
        functions.put(compileJsFunctionPattern("monsterKilled"), "function monsterKilled(mob, eim) {}");
        functions.put(compileJsFunctionPattern("allMonstersDead"), "function allMonstersDead(eim) {}");
        functions.put(compileJsFunctionPattern("playerDisconnected"), "function playerDisconnected(eim, player) {}");
        functions.put(compileJsFunctionPattern("monsterValue"), "function monsterValue(eim, mobid) {return 0;}");
        functions.put(compileJsFunctionPattern("dispose"), "function dispose() {}");
        functions.put(compileJsFunctionPattern("leftParty"), "function leftParty(eim, player) {}");
        functions.put(compileJsFunctionPattern("disbandParty"), "function disbandParty(eim, player) {}");
        functions.put(compileJsFunctionPattern("clearPQ"), "function clearPQ(eim) {}");
        functions.put(compileJsFunctionPattern("afterSetup"), "function afterSetup(eim) {}");
        functions.put(compileJsFunctionPattern("cancelSchedule"), "function cancelSchedule() {}");
        functions.put(compileJsFunctionPattern("setup"), "function setup(eim, leaderid) {}");
        //put(compileJsFunctionPattern("getEligibleParty"), "function getEligibleParty(party) {}"); not really needed
        return functions;
    }

    public static void main(String[] args) throws IOException {
        filterDirectorySearchMatchingData(ToolConstants.SCRIPTS_PATH + "/event", getFunctions());
    }

}
