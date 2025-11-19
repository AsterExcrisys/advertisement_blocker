package com.asterexcrisys.adblocker;

import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import java.io.IOException;
import java.time.Duration;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HTTPServerIntegrationTests {

    private OkHttpClient client;

    @BeforeAll
    public void setUp() {
        client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(3))
                .callTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .retryOnConnectionFailure(true)
                .build();
    }

    @AfterAll
    public void tearDown() throws IOException {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
        if (client.cache() != null) {
            client.cache().close();
        }
    }

}