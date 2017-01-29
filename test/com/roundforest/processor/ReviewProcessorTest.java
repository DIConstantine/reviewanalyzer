package com.roundforest.processor;

import com.roundforest.counter.Counter;
import com.roundforest.counter.SimpleCounter;
import com.roundforest.processor.Header;
import com.roundforest.processor.InvalidFormatException;
import com.roundforest.processor.ReviewProcessor;
import com.roundforest.translation.TranslationService;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ReviewProcessorTest {

    private final TranslationService service = mock(TranslationService.class);
    private final Counter counter = mock(Counter.class);

    @Before
    public void setup() {
        reset(service, counter);
    }

    @Test
    public void testCSVIsCorrectlyParsed() throws IOException, InvalidFormatException {
        String csv = "Id,ProductId,UserId,ProfileName,HelpfulnessNumerator,HelpfulnessDenominator,Score,Time,Summary,Text\n" +
                "1,B001E4KFG0,A3SGXH7AUHU8GW,delmartian,1,1,5,1303862400,Good Quality Dog Food,description\n";

        InputStream stream = new ByteArrayInputStream(csv.getBytes());
        SimpleCounter counter = new SimpleCounter();
        new ReviewProcessor(stream, service, Collections.singletonMap(Header.Id, counter)).process();
        verify(service).translate("description");
        assertEquals(Collections.singletonMap("1", 1), counter.get());
    }

    @Test(expected = InvalidFormatException.class)
    public void testCorruptedHeaderIsNotParsed() throws IOException, InvalidFormatException {
        InputStream stream = new ByteArrayInputStream("Corrupted".getBytes());
        new ReviewProcessor(stream, service, Collections.emptyMap()).process();
    }

    @Test
    public void testNoReviewsForEmptyData() throws IOException, InvalidFormatException {
        InputStream stream = new ByteArrayInputStream("Id,ProductId,UserId,ProfileName,HelpfulnessNumerator,HelpfulnessDenominator,Score,Time,Summary,Text\n".getBytes());
        new ReviewProcessor(stream, service, Collections.singletonMap(Header.HelpfulnessNumerator, counter)).process();
        verify(service, never()).translate(any());
        verify(counter, never()).accumulate(any());
    }

    @Test(expected = InvalidFormatException.class)
    public void testWrongNumberOfColumns() throws IOException, InvalidFormatException {
        InputStream stream = new ByteArrayInputStream("Id,ProductId,UserId,ProfileName,HelpfulnessNumerator,HelpfulnessDenominator,Score,Time,Summary,Text\na,b,c".getBytes());
        new ReviewProcessor(stream, service, Collections.emptyMap()).process();
    }

    @Test
    public void testEscapeCharactersAreProcessedCorrectly() throws IOException, InvalidFormatException {
        String csv = "Id,ProductId,UserId,ProfileName,HelpfulnessNumerator,HelpfulnessDenominator,Score,Time,Summary,Text\n" +
                "1,B001E4KFG0,A3SGXH7AUHU8GW,delmartian,1,1,5,1303862400,Good Quality Dog Food,\"long description :\\\"\n" +
                "2,B001E4KFG0,A3SGXH7AUHU8GW,delmartian,1,1,5,1303862400,Good Quality Dog Food,description\n";

        InputStream stream = new ByteArrayInputStream(csv.getBytes());
        SimpleCounter counter = new SimpleCounter();
        new ReviewProcessor(stream, service, Collections.singletonMap(Header.UserId, counter)).process();
        verify(service).translate("description");
        assertEquals(Collections.singletonMap("A3SGXH7AUHU8GW", 2), counter.get());
    }
}
