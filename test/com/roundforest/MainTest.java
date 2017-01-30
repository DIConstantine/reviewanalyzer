package com.roundforest;

import com.roundforest.counter.Counter;
import com.roundforest.counter.SimpleCounter;
import com.roundforest.processor.Header;
import com.roundforest.processor.ReviewProcessor;
import com.roundforest.counter.WordsCounter;
import com.roundforest.translation.Database;
import com.roundforest.translation.GoogleTranslationService;
import com.roundforest.translation.TranslationService;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainTest {

    private static final String REVIEWS_CSV = "Reviews.csv";

    public static void main(String[] args) {
        String fileName = REVIEWS_CSV;
        if (args.length != 0 && args[0].endsWith(".csv"))
            fileName = args[0];

        if (!Files.exists(Paths.get(fileName)) && !download(fileName)) {
            System.out.println("Cannot download file. Please pass filename as parameter");
            System.exit(1);
        }

        try (FileInputStream stream = new FileInputStream(fileName);
             GoogleTranslationService service = new GoogleTranslationService(HTTP_CLIENT, new Database() {
                 final PrintWriter out = new PrintWriter("translated.csv");
                 final AtomicInteger id = new AtomicInteger();

                 @Override
                 public void save(String original, String translated) {
                     out.println(id.incrementAndGet() + "," + translated);
                 }
             })
        ) {
            System.out.println("Start processing file...");
            long t = System.currentTimeMillis();
            WordsCounter wordsCounter = new WordsCounter();
            SimpleCounter usersCounter = new SimpleCounter();
            SimpleCounter productsCounter = new SimpleCounter();

            Map<Header, Counter> counters = new HashMap<>();
            counters.put(Header.UserId, usersCounter);
            counters.put(Header.ProductId, productsCounter);
            counters.put(Header.Text, wordsCounter);
            new ReviewProcessor(stream, Arrays.stream(args).filter("translate=true"::equalsIgnoreCase).count() > 0 ? service : text -> {}, counters).process();

            Map<String, Integer> topUsers = usersCounter.top(1000);
            Map<String, Integer> topProducts = productsCounter.top(1000);
            Map<String, Integer> topWords = wordsCounter.top(1000);
            System.out.format("Time spent %d\n", System.currentTimeMillis() - t);
            System.out.format("Free memory %d\n", Runtime.getRuntime().freeMemory());
            System.out.format("Top users %s\n", topUsers);
            System.out.format("Top products %s\n", topProducts);
            System.out.format("Top words %s\n", topWords);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean download(String fileName) {
        try {
            System.out.format("%s file not found. Trying to download...\n", fileName);
            HttpGet get = new HttpGet("https://www.kaggle.com/snap/amazon-fine-food-reviews/downloads/amazon-fine-foods-release-2016-01-08-20-34-54.zip");
            get.setHeader("Cookie", "__utma=158690720.1842156152.1485281493.1485281493.1485467698.2; __utmb=158690720.3.9.1485467722235; __utmz=158690720.1485281493.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); .ASPXAUTH=898E50E7C2F02BED0DFFA3E1597A99D35A953E251889737EFA6C6EA62F187CEB2F6B97B4F8CEE4BE744311504A7A25015E965DF06440EEF7E95080D75A701E0838655A1B3445ED6136808FBFCB1A1E4F729E91F4; __utmc=158690720; ARRAffinity=44c5132e978845fa61ced6d6d985047b24150da46800c630d0b75a0d6d962ffb; __RequestVerificationToken=9c-TAIkpj_8ewKWoCAcMyw5OrNgVgz_9_s-mPzVGCDDmRRuqGinvnSBYRU6GuldCKFBpe-vyluX7l8Zy6qZbUWtmD1Y1; TempData=.OThEMddTDdWst0mF6oPvS1A2oklZVhRvI6xPyYXu5WTFM5qWUOSPm4cQeZs/8aKKUuF6uzarh0iG1RgRA1O1x6DuLgk=");
            HttpResponse response = HttpClients.createDefault().execute(get);
            HttpEntity entity;
            StatusLine status;
            if (response != null &&
                    (status = response.getStatusLine()) != null &&
                    status.getStatusCode() == 200 &&
                    (entity = response.getEntity()) != null) {
                ZipInputStream zip = new ZipInputStream(entity.getContent());
                ZipEntry entry;
                do {
                    entry = zip.getNextEntry();
                } while (!entry.getName().endsWith(REVIEWS_CSV));

                FileOutputStream file = new FileOutputStream(REVIEWS_CSV);
                byte[] data = new byte[4096];
                do {
                    int i = zip.read(data);
                    if (i < 0) break;
                    file.write(data, 0, i);
                } while (zip.available() > 0);
                System.out.format("%s file successfully downloaded", REVIEWS_CSV);
                return true;
            }
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
        }
        return false;
    }

    //region Http client
    private static final HttpResponse RESPONSE = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));

    static {
        try {
            RESPONSE.setEntity(new StringEntity("{text: 'something in french'}"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private static final int AVERAGE_RESPONSE_TIME = 200;
    private static final double FACTOR = 0.1;
    private static final int OFFSET = (int) (AVERAGE_RESPONSE_TIME * FACTOR);
    private static final int HALF_OFFSET = OFFSET >> 1;

    private static final HttpClient HTTP_CLIENT = new HttpClient() {

        private final Random random = new Random();

        @Override
        public HttpParams getParams() {
            return null;
        }

        @Override
        public ClientConnectionManager getConnectionManager() {
            return null;
        }

        @Override
        public HttpResponse execute(HttpUriRequest request) throws IOException {
            try {
                Thread.sleep(AVERAGE_RESPONSE_TIME + random.nextInt(OFFSET) - HALF_OFFSET);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return RESPONSE;
        }

        @Override
        public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException {
            return null;
        }

        @Override
        public HttpResponse execute(HttpHost target, HttpRequest request) throws IOException {
            return null;
        }

        @Override
        public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
            return null;
        }

        @Override
        public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException {
            return null;
        }

        @Override
        public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException {
            return null;
        }

        @Override
        public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler) throws IOException {
            return null;
        }

        @Override
        public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException {
            return null;
        }
    };
    //endregion
}

