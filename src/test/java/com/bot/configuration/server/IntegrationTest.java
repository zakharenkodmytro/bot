package com.bot.configuration.server;

import com.bot.model.Product;
import com.bot.model.QuoteBody;
import com.bot.model.subscription.ErrorResponse;
import com.bot.model.subscription.SubscriptionEventResponse;
import com.bot.model.trade.InvestingAmount;
import com.bot.model.trade.OrderResponse;
import com.bot.service.EventServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.mockito.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.bot.utils.SerializationUtil.toJson;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@Slf4j
public class IntegrationTest extends ApplicationTest {

    @Autowired
    private RestTemplate template;

    @Test
    public void testSuccessTrade() {
        // when
        RestTemplate restTemplate = mock(RestTemplate.class);
        OrderResponse orderResponse = OrderResponse.builder().positionId(UUID.randomUUID().toString())
                .price(InvestingAmount.builder().amount(product.getBuyPrice()).build()).build();
        ResponseEntity<OrderResponse> responseEntity = ResponseEntity.ok(orderResponse);

        // for buy order
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OrderResponse.class) )).thenReturn(responseEntity);
        // for sell order
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(OrderResponse.class), Matchers.<String>anyVararg() )).thenReturn(responseEntity);

        ReflectionTestUtils.setField(eventService, "restTemplate", restTemplate);

        // then
        // send buy price
        webServiceClient.onMessage(toJson(generateQuoteEvent(product, product.getBuyPrice())));

        // send event
        webServiceClient.onMessage(toJson(generateQuoteEvent(product, product.getBuyPrice().subtract(eventService.getBuyDelta()))));

        // send sell price
        webServiceClient.onMessage(toJson(generateQuoteEvent(product, product.getLowerLimit().add(eventService.getSellDelta()))));

        // verify
        assertEquals(0, eventService.getOrders().size());
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OrderResponse.class));
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(OrderResponse.class), Matchers.<String>anyVararg() );

        ReflectionTestUtils.setField(eventService, "restTemplate", template);
    }

    @Test
    public void testExceptionDuringBuy() {
        // when
        RestTemplate restTemplate = mock(RestTemplate.class);

        // for buy order
        doThrow(new RestClientException("Failed to connect to host")).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OrderResponse.class) );

        ReflectionTestUtils.setField(eventService, "restTemplate", restTemplate);

        // then
        // send buy price
        webServiceClient.onMessage(toJson(generateQuoteEvent(product, product.getBuyPrice())));

        // send buy price again
        webServiceClient.onMessage(toJson(generateQuoteEvent(product, product.getBuyPrice().subtract(eventService.getBuyDelta()))));

        // send buy price again
        webServiceClient.onMessage(toJson(generateQuoteEvent(product, product.getBuyPrice().subtract(eventService.getBuyDelta().divide(BigDecimal.ONE.add(BigDecimal.ONE))))));

        // verify
        assertEquals(0, eventService.getOrders().size());
        verify(restTemplate, times(3)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OrderResponse.class));
        verify(restTemplate, times(0)).exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(OrderResponse.class), Matchers.<String>anyVararg() );

        ReflectionTestUtils.setField(eventService, "restTemplate", template);
    }

    @Test
    public void testNullBuyOrderResponse() {
        // when
        RestTemplate restTemplate = mock(RestTemplate.class);
        ResponseEntity responseEntity = ResponseEntity.ok().build();

        // for buy order
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OrderResponse.class) )).thenReturn(responseEntity);

        ReflectionTestUtils.setField(eventService, "restTemplate", restTemplate);

        // then
        // send buy price
        webServiceClient.onMessage(toJson(generateQuoteEvent(product, product.getBuyPrice())));

        // send buy price again
        webServiceClient.onMessage(toJson(generateQuoteEvent(product, product.getBuyPrice().subtract(eventService.getBuyDelta()))));

        // send buy price again
        webServiceClient.onMessage(toJson(generateQuoteEvent(product, product.getBuyPrice().subtract(eventService.getBuyDelta().divide(BigDecimal.ONE.add(BigDecimal.ONE))))));

        // verify
        assertEquals(0, eventService.getOrders().size());
        verify(restTemplate, times(3)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OrderResponse.class));
        verify(restTemplate, times(0)).exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(OrderResponse.class), Matchers.<String>anyVararg() );

        ReflectionTestUtils.setField(eventService, "restTemplate", template);
    }

    @Test
    public void testNullSellOrderResponse() {
        // when
        RestTemplate restTemplate = mock(RestTemplate.class);
        OrderResponse orderResponse = OrderResponse.builder().positionId(UUID.randomUUID().toString())
                .price(InvestingAmount.builder().amount(product.getBuyPrice()).build()).build();
        ResponseEntity<OrderResponse> responseEntity = ResponseEntity.ok(orderResponse);

        ResponseEntity sell = ResponseEntity.ok().build();

        // for buy order
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OrderResponse.class) )).thenReturn(responseEntity);
        // for sell order
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(OrderResponse.class), Matchers.<String>anyVararg() )).thenReturn(sell);

        ReflectionTestUtils.setField(eventService, "restTemplate", restTemplate);

        // then
        // send buy price
        webServiceClient.onMessage(toJson(generateQuoteEvent(product, product.getBuyPrice())));

        // send sell price
        webServiceClient.onMessage(toJson(generateQuoteEvent(product, product.getLowerLimit().add(eventService.getSellDelta()))));

        // verify
        assertEquals(1, eventService.getOrders().size());
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OrderResponse.class));
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(OrderResponse.class), Matchers.<String>anyVararg() );

        // post steps
        ReflectionTestUtils.setField(eventService, "restTemplate", template);
        ReflectionTestUtils.setField(eventService, "orders", new ConcurrentHashMap<>());
    }

    @Test
    public void testWebsocketConnectFailed() {
        EventServiceImpl spy = spy(eventService);

        // then send error connection
        webServiceClient.onMessage(toJson(generateErrorConnectionEvent()));

        // verify
        assertEquals(0, eventService.getOrders().size());
        verify(spy, times(0)).processEvent(any(SubscriptionEventResponse.class), anyMap());

    }

    @Test
    public void testEventSerializationError() {
        EventServiceImpl spy = spy(eventService);

        // then send error connection
        webServiceClient.onMessage(toJson(generateWrongEvent()));

        // verify
        assertEquals(0, eventService.getOrders().size());
        verify(spy, times(0)).processEvent(any(SubscriptionEventResponse.class), anyMap());

    }

    @Test
    public void testEventTypeNull() {
        EventServiceImpl spy = spy( eventService);
        SubscriptionEventResponse eventResponse = generateQuoteEvent(product, product.getBuyPrice());
        eventResponse.setT(null);

        // send null T
        webServiceClient.onMessage(toJson(eventResponse));

        // verify
        assertEquals(0,eventService.getOrders().size());
        verify(spy, times(0)).processEvent(any(SubscriptionEventResponse.class), anyMap());
    }

    private SubscriptionEventResponse generateQuoteEvent(Product product, BigDecimal price) {
        SubscriptionEventResponse subscriptionEventResponse = new SubscriptionEventResponse();
        subscriptionEventResponse.setT("trading.quote");
        QuoteBody quoteBody = new QuoteBody();
        quoteBody.setCurrentPrice(price);
        quoteBody.setSecurityId(product.getProductId());
        quoteBody.setTimeStamp(System.currentTimeMillis());
        subscriptionEventResponse.setBody(quoteBody);

        return subscriptionEventResponse;
    }

    private String generateWrongEvent() {
        String subscriptionEventResponse = "wrong event data";

        return subscriptionEventResponse;
    }

    private SubscriptionEventResponse generateErrorConnectionEvent() {
        SubscriptionEventResponse subscriptionEventResponse = new SubscriptionEventResponse();
        subscriptionEventResponse.setT("connect.failed");
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorCode("RTF_002");
        errorResponse.setDeveloperMessage("Missing JWT Access Token in request");
        subscriptionEventResponse.setBody(errorResponse);

        return subscriptionEventResponse;
    }
}
