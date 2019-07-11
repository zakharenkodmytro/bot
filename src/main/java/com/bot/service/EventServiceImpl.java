package com.bot.service;

import com.bot.BotApplication;
import com.bot.model.Product;
import com.bot.model.QuoteBody;
import com.bot.model.subscription.SubscriptionEventResponse;
import com.bot.model.subscription.SubscriptionRequest;
import com.bot.model.trade.BuyOrder;
import com.bot.model.trade.OrderResponse;
import com.bot.model.trade.InvestingAmount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.bot.commons.Constants.*;
import static com.bot.utils.SerializationUtil.fromObject;
import static com.bot.utils.SerializationUtil.toJson;
import static com.bot.utils.Utils.getHttpHeaders;
import static java.lang.String.format;

@Slf4j
public class EventServiceImpl implements EventService<SubscriptionEventResponse> {

    @Value("${bot.trade.host}")
    private String host;

    @Value("${bot.trade.protocol}")
    private String protocol;

    @Value("${bot.trade.token}")
    private String token;

    @Value("${bot.trade.buy-delta}")
    private BigDecimal buyDelta;

    @Value("${bot.trade.sell-delta}")
    private BigDecimal sellDelta;

    public BigDecimal getBuyDelta() {
        return buyDelta;
    }

    public BigDecimal getSellDelta() {
        return sellDelta;
    }

    @Autowired
    private RestTemplate restTemplate;

    public Map<String, OrderResponse> getOrders() {
        return new ConcurrentHashMap<>(orders);
    }

    private Map<String, OrderResponse> orders = new ConcurrentHashMap<>();

    @Override
    public void processEvent(SubscriptionEventResponse event, Map<String, Product> productMap) {
        QuoteBody body = fromObject(event.getBody(), QuoteBody.class);
        String id = body.getSecurityId();
        Product product = productMap.get(id);

        // here we need to define buy threshold
        BigDecimal buyLowerThreshold = product.getBuyPrice().subtract(buyDelta);
        BigDecimal buyUpperThreshold = product.getBuyPrice().add(buyDelta);

        if (!orders.containsKey(product.getProductId()) && body.getCurrentPrice().compareTo(buyLowerThreshold) >= 0
                && body.getCurrentPrice().compareTo(buyUpperThreshold) <= 0) {

            // Execute buy order
            executeBuyOrder(product);
            return;
        }

        OrderResponse order = orders.get(product.getProductId());
        if (order == null) {
            return;
        }

        // here we need to define sell threshold
        BigDecimal sellLowerThreshold = product.getLowerLimit().add(sellDelta);
        BigDecimal sellUpperThreshold = product.getUpperLimit().subtract(sellDelta);

        boolean isBelowLowerThreshold = body.getCurrentPrice().compareTo(sellLowerThreshold) <= 0 && body.getCurrentPrice().compareTo(product.getLowerLimit()) >= 0;
        boolean isAboveUpperThreshold = body.getCurrentPrice().compareTo(sellUpperThreshold) >= 0 && body.getCurrentPrice().compareTo(product.getUpperLimit()) <= 0;

        if (isBelowLowerThreshold || isAboveUpperThreshold) {
            executeSellOrderAndUnsubscribe(product, order);
        }
    }

    private void executeSellOrderAndUnsubscribe(Product product, OrderResponse order) {
        log.info("Close the position with id {}", order.getPositionId());

        // Delete position
        ResponseEntity<OrderResponse> responseEntity;
        String connectionUri = protocol + host + CLOSE_URI; // connect to real server use HTTPS
        try {
            log.info("Executing sel position request");
            responseEntity = executeRequestWithParams(connectionUri, HttpMethod.DELETE, null, OrderResponse.class, token, order.getPositionId());
            if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
                // TODO handle error code scenarios
                return;
            }
            order = responseEntity.getBody();
        } catch (Exception e) {
            log.warn("Failed to to execute sell order : {}", e.getMessage());
            return;
        }

        if (order == null) {
            return;
        }

        log.info("Successfully executed sell order for position with id : {} ", order.getPositionId());
        // remove product from cache
        orders.remove(product.getProductId());

        // unsubscribe
        unsubscribe(product);
    }

    private void unsubscribe(Product product) {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setUnsubscribeFrom(Collections.singletonList(format("trading.product.%s", product.getProductId())));
        String message = toJson(subscriptionRequest);

        log.info("Unsubscribe from product with id {}", product.getProductId());
        BotApplication.getWebSocketClient().send(message);
    }

    private void executeBuyOrder(Product product) {
        InvestingAmount investingAmount = InvestingAmount.builder().amount(product.getBuyPrice())
                .currency(CURRENCY)
                .decimals(2).build();
        BuyOrder buyOrder = BuyOrder.builder().direction(DIRECTION_BUY)
                .leverage(2)
                .productId(product.getProductId())
                .investingAmount(investingAmount).build();

        log.info("Executing buy position request with price : {}", product.getBuyPrice());
        String connectionUri = protocol + host + TRADE_URI; // connect to real server use HTTPS
        OrderResponse response;
        ResponseEntity<OrderResponse> responseEntity;
        try {
            responseEntity = executeRequest(connectionUri, HttpMethod.POST, buyOrder, OrderResponse.class, token);
            if (responseEntity == null) {
                return;
            }
            if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
                // TODO handle error code scenarios
                return;
            }
            response = responseEntity.getBody();
        } catch (Exception e) {
            log.warn("Failed to to execute buy order : {}", e.getMessage());
            return;
        }

        if (response == null) {
            return;
        }

        log.info("Successfully executed buy order for position with id : {} ", response.getPositionId());
        // put in cache product that we are already bought
        orders.put(product.getProductId(), response);
    }

    private <T, R> ResponseEntity<R> executeRequest(String url, HttpMethod method, T payload, Class<R> returnType, String accessToken) {
        return restTemplate.exchange(url, method, new HttpEntity<>(payload, getHttpHeaders(accessToken)), returnType);
    }

    private <T, R> ResponseEntity<R> executeRequestWithParams(String url, HttpMethod method, T payload, Class<R> returnType, String accessToken, Object... uriVars) {
        return restTemplate.exchange(url, method, new HttpEntity<>(payload, getHttpHeaders(accessToken)), returnType, uriVars);
    }
}
