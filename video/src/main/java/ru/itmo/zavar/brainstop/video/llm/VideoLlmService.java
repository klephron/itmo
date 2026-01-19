package ru.itmo.zavar.brainstop.video.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoLlmService {

    private static final Pattern JSON_ARRAY = Pattern.compile("\\[[\\s\\S]*\\]");
    private static final Pattern JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*\\}");

    private final OpenRouterClient openRouterClient;
    private final VideoLlmProperties properties;
    private final ObjectMapper objectMapper;
    private LlmCache<List<String>> tagCache;
    private LlmCache<ModerationResult> moderationCache;

    @PostConstruct
    public void init() {
        tagCache = new LlmCache<>(properties.getCacheTtl());
        moderationCache = new LlmCache<>(properties.getCacheTtl());
    }

    public VideoLlmResult enrich(String title, String description) {
        if (!properties.isEnabled()) {
            return VideoLlmResult.empty();
        }
        List<String> tags = properties.isTaggingEnabled()
                ? generateTags(title, description)
                : List.of();
        ModerationResult moderation = properties.isModerationEnabled()
                ? moderate(title, description)
                : ModerationResult.empty();
        VideoLlmResult result = new VideoLlmResult(tags, moderation.flagged(), moderation.categories());
        log.debug("LLM enrichment tags={} flagged={} categories={}", result.tags(), result.flagged(), result.categories());
        return result;
    }

    private List<String> generateTags(String title, String description) {
        String key = "tags:" + normalizeKey(title, description);
        Optional<List<String>> cached = tagCache.get(key);
        if (cached.isPresent()) {
            return cached.get();
        }
        int tagCount = Math.max(properties.getTagCount(), 1);
        String prompt = "Return a JSON array with " + tagCount + " short lowercase tags (1-3 words each). " +
                "Use only relevant tags. JSON only.";
        List<OpenRouterClient.OpenRouterMessage> messages = List.of(
                new OpenRouterClient.OpenRouterMessage("system", prompt),
                new OpenRouterClient.OpenRouterMessage("user", buildInput(title, description))
        );
        Optional<String> result = openRouterClient.complete(messages, properties.getTagMaxTokens(), "tags");
        List<String> parsed = parseJsonArray(result.orElse(""));
        List<String> cleaned = sanitizeTags(parsed, tagCount);
        tagCache.put(key, cleaned);
        return cleaned;
    }

    private ModerationResult moderate(String title, String description) {
        String key = "moderation:" + normalizeKey(title, description);
        Optional<ModerationResult> cached = moderationCache.get(key);
        if (cached.isPresent()) {
            return cached.get();
        }
        String prompt = "Return a JSON object: {\"flagged\": boolean, \"categories\": [string]}. " +
                "Categories: sexual, violence, hate, self-harm, drugs, other. " +
                "If unsure, flagged=false and categories empty. JSON only.";
        List<OpenRouterClient.OpenRouterMessage> messages = List.of(
                new OpenRouterClient.OpenRouterMessage("system", prompt),
                new OpenRouterClient.OpenRouterMessage("user", buildInput(title, description))
        );
        Optional<String> result = openRouterClient.complete(messages, properties.getModerationMaxTokens(), "moderation");
        ModerationResult parsed = parseModeration(result.orElse(""));
        moderationCache.put(key, parsed);
        return parsed;
    }

    private String buildInput(String title, String description) {
        String safeTitle = title == null ? "" : title;
        String safeDesc = description == null ? "" : description;
        return "Title: " + safeTitle + "\nDescription: " + safeDesc;
    }

    private List<String> parseJsonArray(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String candidate = raw.trim();
        try {
            return objectMapper.readValue(candidate, new TypeReference<List<String>>() {
            });
        } catch (Exception ignored) {
        }
        Matcher matcher = JSON_ARRAY.matcher(candidate);
        if (matcher.find()) {
            try {
                return objectMapper.readValue(matcher.group(), new TypeReference<List<String>>() {
                });
            } catch (Exception ignored) {
            }
        }
        return splitFallback(candidate);
    }

    private ModerationResult parseModeration(String raw) {
        if (raw == null || raw.isBlank()) {
            return ModerationResult.empty();
        }
        String candidate = raw.trim();
        try {
            return objectMapper.readValue(candidate, ModerationResult.class);
        } catch (Exception ignored) {
        }
        Matcher matcher = JSON_OBJECT.matcher(candidate);
        if (matcher.find()) {
            try {
                return objectMapper.readValue(matcher.group(), ModerationResult.class);
            } catch (Exception ignored) {
            }
        }
        return ModerationResult.empty();
    }

    private List<String> splitFallback(String raw) {
        List<String> list = new ArrayList<>();
        for (String part : raw.split("[\\n,;]+")) {
            String value = sanitizeTag(part);
            if (!value.isBlank()) {
                list.add(value);
            }
        }
        return list;
    }

    private List<String> sanitizeTags(List<String> tags, int limit) {
        Set<String> cleaned = new LinkedHashSet<>();
        if (tags == null) {
            return List.of();
        }
        for (String tag : tags) {
            String value = sanitizeTag(tag);
            if (!value.isBlank()) {
                cleaned.add(value);
                if (cleaned.size() >= limit) {
                    break;
                }
            }
        }
        return new ArrayList<>(cleaned);
    }

    private String sanitizeTag(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
                (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        trimmed = trimmed.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s_-]+", " ");
        trimmed = trimmed.replaceAll("\\s+", " ").trim();
        if (trimmed.isBlank()) {
            return "";
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private String normalizeKey(String title, String description) {
        return (title == null ? "" : title.trim().toLowerCase(Locale.ROOT)) + "|" +
                (description == null ? "" : description.trim().toLowerCase(Locale.ROOT));
    }

    private record ModerationResult(boolean flagged, List<String> categories) {
        static ModerationResult empty() {
            return new ModerationResult(false, List.of());
        }
    }
}
