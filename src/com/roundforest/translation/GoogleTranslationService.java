package com.roundforest.translation;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;

public class GoogleTranslationService implements TranslationService, AutoCloseable {
    private static final String INPUT_LANG = "en";
    private static final String OUTPUT_LANG = "fr";
    private static final int TIMEOUT = 500;
    private static final int POOL_SIZE = 100;
    private static final int CAPACITY = POOL_SIZE << 1;

    private static class Message {
        private final String input_lang;
        private final String output_lang;
        private final String text;

        private Message(String input_lang, String output_lang, String text) {
            this.input_lang = input_lang;
            this.output_lang = output_lang;
            this.text = text;
        }
    }

    private static class TranslatedMessage {
        private String text;
    }

    private static final RejectedExecutionHandler HANDLER = (r, e) -> {
        try {
            e.getQueue().put(r);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    };

    private final HttpClient httpClient;
    private final Database database;
    private final ExecutorService pool;
    private final Gson gson = new Gson();

    public GoogleTranslationService(Database database) {
        this(HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(TIMEOUT)
                        .setConnectionRequestTimeout(TIMEOUT)
                        .setSocketTimeout(TIMEOUT)
                        .build())
                .build(), database);
    }

    public GoogleTranslationService(HttpClient client, Database database) {
        this(client, database, new ThreadPoolExecutor(POOL_SIZE, POOL_SIZE, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(CAPACITY), HANDLER));
    }

    GoogleTranslationService(HttpClient httpClient, Database database, ExecutorService pool) {
        this.httpClient = httpClient;
        this.database = database;
        this.pool = pool;
    }

    @Override
    public void translate(String text) {
        pool.submit(() -> {
            String json = gson.toJson(new Message(INPUT_LANG, OUTPUT_LANG, text));
            HttpResponse response = send(json);
            processResponse(response, text);
        });
    }

    private void processResponse(HttpResponse response, String text) {
        try {
            HttpEntity entity;
            StatusLine status;
            if (response != null &&
                    (status = response.getStatusLine()) != null &&
                    status.getStatusCode() == 200 &&
                    (entity = response.getEntity()) != null) {
                TranslatedMessage translated = gson.fromJson(EntityUtils.toString(entity), TranslatedMessage.class);
                database.save(text, translated.text);
            }
        } catch (IOException | IllegalStateException | JsonSyntaxException e) {
            e.printStackTrace();
        }
    }

    private HttpResponse send(String json) {
        try {
            HttpPost post = new HttpPost(new URI("https://api.google.com/translate"));
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(json));
            return httpClient.execute(post);
        } catch (IOException | IllegalStateException | URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void close() {
        pool.shutdown();
    }
}
