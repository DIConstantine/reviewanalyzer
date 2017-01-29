package com.roundforest.counter;

import java.util.Map;

public interface Counter {
    void accumulate(String data);
    Map<String, Integer> get();
    Map<String, Integer> top(int limit);
}
