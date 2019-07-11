package com.bot.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.core.serializer.support.SerializationFailedException;

import java.io.IOException;
import java.util.Map;

public class SerializationUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public SerializationUtil() {
    }

    public static String toJson(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (IOException e) {
            throw new SerializationFailedException("Error during serialization", e);
        }
    }

    public static <T> T fromJson(String data, Class<T> tClass) {
        try {
            return OBJECT_MAPPER.readValue(data, tClass);
        } catch (IOException e) {
            throw new SerializationFailedException("Error during serialization", e);
        }
    }

    public static <T,R> T fromObject(R data, Class<T> tClass) {
        return OBJECT_MAPPER.convertValue(data, tClass);
    }
}
