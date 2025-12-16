package ru.itmo.zavar.brainstop.interactions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import ru.itmo.zavar.brainstop.common.security.JwtAuthConverter;

@SpringBootApplication
@EnableFeignClients
@Import(JwtAuthConverter.class)
public class InteractionsApplication {
    public static void main(String[] args) {
        SpringApplication.run(InteractionsApplication.class, args);
    }
}
