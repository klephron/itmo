package ru.itmo.zavar.account.mqtt;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
public class MqttOutboundConfig {

    @Value("${mqtt.broker.uri}")
    private String brokerUri;
    @Value("${mqtt.topic}")
    private String topic;
    @Value("${mqtt.client_id}")
    private String clientId;

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(this.clientId, mqttClientFactory());
        handler.setAsync(true);
        handler.setDefaultTopic(this.topic);
        return handler;
    }

    @MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
    public interface MqttGateway {
        void sendToMqtt(String data);
    }

    @Bean
    public DefaultMqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{this.brokerUri});
        factory.setConnectionOptions(options);
        return factory;
    }
}
