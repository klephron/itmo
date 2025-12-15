package ru.itmo.zavar.ticktokcoretest.transcoding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import ru.itmo.zavar.ticktokcoretest.common.security.JwtAuthConverter;

@SpringBootApplication
@EnableMethodSecurity
@Import(JwtAuthConverter.class)
public class TranscodingApplication {
    public static void main(String[] args) {
        SpringApplication.run(TranscodingApplication.class, args);
    }
}
