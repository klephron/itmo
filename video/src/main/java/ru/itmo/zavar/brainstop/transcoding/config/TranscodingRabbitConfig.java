package ru.itmo.zavar.brainstop.transcoding.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

@Configuration
@EnableRabbit
public class TranscodingRabbitConfig {
    public static final String EXCHANGE = "transcoding.exchange";
    public static final String QUEUE_PROGRESS = "transcoding.progress.queue";
    public static final String ROUTING_PROGRESS = "transcoding.progress";

    @Bean
    public DirectExchange transcodingExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue progressQueue() {
        return new Queue(QUEUE_PROGRESS, true);
    }

    @Bean
    public Binding progressBinding() {
        return BindingBuilder.bind(progressQueue()).to(transcodingExchange()).with(ROUTING_PROGRESS);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                               MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        return factory;
    }
}
