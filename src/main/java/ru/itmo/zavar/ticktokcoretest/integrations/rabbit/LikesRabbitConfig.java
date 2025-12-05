package ru.itmo.zavar.ticktokcoretest.integrations.rabbit;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LikesRabbitConfig {

    @Value("${app.likes.exchange:video.likes}")
    private String likesExchangeName;

    @Bean
    public DirectExchange likesExchange() {
        return new DirectExchange(likesExchangeName, true, false);
    }
}
