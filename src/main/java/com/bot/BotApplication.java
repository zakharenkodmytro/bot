package com.bot;

import com.bot.configuration.WebSocketConfiguration;
import com.bot.model.subscription.SubscriptionRequest;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static com.bot.commons.Constants.TIMEOUT;
import static com.bot.utils.SerializationUtil.toJson;
import static java.lang.String.format;

@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan("com.bot")
@Slf4j
public class BotApplication extends SpringBootServletInitializer {

    private static WebSocketConfiguration webSocketConfiguration;

    @Autowired
    public void setWebSocketConfiguration(WebSocketConfiguration webSocketConfiguration) {
        BotApplication.webSocketConfiguration = webSocketConfiguration;
    }

    public static WebSocketClient getWebSocketClient() {
        return webSocketClient;
    }

    private static WebSocketClient webSocketClient;

    @Autowired
    public void setWebSocketClient(WebSocketClient webSocketClient) {
        BotApplication.webSocketClient = webSocketClient;
    }

    public static void main(final String[] args) throws InterruptedException {
        SpringApplication.run(BotApplication.class, args);

        log.info("----------- Starting bot -------------");

        webSocketClient.connect();
        while (!webSocketClient.isOpen()) {
            log.info("------- Waiting for connection to websocket server -------");
            TimeUnit.MILLISECONDS.sleep(TIMEOUT);
        }

        //send message
        if (webSocketClient.isOpen()) {
            webSocketConfiguration.getProducts().stream().forEach(product -> {

                SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
                subscriptionRequest.setSubscribeTo(Collections.singletonList(format("trading.product.%s", product.getProductId())));
                String message = toJson(subscriptionRequest);
                webSocketClient.send(message);
            });
        }
    }
}
