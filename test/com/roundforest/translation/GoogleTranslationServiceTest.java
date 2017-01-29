package com.roundforest.translation;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GoogleTranslationServiceTest {

    private final Database database = mock(Database.class);
    private final HttpClient httpClient = mock(HttpClient.class);
    private final ExecutorService pool = mock(ExecutorService.class);

    @Before
    public void setup() {
        reset(database, httpClient, pool);
    }

    @Test
    public void testMessageIsTranslated() throws Exception {
        GoogleTranslationService service = new GoogleTranslationService(httpClient, database, pool);
        when(pool.submit(any(Runnable.class))).thenAnswer(
                (Answer<Object>) invocation -> {
                    ((Runnable) invocation.getArguments()[0]).run();
                    return null;
                });
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
        response.setEntity(new StringEntity("{text: 'something in french'}"));
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);
        service.translate("Hello, world!");
        verify(database).save("Hello, world!", "something in french");
    }

    @Test
    public void testMessageIsNotSavedIfNotOK() throws Exception {
        GoogleTranslationService service = new GoogleTranslationService(httpClient, database, pool);
        when(pool.submit(any(Runnable.class))).thenAnswer(
                (Answer<Object>) invocation -> {
                    ((Runnable) invocation.getArguments()[0]).run();
                    return null;
                });
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 404, "Not Found"));
        response.setEntity(new StringEntity("{text: 'something in french'}"));
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);
        service.translate("Hello, world!");
        verify(database, never()).save(any(), any());
    }

    @Test
    public void testMessageIsNotSavedIfCorruptedJson() throws Exception {
        GoogleTranslationService service = new GoogleTranslationService(httpClient, database, pool);
        when(pool.submit(any(Runnable.class))).thenAnswer(
                (Answer<Object>) invocation -> {
                    ((Runnable) invocation.getArguments()[0]).run();
                    return null;
                });
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "Not Found"));
        response.setEntity(new StringEntity("{text: 'something in french'"));
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);
        service.translate("Hello, world!");
        verify(database, never()).save(any(), any());
    }
}
