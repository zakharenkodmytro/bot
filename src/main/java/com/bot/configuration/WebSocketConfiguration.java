package com.bot.configuration;

import com.bot.model.Product;
import com.bot.model.subscription.ErrorResponse;
import com.bot.model.subscription.SubscriptionEventResponse;
import com.bot.service.EventService;
import com.bot.service.EventServiceImpl;
import com.bot.utils.OptionalConsumer;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bot.commons.Constants.*;
import static com.bot.utils.EventsConverter.safeConvertRaw;
import static com.bot.utils.SerializationUtil.fromObject;
import static com.bot.utils.Utils.getHttpHeaders;


@Configuration
@ConfigurationProperties("bot")
@Slf4j
public class WebSocketConfiguration {

    @Value("${bot.subscription.host}")
    private String host;

    @Value("${bot.subscription.protocol}")
    private String protocol;

    @Value("${bot.subscription.token}")
    private String token;

    private List<Product> products;

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    @Autowired
    private EventService eventService;

    @Bean
    @DependsOn(value = { "eventService"})
    public WebSocketClient webSocketClientBean() throws URISyntaxException, JsonProcessingException, InterruptedException {

        Map<String, Product> productsMap = products.stream()
                .collect(Collectors.toMap(product -> product.getProductId(), product -> product));

        String connectionUri = protocol + host + WEBSOCKET_URI; // for real server use WSS
        WebSocketClient mWs = new WebSocketClient( new URI( connectionUri), new Draft_6455(), getHttpHeaders(token).toSingleValueMap(), 20000 ) {
            @Override
            public void onMessage(String message) {
                log.debug("Get message : {}", message);

                // Connect Event
                boolean isConverted = OptionalConsumer.of(safeConvertRaw(message, SubscriptionEventResponse.class, log))
                        .ifPresent(event -> {
                            String type = event.getT();
                            if (type == null) return;

                            if (type.equalsIgnoreCase(WEBSOCKET_CONNECTED)) {
                                log.info("Successfully connected to webSocket and ready to listen events!");
                            } else if (event.getT().equalsIgnoreCase(WEBSOCKET_FAILED)) {
                                ErrorResponse errorResponse = fromObject(event.getBody(), ErrorResponse.class);
                                log.info("Failed to connect to webSocket : {}", errorResponse == null ? "" : errorResponse.getDeveloperMessage());
                            } else if (event.getT().equalsIgnoreCase(WEBSOCKET_QUOTE)) {
                                log.info("Received quote event  : {}", event);
                                eventService.processEvent(event, productsMap);
                            }
                        }).ifNotPresentReturn(false, true);
                if (!isConverted) {
                    log.debug("The message : {} was not converted to our entities", message);
                }
            }

            @Override
            public void onOpen( ServerHandshake handshake ) {
                log.info( "opened connection" );
            }

            @Override
            public void onClose( int code, String reason, boolean remote ) {
                log.info( "closed connection, because of {}", reason );
            }

            @Override
            public void onError( Exception ex ) {
                log.error("Error : {}" ,ex.getMessage());
            }

        };

        return mWs;
    }

    @Bean("eventService")
    public EventService getEventService() {
        return new EventServiceImpl();
    }

}
