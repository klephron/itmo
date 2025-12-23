package ru.itmo.zavar.brainstop.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

@AutoConfiguration
@ConditionalOnProperty(value = "app.debug.kafka-bootstrap-origin", havingValue = "true", matchIfMissing = true)
@Slf4j
public class KafkaBootstrapOriginLoggerAutoConfiguration {

    @Bean
    public ApplicationRunner kafkaBootstrapOriginLogger(ConfigurableEnvironment environment) {
        return args -> {
            String key = "spring.kafka.bootstrap-servers";
            String value = environment.getProperty(key);
            if (value == null) {
                log.info("Kafka bootstrap servers: <not set>");
                return;
            }

            String sourceName = null;
            String originText = null;

            for (PropertySource<?> source : environment.getPropertySources()) {
                Object raw = source.getProperty(key);
                if (raw == null) {
                    continue;
                }
                sourceName = source.getName();
                if (source instanceof OriginLookup<?>) {
                    Origin origin = OriginLookup.getOrigin(source, key);
                    if (origin != null) {
                        originText = origin.toString();
                    }
                }
                break;
            }

            if (sourceName == null) {
                log.info("Kafka bootstrap servers: {} (source: <unknown>)", value);
                return;
            }
            if (originText == null) {
                log.info("Kafka bootstrap servers: {} (source: {})", value, sourceName);
                return;
            }
            log.info("Kafka bootstrap servers: {} (source: {}, origin: {})", value, sourceName, originText);
        };
    }
}
