package com.bot.utils;

import org.springframework.http.HttpHeaders;

import static java.lang.String.format;

public class Utils {
    public static String BEARER_TOKEN_TEMPLATE = "Bearer %s";

    public static HttpHeaders getHttpHeaders(String accessToken) {
        HttpHeaders httpHeader = new HttpHeaders();
        httpHeader.add(HttpHeaders.AUTHORIZATION, format(BEARER_TOKEN_TEMPLATE, accessToken));
        httpHeader.add(HttpHeaders.CONTENT_TYPE, "application/json");
        httpHeader.add(HttpHeaders.ACCEPT_LANGUAGE, "nl-NL,en;q=0.8");
        httpHeader.add(HttpHeaders.ACCEPT, "application/json");

        return httpHeader;
    }
}
