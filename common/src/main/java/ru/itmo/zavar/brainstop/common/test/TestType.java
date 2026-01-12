package ru.itmo.zavar.brainstop.common.test;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag("type")
public @interface TestType {
    Type value();

    enum Type {
        BASIC,
        ALT,
        NEGATIVE
    }
}