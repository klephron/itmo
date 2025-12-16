package ru.itmo.zavar.brainstop.common.rabbit;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitExchangesConfig {

    @Value("${app.likes.exchange:video.likes}")
    private String likesExchangeName;

    @Value("${app.views.exchange:video.views}")
    private String viewsExchangeName;

    @Bean
    public DirectExchange likesExchange() {
        return new DirectExchange(likesExchangeName);
    }

    @Bean
    public DirectExchange viewsExchange() {
        return new DirectExchange(viewsExchangeName);
    }
}
