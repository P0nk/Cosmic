package tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentConfigTest {

    @Test
    void getShouldReturnDefaultWhenVariableNotFound() {
        String value = EnvironmentConfig.get("NONEXISTENT_VARIABLE_XYZ", "my_default");
        assertEquals("my_default", value);
    }
}
