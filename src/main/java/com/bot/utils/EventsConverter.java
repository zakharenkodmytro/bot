package com.bot.utils;

import org.slf4j.Logger;
import org.springframework.core.serializer.support.SerializationFailedException;

import java.util.Optional;

import static com.bot.utils.SerializationUtil.fromJson;

public class EventsConverter {

    public static <T> Optional<T> safeConvertRaw(String payload, Class<T> desiredType, Logger logger) {
        try {
            return Optional.ofNullable(fromJson(payload, desiredType));
        } catch (SerializationFailedException e) {
            logger.error(e.getMessage());
            return Optional.empty();
        }
    }
}
