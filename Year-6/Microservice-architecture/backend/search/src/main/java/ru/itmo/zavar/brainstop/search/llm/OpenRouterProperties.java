package ru.itmo.zavar.brainstop.search.llm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "llm.openrouter")
public class OpenRouterProperties {
    private boolean enabled = false;
    private String baseUrl = "https://openrouter.ai/api/v1";
    private String apiKey;
    private List<String> models = new ArrayList<>(List.of(
            "meta-llama/llama-3.1-8b-instruct:free",
            "mistralai/mistral-7b-instruct:free",
            "qwen/qwen2.5-7b-instruct:free"
    ));
    private Duration timeout = Duration.ofSeconds(8);
    private Double temperature = 0.2;
    private Double topP = 0.9;
    private Integer maxTokens = 256;
    private boolean streamLog = false;
    private String referer;
    private String title;
}
