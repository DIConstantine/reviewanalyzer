package com.roundforest.counter;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class SimpleCounterTest {
    @Test
    public void testAllAccumulated() {
        Counter counter = new SimpleCounter();
        for (String value : Arrays.asList("B", "B", "C", "A", "B", "A"))
            counter.accumulate(value);

        Map<String, Integer> expected = new HashMap<>();
        expected.put("A", 2);
        expected.put("B", 3);
        expected.put("C", 1);

        Map<String, Integer> result = counter.get();
        assertEquals(expected, result);
    }

    @Test
    public void testTopMost() {
        Counter counter = new SimpleCounter();
        for (String value : Arrays.asList("B", "B", "C", "A", "B", "A"))
            counter.accumulate(value);

        Map<String, Integer> result = counter.top(2);
        assertArrayEquals(new String[] {"A", "B"}, result.keySet().toArray());
        assertArrayEquals(new Integer[] {2, 3}, result.values().toArray());
    }
}
