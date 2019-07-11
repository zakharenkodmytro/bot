package com.bot.utils;


import java.util.Optional;
import java.util.function.Consumer;

public class OptionalConsumer<T> {
    private Optional<T> optional;

    private OptionalConsumer(Optional<T> optional) {
        this.optional = optional;
    }

    public static <T> OptionalConsumer<T> of(Optional<T> optional) {
        return new OptionalConsumer<>(optional);
    }

    public OptionalConsumer<T> ifPresent(Consumer<T> c) {
        optional.ifPresent(c);
        return this;
    }

    public <R> R ifNotPresentReturn(R other, R ok) {
        if (optional.isPresent()) {
            return ok;
        }
        return other;
    }
}