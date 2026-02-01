package ru.itmopai.backend.annotation.permission;

import io.micronaut.aop.Around;
import io.micronaut.context.annotation.Type;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Around
@Type(RequiresPermissionsInterceptor.class)
public @interface RequiresPermissions {
  String[] value();
}
