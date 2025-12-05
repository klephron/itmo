package ru.itmo.zavar.ticktokcoretest.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;
import ru.itmo.zavar.ticktokcoretest.security.Role;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@roleEvaluator.hasRole(authentication, #required)")
public @interface RequireRole {
    Role required();
}
