package tools;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NextIntTest {

    @Test
    void dropQuantityShouldIncludeEntireRange() {
        Map<Integer, Integer> rands;
        final int rounds = 100_000;
        for (int min = 1; min < 100; min++) {
            for (int max = min; max < 100; max++) {
                rands = new HashMap<>();
                for (int i = 0; i < rounds; i++) {
                    int randomValue = Randomizer.nextInt(max - min + 1) + min;
                    rands.compute(randomValue, (k, v) -> v == null ? 0 : v + 1);
                }

                assertFalse(rands.containsKey(min - 1));
                for (int i = min; i <= max; i++) {
                    assertTrue(rands.containsKey(i));
                }
                assertFalse(rands.containsKey(max + 1));
            }
        }
    }
}
