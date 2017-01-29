package com.roundforest.processor;

import com.opencsv.CSVReader;
import com.roundforest.counter.Counter;
import com.roundforest.translation.TranslationService;

import java.io.*;
import java.util.*;

public class ReviewProcessor {

    private final InputStream stream;
    private final TranslationService translationService;
    private final Map<Header, Counter> counters;

    public ReviewProcessor(InputStream stream,
                           TranslationService translationService,
                           Map<Header, Counter> counters) {
        this.translationService = translationService;
        this.stream = stream;
        this.counters = counters;
    }

    public void process() throws InvalidFormatException, IOException {
        //no escape char is needed because of smile before quote :\" which is treated as escaped quote and breaks further lines
        CSVReader reader = new CSVReader(new InputStreamReader(stream), ',', '"', (char) 0);
        String[] headers = reader.readNext();
        if (!Arrays.equals(headers, Arrays.stream(Header.values()).map(Enum::name).toArray()))
            throw new InvalidFormatException();

        for (String[] data : reader) {
            if (data.length != headers.length)
                throw new InvalidFormatException();
            String text = data[Header.Text.ordinal()];
            translationService.translate(text);
            counters.entrySet().forEach(c -> accumulate(data, c.getKey(), c.getValue()));
        }
    }

    private static void accumulate(String[] data, Header header, Counter counter) {
        counter.accumulate(data[header.ordinal()]);
    }
}
