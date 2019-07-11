package com.bot.configuration.server;

import com.bot.configuration.WebSocketConfiguration;
import com.bot.configuration.server.mock.WsMockServer;
import com.bot.model.Product;
import com.bot.service.EventServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@Slf4j
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ApplicationTest {

    @Autowired
    public WebSocketClient webServiceClient;

    @Autowired
    public EventServiceImpl eventService;

    @Autowired
    public WebSocketConfiguration webSocketConfiguration;

    public Product product;

    @BeforeClass
    public static void config() throws IOException {
        if (WsMockServer.server == null) {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> WsMockServer.startWsServer());
        }
    }

    @Before
    public void setup() throws InterruptedException {
        TimeUnit.SECONDS.sleep(1);

        if (!webServiceClient.isOpen()) {
            webServiceClient.connect();
        }

        while (!webServiceClient.isOpen()) {
            TimeUnit.MILLISECONDS.sleep(200);
            log.info("------- Waiting for connection to websocket server -------");
        }

        product = webSocketConfiguration.getProducts().get(0);
    }

    @Test
    public void contextLoads() {
        log.info("Context loaded ");
    }
}