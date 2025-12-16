package ru.itmo.zavar.brainstop.gateway;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "ru.itmo.zavar.brainstop.user")
@EntityScan(basePackages = "ru.itmo.zavar.brainstop.user")
public class JpaConfig {
}
