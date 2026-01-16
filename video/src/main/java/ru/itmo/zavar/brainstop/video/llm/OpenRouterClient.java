package ru.itmo.zavar.brainstop.video.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenRouterClient {

    private final OpenRouterProperties properties;
    private final ObjectMapper objectMapper;
    private RestClient restClient;

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        Duration timeout = properties.getTimeout();
        int timeoutMs = timeout == null ? 8000 : Math.toIntExact(Math.min(timeout.toMillis(), Integer.MAX_VALUE));
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Accept", "application/json");

        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + properties.getApiKey());
        }
        if (properties.getReferer() != null && !properties.getReferer().isBlank()) {
            builder.defaultHeader("HTTP-Referer", properties.getReferer());
        }
        if (properties.getTitle() != null && !properties.getTitle().isBlank()) {
            builder.defaultHeader("X-Title", properties.getTitle());
        }
        restClient = builder.build();
    }

    public Optional<String> complete(List<OpenRouterMessage> messages, Integer maxTokens, String requestTag) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            log.warn("OpenRouter enabled but API key is missing");
            return Optional.empty();
        }
        if (messages == null || messages.isEmpty()) {
            return Optional.empty();
        }
        List<String> models = properties.getModels();
        if (models == null || models.isEmpty()) {
            return Optional.empty();
        }
        Integer tokenLimit = maxTokens != null ? maxTokens : properties.getMaxTokens();
        for (String model : models) {
            if (model == null || model.isBlank()) {
                continue;
            }
            try {
                if (log.isDebugEnabled()) {
                    log.debug("OpenRouter {} request model={} messages={}", requestTag, model, messages);
                }
                boolean streamLog = properties.isStreamLog();
                OpenRouterRequest request = new OpenRouterRequest(
                        model,
                        messages,
                        properties.getTemperature(),
                        properties.getTopP(),
                        tokenLimit,
                        streamLog
                );
                String content = streamLog
                        ? postJsonStream(request, requestTag, model)
                        : extractContent(parseResponse(postJson(request)));
                if (content != null && !content.isBlank()) {
                    log.debug("OpenRouter {} response model={} content={}", requestTag, model, content);
                    return Optional.of(content.trim());
                }
            } catch (Exception e) {
                log.warn("OpenRouter {} request failed for model {}: {}", requestTag, model, e.getMessage());
            }
        }
        return Optional.empty();
    }

    private String postJson(OpenRouterRequest request) {
        return restClient.post()
                .uri("/chat/completions")
                .body(request)
                .exchange((req, res) -> {
                    try (InputStream body = res.getBody()) {
                        if (body == null) {
                            return "";
                        }
                        return new String(body.readAllBytes(), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new IllegalStateException("Failed to read LLM response", e);
                    }
                });
    }

    private String postJsonStream(OpenRouterRequest request, String requestTag, String model) {
        return restClient.post()
                .uri("/chat/completions")
                .body(request)
                .exchange((req, res) -> {
                    StringBuilder full = new StringBuilder();
                    try (InputStream body = res.getBody();
                         BufferedReader reader = body == null
                                 ? null
                                 : new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
                        if (reader == null) {
                            return "";
                        }
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String trimmed = line.trim();
                            if (trimmed.isEmpty()) {
                                continue;
                            }
                            if (trimmed.startsWith("data:")) {
                                trimmed = trimmed.substring(5).trim();
                            }
                            if ("[DONE]".equals(trimmed)) {
                                break;
                            }
                            String delta = extractStreamDelta(trimmed);
                            if (delta != null && !delta.isBlank()) {
                                full.append(delta);
                                log.info("OpenRouter {} stream model={} chunk={}", requestTag, model, delta);
                            }
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to read LLM response", e);
                    }
                    return full.toString();
                });
    }

    private String extractStreamDelta(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return "";
            }
            JsonNode choice = choices.get(0);
            JsonNode delta = choice.path("delta");
            if (delta.isObject()) {
                return delta.path("content").asText("");
            }
            JsonNode message = choice.path("message");
            if (message.isObject()) {
                return message.path("content").asText("");
            }
            return choice.path("text").asText("");
        } catch (Exception e) {
            String snippet = json.length() > 200 ? json.substring(0, 200) + "..." : json;
            log.debug("OpenRouter stream parse failed: {}", snippet);
            return "";
        }
    }

    private OpenRouterResponse parseResponse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, OpenRouterResponse.class);
        } catch (Exception e) {
            String snippet = raw.length() > 500 ? raw.substring(0, 500) + "..." : raw;
            log.warn("OpenRouter response parse failed: {}", snippet);
            return null;
        }
    }

    private String extractContent(OpenRouterResponse response) {
        if (response == null) {
            return null;
        }
        if (response.error() != null && response.error().message() != null) {
            log.warn("OpenRouter error: {}", response.error().message());
        }
        if (response.choices() == null || response.choices().isEmpty()) {
            return null;
        }
        OpenRouterChoice choice = response.choices().get(0);
        if (choice == null) {
            return null;
        }
        if (choice.message() != null) {
            return choice.message().content();
        }
        return choice.text();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record OpenRouterRequest(String model,
                                     List<OpenRouterMessage> messages,
                                     Double temperature,
                                     Double top_p,
                                     Integer max_tokens,
                                     Boolean stream) {
    }

    private record OpenRouterResponse(List<OpenRouterChoice> choices, OpenRouterError error) {
    }

    private record OpenRouterChoice(OpenRouterMessage message, String text) {
    }

    private record OpenRouterError(String message, String type, Object code) {
    }

    public record OpenRouterMessage(String role, String content) {
    }
}
