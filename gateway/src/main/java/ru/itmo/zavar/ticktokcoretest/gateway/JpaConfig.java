package ru.itmo.zavar.ticktokcoretest.gateway;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "ru.itmo.zavar.ticktokcoretest.user")
@EntityScan(basePackages = "ru.itmo.zavar.ticktokcoretest.user")
public class JpaConfig {
}
