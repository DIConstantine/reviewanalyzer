package com.roundforest.counter;

import java.util.*;
import java.util.regex.Pattern;

public class WordsCounter extends SimpleCounter {

    //TODO improve regex
    private static final Pattern WORDS = Pattern.compile("[\\W0-9]+");

    @Override
    public void accumulate(String text) {
        String[] words = WORDS.split(text);
        Arrays.stream(words).forEach(word -> {
            if (!word.isEmpty())
                super.accumulate(word.toLowerCase());
        });
    }
}
