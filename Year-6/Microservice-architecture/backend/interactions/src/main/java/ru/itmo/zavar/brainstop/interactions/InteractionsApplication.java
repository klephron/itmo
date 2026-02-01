package ru.itmo.zavar.brainstop.interactions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import ru.itmo.zavar.brainstop.common.security.JwtAuthConverter;

@SpringBootApplication(scanBasePackages = "ru.itmo.zavar.brainstop")
@EntityScan(basePackages = "ru.itmo.zavar.brainstop")
@EnableJpaRepositories(basePackages = "ru.itmo.zavar.brainstop")
@EnableFeignClients(basePackages = "ru.itmo.zavar.brainstop")
@Import(JwtAuthConverter.class)
public class InteractionsApplication {
    public static void main(String[] args) {
        SpringApplication.run(InteractionsApplication.class, args);
    }
}
