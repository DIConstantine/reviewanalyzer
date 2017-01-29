package com.roundforest.counter;

import org.apache.commons.lang3.mutable.MutableInt;

import java.util.*;
import java.util.stream.Collectors;

public class SimpleCounter implements Counter {

    protected final Map<String, MutableInt> map = new HashMap<>();

    @Override
    public void accumulate(String value) {
        map.computeIfAbsent(value, k -> new MutableInt()).increment();
    }

    @Override
    public Map<String, Integer> get() {
        return map.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue()));
    }

    @Override
    public Map<String, Integer> top(int limit) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .limit(limit)
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue(), (e1, e2) -> e1, LinkedHashMap::new));
    }
}
