package com.roundforest.counter;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class WordsCounterTest {

    @Test
    public void testWordsAreSplit() {
        Counter counter = new WordsCounter();
        counter.accumulate("Hello, world! And hello again");
        Map<String, Integer> result = counter.get();
        Map<String, Integer> expected = new HashMap<String, Integer>() {{
            put("hello", 2);
            put("world", 1);
            put("and", 1);
            put("again", 1);
        }};
        assertEquals(expected, result);
    }

    @Test
    public void testTopMostWords() {
        Counter counter = new WordsCounter();
        counter.accumulate("A B A C B C a d a");
        Map<String, Integer> result = counter.top(2);
        Map<String, Integer> expected = new HashMap<String, Integer>() {{
            put("a", 4);
            put("b", 2);
        }};
        assertEquals(expected, result);
    }
}
