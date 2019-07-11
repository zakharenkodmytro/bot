package com.bot.configuration.server.mock;

import lombok.extern.slf4j.Slf4j;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

import static com.bot.commons.Constants.WEBSOCKET_URI;

@ClientEndpoint
@Slf4j
@ServerEndpoint(value = WEBSOCKET_URI)
public class QuoteWebSocket {
    @OnOpen
    public void onWebSocketConnect(Session sess) {
        log.info("Socket Connected: " + sess);
    }

    @OnMessage
    public void onWebSocketText(String message) {
        log.info("Received TEXT message: " + message);
    }

    @OnClose
    public void onWebSocketClose(CloseReason reason) {
        log.info("Socket Closed: " + reason);
    }

    @OnError
    public void onWebSocketError(Throwable cause) {
        log.error(cause.getMessage());
    }
}