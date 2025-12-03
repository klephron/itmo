package ru.itmo.zavar.ticktokcoretest.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@roleEvaluator.hasRole(authentication, #required)")
public @interface RequireRole {
    Role required();
}
