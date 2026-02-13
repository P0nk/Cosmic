package scripting;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.script.ScriptEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled("Excluding from the build because these tests take too long to run. Can run on demand if needed.")
public class ScriptEvaluationTest {
    private AbstractScriptManager scriptManager = new AbstractScriptManager() {};

    @BeforeAll
    static void muteGraal() {
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
    }

    private static List<String> eventScriptFilePaths() throws IOException {
        return getScriptFilePaths("event");
    }

    private static List<String> itemScriptFilePaths() throws IOException {
        return getScriptFilePaths("item");
    }

    private static List<String> npcScriptFilePaths() throws IOException {
        return getScriptFilePaths("npc");
    }

    private static List<String> portalScriptFilePaths() throws IOException {
        return getScriptFilePaths("portal");
    }

    private static List<String> questScriptFilePaths() throws IOException {
        return getScriptFilePaths("quest");
    }

    private static List<String> reactorScriptFilePaths() throws IOException {
        return getScriptFilePaths("reactor");
    }

    private static List<String> getScriptFilePaths(final String scriptsSubdirectory) throws IOException {
        Path scriptDirectory = Path.of("scripts", scriptsSubdirectory);
        try (Stream<Path> pathStream = Files.walk(scriptDirectory)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .map(path -> "%s/%s".formatted(scriptsSubdirectory, path.getFileName().toString()))
                    .toList();
        }
    }

    @ParameterizedTest
    @MethodSource("eventScriptFilePaths")
    void eventScriptShouldEvaluate(String eventScriptPath) {
        ScriptEngine scriptEngine = scriptManager.getInvocableScriptEngine(eventScriptPath);

        assertNotNull(scriptEngine);
    }

    @ParameterizedTest
    @MethodSource("itemScriptFilePaths")
    void itemScriptShouldEvaluate(String itemScriptPath) {
        ScriptEngine scriptEngine = scriptManager.getInvocableScriptEngine(itemScriptPath);

        assertNotNull(scriptEngine);
    }

    @ParameterizedTest
    @MethodSource("npcScriptFilePaths")
    void npcScriptShouldEvaluate(String npcScriptPath) {
        ScriptEngine scriptEngine = scriptManager.getInvocableScriptEngine(npcScriptPath);

        assertNotNull(scriptEngine);
    }

    @ParameterizedTest
    @MethodSource("portalScriptFilePaths")
    void portalScriptShouldEvaluate(String portalScriptPath) {
        ScriptEngine scriptEngine = scriptManager.getInvocableScriptEngine(portalScriptPath);

        assertNotNull(scriptEngine);
    }

    @ParameterizedTest
    @MethodSource("questScriptFilePaths")
    void questScriptShouldEvaluate(String questScriptPath) {
        ScriptEngine scriptEngine = scriptManager.getInvocableScriptEngine(questScriptPath);

        assertNotNull(scriptEngine);
    }

    @ParameterizedTest
    @MethodSource("reactorScriptFilePaths")
    void reactorScriptShouldEvaluate(String reactorScriptPath) {
        ScriptEngine scriptEngine = scriptManager.getInvocableScriptEngine(reactorScriptPath);

        assertNotNull(scriptEngine);
    }
}
