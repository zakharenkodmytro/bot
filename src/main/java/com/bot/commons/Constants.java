package com.bot.commons;

public class Constants {

    public static final String HTTPS = "https://";
    public static final String HTTP = "http://";
    public static final String WSS = "wss://";
    public static final String WEBSOCKET_URI = "/subscriptions/me";
    public static final String TRADE_URI ="/core/16/users/me/trades";
    public static final String CLOSE_URI ="/core/16/users/me/portfolio/positions/{positionId}";

    public static final String WEBSOCKET_CONNECTED = "connect.connected";
    public static final String WEBSOCKET_FAILED    = "connect.failed";
    public static final String WEBSOCKET_QUOTE     = "trading.quote";

    public static final String DIRECTION_BUY = "BUY";

    public static final String CURRENCY = "BUX";

    public static final int TIMEOUT = 200;

}
