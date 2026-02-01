package ru.itmo.zavar.brainstop.transcoding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import ru.itmo.zavar.brainstop.common.security.JwtAuthConverter;
import ru.itmo.zavar.brainstop.common.infra.StorageClientConfiguration;

@SpringBootApplication(scanBasePackages = "ru.itmo.zavar.brainstop")
@EnableMethodSecurity
@Import({JwtAuthConverter.class, StorageClientConfiguration.class})
public class TranscodingApplication {
    public static void main(String[] args) {
        SpringApplication.run(TranscodingApplication.class, args);
    }
}
