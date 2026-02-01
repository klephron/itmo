package ru.itmo.zavar.brainstop.common.infra;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import ru.itmo.zavar.brainstop.common.api.infra.VideoObjectStorage;

@Configuration
public class StorageClientConfiguration {

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1))
                .build();
        return WebClient.builder()
                .exchangeStrategies(strategies);
    }

    @Bean
    @ConditionalOnMissingBean(VideoObjectStorage.class)
    public VideoObjectStorage remoteStorageClient(@Value("${storage.client.base-url:http://storage-service}") String baseUrl,
                                                  @Qualifier("loadBalancedWebClientBuilder") WebClient.Builder builder) {
        return new RemoteVideoObjectStorage(builder.baseUrl(baseUrl).build());
    }
}
