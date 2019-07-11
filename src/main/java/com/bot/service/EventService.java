package com.bot.service;

import com.bot.model.Product;

import java.util.Map;

public interface EventService<T> {

    void processEvent(T event, Map<String, Product> productMap);
}
