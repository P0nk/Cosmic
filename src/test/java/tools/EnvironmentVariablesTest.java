package tools;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class EnvironmentVariablesTest {
    @AfterEach
    void tearDown() {
        EnvironmentVariables.setInstance(null);
    }

    @Test
    void instance_providesDefaultImplementation() {
        EnvironmentVariables instance = EnvironmentVariables.instance();
        assertNotNull(instance, "Instance should not be null");
        assertEquals(instance.getAll(), System.getenv(), "getAll() should return all environment variables");
    }

    @Test
    void instance_returnsSameSingleton() {
        EnvironmentVariables first = EnvironmentVariables.instance();
        EnvironmentVariables second = EnvironmentVariables.instance();
        assertSame(first, second, "The singleton should return the same instance on subsequent calls");
    }

    @Test
    void setInstance_allowsMocking() {
        EnvironmentVariables mock = Mockito.mock(EnvironmentVariables.class);
        String dbUser = "user";
        String dbPass = "pass";
        when(mock.getAll()).thenReturn(Map.of("DB_USER", dbUser, "DB_PASS", dbPass));

        EnvironmentVariables.setInstance(mock);

        Map<String, String> vars = EnvironmentVariables.instance().getAll();
        assertEquals(dbUser, vars.get("DB_USER"));
        assertEquals(dbPass, vars.get("DB_PASS"));
    }
}
