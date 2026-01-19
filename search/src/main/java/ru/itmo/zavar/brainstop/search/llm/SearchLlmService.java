package ru.itmo.zavar.brainstop.search.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.video.dto.FileMetadataDto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchLlmService {

    private static final Pattern JSON_ARRAY = Pattern.compile("\\[[\\s\\S]*\\]");
    private static final Pattern UUID_PATTERN =
            Pattern.compile("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final List<String> EN_FALLBACK_SUFFIXES = List.of(
            "video",
            "clip",
            "movie",
            "film",
            "footage",
            "demo",
            "preview",
            "trailer"
    );
    private static final List<String> RU_FALLBACK_SUFFIXES = List.of(
        "\u0432\u0438\u0434\u0435\u043e",
        "\u0440\u043e\u043b\u0438\u043a",
        "\u043a\u043b\u0438\u043f",
        "\u0444\u0438\u043b\u044c\u043c",
        "\u043a\u0430\u0434\u0440\u044b",
        "\u0434\u0435\u043c\u043e",
        "\u043f\u0440\u0435\u0432\u044c\u044e",
        "\u0442\u0440\u0435\u0439\u043b\u0435\u0440"
    );
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "or", "for", "with", "from", "into", "to", "in", "on", "of", "a", "an", "by", "is", "are",
            "be", "this", "that", "these", "those", "about", "near", "how", "what", "why", "which", "when", "where",
            "who", "whom", "was", "were", "been", "at", "as",
            "\u0438", "\u0432", "\u0432\u043e", "\u043d\u0430", "\u043f\u043e", "\u043e", "\u043e\u0442",
            "\u043a", "\u0441", "\u0441\u043e", "\u0434\u043b\u044f", "\u0438\u0437", "\u043f\u0440\u043e",
            "\u043e\u0431", "\u0431\u0435\u0437", "\u0434\u043e"
    );

    private final OpenRouterClient openRouterClient;
    private final SearchLlmProperties properties;
    private final ObjectMapper objectMapper;
    private LlmCache<String> rewriteCache;
    private LlmCache<List<String>> expansionCache;
    private LlmCache<List<String>> filterCache;
    private LlmCache<String> translationCache;

    @PostConstruct
    public void init() {
        rewriteCache = new LlmCache<>(properties.getCacheTtl());
        expansionCache = new LlmCache<>(properties.getCacheTtl());
        filterCache = new LlmCache<>(properties.getCacheTtl());
        translationCache = new LlmCache<>(properties.getCacheTtl());
    }

    public List<String> buildQueryVariants(String rawQuery) {
        return buildQueryVariants(rawQuery, null);
    }

    public List<String> buildQueryVariants(String rawQuery, String ragContext) {
        String original = safeTrim(rawQuery);
        if (original.isBlank()) {
            return List.of(original);
        }
        if (!shouldUseLlmVariants(original)) {
            return List.of(original);
        }
        if (original.length() < properties.getMinQueryLength()) {
            return List.of(original);
        }
        String context = prepareContext(ragContext);
        String rewritten = rewriteQuery(original, context);
        String base = rewritten.isBlank() ? original : rewritten;

        LinkedHashMap<String, String> deduped = new LinkedHashMap<>();
        putIfPresent(deduped, base);
        if (!original.equalsIgnoreCase(base)) {
            putIfPresent(deduped, original);
        }
        if (properties.isEnabled() && properties.isExpansionEnabled()) {
            List<String> expansions = expandQueries(base, context);
            for (String expansion : expansions) {
                putIfPresent(deduped, expansion);
            }
        }
        List<String> variants = new ArrayList<>(deduped.values());
        log.debug("LLM variants for '{}': {}", original, variants);
        return variants;
    }

    public List<String> buildQueryVariants(String rawQuery,
                                           String ragContext,
                                           List<FileMetadataDto> corpusCandidates) {
        List<String> variants = buildQueryVariants(rawQuery, ragContext);
        if (corpusCandidates == null || corpusCandidates.isEmpty()) {
            return variants;
        }
        int limit = expansionTargetCount();
        if (limit == 0) {
            return variants;
        }
        LinkedHashMap<String, String> deduped = new LinkedHashMap<>();
        for (String variant : variants) {
            putIfPresent(deduped, variant);
        }
        addCorpusExpansions(deduped, rawQuery, corpusCandidates, limit);
        return new ArrayList<>(deduped.values());
    }

    public String buildCandidateContext(List<FileMetadataDto> candidates) {
        return buildFilterContext(candidates);
    }

    public boolean isRagEnabled() {
        return properties.isRagEnabled();
    }

    public boolean isLlmEnabled() {
        return properties.isEnabled();
    }

    public boolean isRewriteEnabled() {
        return properties.isEnabled() && properties.isRewriteEnabled();
    }

    public boolean isExpansionEnabled() {
        return properties.isEnabled() && properties.isExpansionEnabled();
    }

    public boolean isFilterEnabled() {
        return properties.isEnabled() && properties.isFilterEnabled();
    }

    public boolean shouldUseLlmVariants(String query) {
        if (!isLlmEnabled()) {
            return false;
        }
        String trimmed = safeTrim(query);
        if (trimmed.isBlank()) {
            return false;
        }
        if (trimmed.length() < properties.getMinQueryLength()) {
            return false;
        }
        if (isSimpleQuery(trimmed)) {
            return properties.isExpansionEnabled();
        }
        return true;
    }

    public int minQueryLength() {
        return Math.max(properties.getMinQueryLength(), 0);
    }

    public int rewriteMaxTokens() {
        return properties.getRewriteMaxTokens();
    }

    public int expansionMaxTokens() {
        return properties.getExpansionMaxTokens();
    }

    public int filterMaxTokens() {
        return properties.getFilterMaxTokens();
    }

    public int ragLimit() {
        return Math.max(properties.getRagMaxItems(), 0);
    }

    public List<FileMetadataDto> filterCandidates(String rawQuery, List<FileMetadataDto> candidates) {
        return filterCandidates(rawQuery, null, candidates, null);
    }

    public List<FileMetadataDto> filterCandidates(String rawQuery,
                                                  List<String> variants,
                                                  List<FileMetadataDto> candidates,
                                                  List<FileMetadataDto> contextCandidates) {
        if (!isFilterEnabled()) {
            return candidates == null ? List.of() : candidates;
        }
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        String query = safeTrim(rawQuery);
        if (query.isBlank()) {
            return candidates;
        }
        List<FileMetadataDto> directMatches = new ArrayList<>();
        if (isSimpleQuery(query)) {
            directMatches.addAll(filterByShortQuery(query, candidates));
        }
        List<String> queriesForMatch = prepareVariantQueries(query, variants);
        directMatches.addAll(filterByTextMatchAny(queriesForMatch, candidates));
        boolean hasDirectMatches = !directMatches.isEmpty();
        String context = buildFilterContext(contextCandidates == null ? candidates : contextCandidates);
        if (context.isBlank()) {
            return hasDirectMatches ? dedupeCandidates(mergeCandidates(directMatches, candidates)) : candidates;
        }
        String key = "filter:" + normalize(query) + ":" + contextKey(context);
        Optional<List<String>> cached = filterCache.get(key);
        List<String> ids = cached.orElse(null);
        if (ids == null) {
            String prompt = "Given the search query and candidate videos, return a JSON array of UUID strings that are relevant. " +
                    "Only use UUID values from the list. Do not include 'uuid=' prefixes. Return [] if none. JSON only.";
            String variantsBlock = buildVariantBlock(queriesForMatch, query);
            if (!variantsBlock.isBlank()) {
                prompt = prompt + "\nQuery variants:\n" + variantsBlock;
            }
            List<OpenRouterClient.OpenRouterMessage> messages = List.of(
                    new OpenRouterClient.OpenRouterMessage("system", prompt + "\nCandidates:\n" + context),
                    new OpenRouterClient.OpenRouterMessage("user", query)
            );
            Optional<String> result = openRouterClient.complete(messages, properties.getFilterMaxTokens(), "filter");
            String raw = result.orElse("").trim();
            if (log.isDebugEnabled()) {
                log.debug("LLM filter raw response for '{}': {}", query, raw);
            }
            List<String> parsed = parseJsonArray(raw);
            List<String> extracted = extractUuids(parsed, raw);
            boolean explicitEmpty = raw.replaceAll("\\s+", "").equals("[]");
            if (extracted.isEmpty() && !explicitEmpty) {
                return hasDirectMatches ? dedupeCandidates(mergeCandidates(directMatches, candidates)) : candidates;
            }
            ids = extracted;
            filterCache.put(key, ids);
        }
        if (ids.isEmpty()) {
            return hasDirectMatches ? dedupeCandidates(directMatches) : List.of();
        }
        Set<String> allowed = new HashSet<>();
        for (String id : ids) {
            String normalized = normalize(id);
            if (!normalized.isBlank()) {
                allowed.add(normalized);
            }
        }
        if (allowed.isEmpty()) {
            return hasDirectMatches ? dedupeCandidates(mergeCandidates(directMatches, candidates)) : candidates;
        }
        List<FileMetadataDto> filtered = new ArrayList<>();
        for (FileMetadataDto item : candidates) {
            String candidateKey = candidateKey(item);
            if (candidateKey != null && allowed.contains(candidateKey)) {
                filtered.add(item);
            }
        }
        if (hasDirectMatches) {
            return dedupeCandidates(mergeCandidates(directMatches, filtered));
        }
        return filtered;
    }

    public String buildRagContext(List<FileMetadataDto> candidates) {
        if (!properties.isRagEnabled()) {
            return "";
        }
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }
        int maxItems = Math.max(properties.getRagMaxItems(), 0);
        if (maxItems == 0) {
            return "";
        }
        int maxChars = Math.max(properties.getRagMaxChars(), 0);
        int maxField = Math.max(properties.getRagMaxFieldLength(), 0);

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (FileMetadataDto item : candidates) {
            if (count >= maxItems) {
                break;
            }
            if (item == null) {
                continue;
            }
            String uuid = item.uuid() == null ? "" : item.uuid().toString();
            String name = cleanText(item.name(), maxField);
            String desc = cleanText(item.description(), maxField);
            String uploader = cleanText(item.uploaderName(), maxField);
            String line = joinParts(
                    uuid.isBlank() ? "" : "uuid=" + uuid,
                    name.isBlank() ? "" : "name=" + name,
                    desc.isBlank() ? "" : "desc=" + desc,
                    uploader.isBlank() ? "" : "uploader=" + uploader
            );
            if (line.isBlank()) {
                continue;
            }
            if (maxChars > 0 && sb.length() + line.length() + 2 > maxChars) {
                break;
            }
            sb.append("- ").append(line).append('\n');
            count++;
        }
        return sb.toString().trim();
    }

    public Optional<String> streamRewrite(String query, String context, Consumer<String> onDelta) {
        if (!isRewriteEnabled()) {
            return Optional.empty();
        }
        String systemPrompt = "You rewrite video search queries. Return a single concise rewritten query in the same language. " +
                "Preserve important numbers, brands, and product codes. No quotes, no explanations.";
        if (!context.isBlank()) {
            systemPrompt = systemPrompt + " Use the context when helpful.\nContext:\n" + context;
        }
        List<OpenRouterClient.OpenRouterMessage> messages = List.of(
                new OpenRouterClient.OpenRouterMessage("system", systemPrompt),
                new OpenRouterClient.OpenRouterMessage("user", query)
        );
        return openRouterClient.stream(messages, properties.getRewriteMaxTokens(), "rewrite", onDelta);
    }

    public Optional<String> streamExpand(String query, String context, Consumer<String> onDelta) {
        if (!isExpansionEnabled()) {
            return Optional.empty();
        }
        int count = expansionTargetCount();
        if (count == 0) {
            return Optional.empty();
        }
        String prompt = buildExpansionPrompt(query, count);
        List<OpenRouterClient.OpenRouterMessage> messages = List.of(
                new OpenRouterClient.OpenRouterMessage("system", prompt),
                new OpenRouterClient.OpenRouterMessage("user", query)
        );
        return openRouterClient.stream(messages, properties.getExpansionMaxTokens(), "expand", onDelta);
    }

    public List<String> cacheExpansionFromStream(String rawQuery, String raw) {
        String query = safeTrim(rawQuery);
        int count = expansionTargetCount();
        if (query.isBlank() || count == 0) {
            return List.of();
        }
        List<String> parsed = parseJsonArray(raw);
        List<String> expanded = normalizeAndFillExpansions(query, parsed, count);
        expansionCache.put(expansionCacheKey(query, count), expanded);
        return expanded;
    }

    public int poolSize(int page, int size) {
        int safeSize = Math.max(size, 1);
        int safePage = Math.max(page, 0);
        long raw = (long) safeSize * (safePage + 1L) * Math.max(properties.getPoolMultiplier(), 1);
        int limit = Math.max(properties.getMaxPoolSize(), safeSize);
        return (int) Math.min(raw, limit);
    }

    public int suggestPoolSize(int limit) {
        int safeLimit = Math.max(limit, 1);
        int mult = Math.max(properties.getSuggestMultiplier(), 1);
        long raw = (long) safeLimit * mult;
        int cap = Math.max(properties.getMaxPoolSize(), safeLimit);
        return (int) Math.min(raw, cap);
    }

    private String rewriteQuery(String query, String context) {
        if (!properties.isEnabled() || !properties.isRewriteEnabled()) {
            return query;
        }
        String key = "rewrite:" + normalize(query) + ":" + contextKey(context);
        Optional<String> cached = rewriteCache.get(key);
        if (cached.isPresent()) {
            return cached.get();
        }
        String systemPrompt = "You rewrite video search queries. Return a single concise rewritten query in the same language. " +
                "Preserve important numbers, brands, and product codes. No quotes, no explanations.";
        if (!context.isBlank()) {
            systemPrompt = systemPrompt + " Use the context when helpful.\nContext:\n" + context;
        }
        List<OpenRouterClient.OpenRouterMessage> messages = List.of(
                new OpenRouterClient.OpenRouterMessage("system", systemPrompt),
                new OpenRouterClient.OpenRouterMessage("user", query)
        );
        Optional<String> result = openRouterClient.complete(messages, properties.getRewriteMaxTokens(), "rewrite");
        String rewritten = stripQuotes(result.orElse(query));
        if (rewritten.isBlank()) {
            rewritten = query;
        }
        rewriteCache.put(key, rewritten);
        return rewritten;
    }

    private List<String> expandQueries(String query, String context) {
        int count = expansionTargetCount();
        String key = expansionCacheKey(query, count);
        Optional<List<String>> cached = expansionCache.get(key);
        if (cached.isPresent()) {
            return cached.get();
        }
        if (count == 0) {
            return List.of();
        }
        String prompt = buildExpansionPrompt(query, count);
        List<OpenRouterClient.OpenRouterMessage> messages = List.of(
                new OpenRouterClient.OpenRouterMessage("system", prompt),
                new OpenRouterClient.OpenRouterMessage("user", query)
        );
        Optional<String> result = openRouterClient.complete(messages, properties.getExpansionMaxTokens(), "expand");
        List<String> parsed = parseJsonArray(result.orElse(""));
        List<String> expanded = normalizeAndFillExpansions(query, parsed, count);
        expansionCache.put(key, expanded);
        return expanded;
    }

    private List<FileMetadataDto> filterByShortQuery(String query, List<FileMetadataDto> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        Set<String> terms = buildShortQueryTerms(query);
        if (terms.isEmpty()) {
            return candidates;
        }
        List<FileMetadataDto> filtered = new ArrayList<>();
        for (FileMetadataDto item : candidates) {
            if (item == null) {
                continue;
            }
            String haystack = normalize(joinParts(item.name(), item.description(), item.uploaderName()));
            for (String term : terms) {
                if (!term.isBlank() && haystack.contains(term)) {
                    filtered.add(item);
                    break;
                }
            }
        }
        return filtered;
    }

    private Set<String> buildShortQueryTerms(String query) {
        String trimmed = safeTrim(query);
        if (trimmed.isBlank()) {
            return Set.of();
        }
        LinkedHashMap<String, String> expanded = new LinkedHashMap<>();
        putIfPresent(expanded, trimmed);
        TranslationBases bases = resolveTranslationBases(trimmed);
        if (bases != null) {
            putIfPresent(expanded, bases.ruBase());
            putIfPresent(expanded, bases.enBase());
            String translation = safeTrim(bases.translation());
            if (!translation.isBlank()) {
                putIfPresent(expanded, translation);
            }
        }
        if (containsCyrillic(trimmed)) {
            putIfPresent(expanded, transliterateToLatin(trimmed));
        } else if (containsLatin(trimmed)) {
            putIfPresent(expanded, transliterateToCyrillic(trimmed));
        }
        Set<String> terms = new HashSet<>();
        for (String value : expanded.values()) {
            String term = normalize(value);
            if (!term.isBlank()) {
                terms.add(term);
            }
        }
        return terms;
    }

    private List<FileMetadataDto> filterByTextMatch(String query, List<FileMetadataDto> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        String normalized = normalize(query);
        if (normalized.isBlank()) {
            return List.of();
        }
        List<String> tokens = tokenizeQuery(normalized);
        List<FileMetadataDto> filtered = new ArrayList<>();
        for (FileMetadataDto item : candidates) {
            if (item == null) {
                continue;
            }
            String haystack = normalize(joinParts(item.name(), item.description(), item.uploaderName()));
            if (haystack.contains(normalized)) {
                filtered.add(item);
                continue;
            }
            for (String token : tokens) {
                if (!token.isBlank() && haystack.contains(token)) {
                    filtered.add(item);
                    break;
                }
            }
        }
        return filtered;
    }

    private List<FileMetadataDto> filterByTextMatchAny(List<String> queries, List<FileMetadataDto> candidates) {
        if (queries == null || queries.isEmpty()) {
            return List.of();
        }
        List<FileMetadataDto> matches = new ArrayList<>();
        for (String variant : queries) {
            if (variant == null || variant.isBlank()) {
                continue;
            }
            matches.addAll(filterByTextMatch(variant, candidates));
        }
        return matches;
    }

    private List<String> prepareVariantQueries(String rawQuery, List<String> variants) {
        List<String> queries = new ArrayList<>();
        putIfPresentVariant(queries, rawQuery);
        if (variants != null) {
            for (String variant : variants) {
                putIfPresentVariant(queries, variant);
            }
        }
        return queries;
    }

    private void putIfPresentVariant(List<String> target, String value) {
        String trimmed = safeTrim(value);
        if (trimmed.isBlank()) {
            return;
        }
        for (String existing : target) {
            if (trimmed.equalsIgnoreCase(existing)) {
                return;
            }
        }
        target.add(trimmed);
    }

    private String buildVariantBlock(List<String> variants, String fallback) {
        if (variants == null || variants.isEmpty()) {
            return safeTrim(fallback);
        }
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String variant : variants) {
            if (variant == null || variant.isBlank()) {
                continue;
            }
            sb.append("- ").append(variant.trim()).append('\n');
            count++;
            if (count >= 10) {
                break;
            }
        }
        return sb.toString().trim();
    }

    private List<FileMetadataDto> dedupeCandidates(List<FileMetadataDto> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<FileMetadataDto> deduped = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (FileMetadataDto item : candidates) {
            String key = candidateKey(item);
            if (key != null && seen.add(key)) {
                deduped.add(item);
            }
        }
        return deduped;
    }

    private List<FileMetadataDto> mergeCandidates(List<FileMetadataDto> primary,
                                                  List<FileMetadataDto> secondary) {
        List<FileMetadataDto> merged = new ArrayList<>();
        if (primary != null && !primary.isEmpty()) {
            merged.addAll(primary);
        }
        if (secondary != null && !secondary.isEmpty()) {
            merged.addAll(secondary);
        }
        return merged;
    }

    private List<String> tokenizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String cleaned = query.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ").trim();
        if (cleaned.isBlank()) {
            return List.of();
        }
        String[] parts = cleaned.split("\\s+");
        Set<String> tokens = new HashSet<>();
        for (String part : parts) {
            addTokenVariants(tokens, part);
        }
        return new ArrayList<>(tokens);
    }

    private List<String> splitQueryTokens(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String cleaned = query.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ").trim();
        if (cleaned.isBlank()) {
            return List.of();
        }
        String[] parts = cleaned.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            String token = part.trim();
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private void addTokenVariants(Set<String> tokens, String part) {
        if (part == null) {
            return;
        }
        String token = part.trim();
        if (token.length() < 2) {
            return;
        }
        tokens.add(token);
        if (token.length() > 4 && token.endsWith("ies")) {
            tokens.add(token.substring(0, token.length() - 3) + "y");
            return;
        }
        if (token.length() > 4 && token.endsWith("es")) {
            tokens.add(token.substring(0, token.length() - 2));
            return;
        }
        if (token.length() > 3 && token.endsWith("s") && !token.endsWith("ss")) {
            tokens.add(token.substring(0, token.length() - 1));
        }
    }

    private boolean isSimpleQuery(String query) {
        if (query == null) {
            return true;
        }
        String trimmed = query.trim();
        if (trimmed.isBlank()) {
            return true;
        }
        if (trimmed.contains(" ")) {
            return false;
        }
        int limit = Math.max(properties.getShortQueryMaxLength(), 0);
        return limit > 0 && trimmed.length() <= limit;
    }

    private List<String> parseJsonArray(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String candidate = raw.trim();
        Object parsed = readJsonValue(candidate);
        List<String> extracted = extractStringsFromJson(parsed);
        if (!extracted.isEmpty()) {
            return extracted;
        }
        try {
            return objectMapper.readValue(candidate, new TypeReference<List<String>>() {
            });
        } catch (Exception ignored) {
        }
        Matcher matcher = JSON_ARRAY.matcher(candidate);
        if (matcher.find()) {
            try {
                Object nested = readJsonValue(matcher.group());
                List<String> nestedExtracted = extractStringsFromJson(nested);
                if (!nestedExtracted.isEmpty()) {
                    return nestedExtracted;
                }
                return objectMapper.readValue(matcher.group(), new TypeReference<List<String>>() {
                });
            } catch (Exception ignored) {
            }
        }
        return splitFallback(candidate);
    }

    private Object readJsonValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, Object.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> extractStringsFromJson(Object value) {
        if (value == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        if (value instanceof String text) {
            out.add(text);
            return out;
        }
        if (value instanceof List<?> list) {
            for (Object item : list) {
                out.addAll(extractStringsFromJson(item));
            }
            return out;
        }
        if (value instanceof Map<?, ?> map) {
            List<String> fromContainer = extractStringsFromContainer(map);
            if (!fromContainer.isEmpty()) {
                out.addAll(fromContainer);
            }
        }
        return out;
    }

    private List<String> extractStringsFromContainer(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String key : List.of("queries", "synonyms", "variants", "items", "results")) {
            Object value = map.get(key);
            if (value instanceof List<?> list) {
                for (Object item : list) {
                    out.addAll(extractStringsFromJson(item));
                }
            }
        }
        if (!out.isEmpty()) {
            return out;
        }
        for (String key : List.of("query", "term", "variant", "phrase", "synonym", "text")) {
            Object value = map.get(key);
            if (value instanceof String text && !text.isBlank()) {
                out.add(text);
            }
        }
        return out;
    }

    private String normalizeExpansion(String value) {
        String trimmed = stripQuotes(safeTrim(value));
        if (trimmed.isBlank()) {
            return "";
        }
        return trimmed.replaceAll("\\s+", " ").trim();
    }

    private String limitWords(String value, int maxWords) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return "";
        }
        if (maxWords <= 0) {
            return trimmed;
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length <= maxWords) {
            return trimmed;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(parts[i]);
        }
        return sb.toString();
    }
    private void fillExpansions(LinkedHashMap<String, String> target,
                                TranslationBases bases,
                                int count,
                                LanguagePlan plan) {
        if (target.size() >= count) {
            return;
        }
        if (bases == null) {
            return;
        }
        String ruBase = safeTrim(bases.ruBase());
        String enBase = safeTrim(bases.enBase());
        if (ruBase.isBlank() && enBase.isBlank()) {
            return;
        }
        int currentRu = countLanguage(target.values()).ruCount();
        int currentEn = countLanguage(target.values()).enCount();
        if (currentRu < plan.minRu() && !ruBase.isBlank()) {
            addLanguageFallbacks(target, buildRuFallbacks(ruBase), count);
            currentRu = countLanguage(target.values()).ruCount();
        }
        if (currentEn < plan.minEn() && !enBase.isBlank()) {
            addLanguageFallbacks(target, buildEnFallbacks(enBase), count);
        }
        if (target.size() < count) {
            List<String> combined = new ArrayList<>();
            combined.addAll(buildRuFallbacks(ruBase));
            combined.addAll(buildEnFallbacks(enBase));
            addLanguageFallbacks(target, combined, count);
        }
    }
    private void addLanguageFallbacks(LinkedHashMap<String, String> target, List<String> fallbacks, int count) {
        for (String fallback : fallbacks) {
            if (target.size() >= count) {
                break;
            }
            putIfPresent(target, fallback);
        }
    }

    private List<String> buildEnFallbacks(String base) {
        String query = safeTrim(base);
        if (query.isBlank()) {
            return List.of();
        }
        List<String> fallbacks = new ArrayList<>();
        for (String suffix : EN_FALLBACK_SUFFIXES) {
            fallbacks.add(query + " " + suffix);
        }
        fallbacks.add("video " + query);
        fallbacks.add("clip " + query);
        return fallbacks;
    }

    private List<String> buildRuFallbacks(String base) {
        String query = safeTrim(base);
        if (query.isBlank()) {
            return List.of();
        }
        List<String> fallbacks = new ArrayList<>();
        for (String suffix : RU_FALLBACK_SUFFIXES) {
            fallbacks.add(query + " " + suffix);
        }
        fallbacks.add("\u0432\u0438\u0434\u0435\u043e " + query);
        fallbacks.add("\u0440\u043e\u043b\u0438\u043a " + query);
        return fallbacks;
    }
    private LinkedHashMap<String, String> enforceLanguagePlan(Iterable<String> items,
                                                              LanguagePlan plan,
                                                              int count) {
        List<String> ru = new ArrayList<>();
        List<String> en = new ArrayList<>();
        List<String> other = new ArrayList<>();
        for (String item : items) {
            if (item == null || item.isBlank()) {
                continue;
            }
            if (containsCyrillic(item)) {
                ru.add(item);
            } else if (containsLatin(item)) {
                en.add(item);
            } else {
                other.add(item);
            }
        }
        LinkedHashMap<String, String> ordered = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(plan.minRu(), ru.size()); i++) {
            putIfPresent(ordered, ru.get(i));
        }
        for (int i = 0; i < Math.min(plan.minEn(), en.size()); i++) {
            putIfPresent(ordered, en.get(i));
        }
        for (String item : ru) {
            if (ordered.size() >= count) {
                return ordered;
            }
            putIfPresent(ordered, item);
        }
        for (String item : en) {
            if (ordered.size() >= count) {
                return ordered;
            }
            putIfPresent(ordered, item);
        }
        for (String item : other) {
            if (ordered.size() >= count) {
                return ordered;
            }
            putIfPresent(ordered, item);
        }
        return ordered;
    }

    private LanguagePlan languagePlan(String query, int count) {
        if (count <= 1) {
            return new LanguagePlan(1, 0);
        }
        int minRu = (count + 1) / 2;
        int minEn = count - minRu;
        return new LanguagePlan(minRu, minEn);
    }

    private String buildExpansionPrompt(String query, int count) {
        LanguagePlan plan = languagePlan(query, count);
        StringBuilder prompt = new StringBuilder();
        prompt.append("Return a JSON array with exactly ")
                .append(count)
                .append(" short synonym or keyword variants of the query. ");
        prompt.append("Return at least ")
                .append(plan.minRu())
                .append(" items in Russian (Cyrillic) and at least ")
                .append(plan.minEn())
                .append(" items in English (Latin), even if the query is in Russian. ");
        prompt.append("Include a direct translation of the full query into the other language as a standalone item. ");
        prompt.append("If the query is in Cyrillic, include the English translation; if it is in Latin, include the Russian translation. ");
        prompt.append("Preserve brand names, model codes, abbreviations, and numbers from the query exactly, and include them in every variant. ");
        String preserveLine = buildPreserveTokensPrompt(query);
        if (!preserveLine.isBlank()) {
            prompt.append(preserveLine);
        }
        prompt.append("Keep the meaning aligned with the original query; avoid unrelated topics or rephrasing into questions. ");
        prompt.append("Keep each item 1-3 words. Use spaces between words and do not concatenate words. ")
                .append("Include spelling variants when helpful. JSON only.");
        return prompt.toString();
    }

    private int expansionTargetCount() {
        int base = Math.max(properties.getExpansionCount(), 0);
        if (base == 0) {
            return 0;
        }
        long doubled = (long) base * 2L;
        return doubled > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) doubled;
    }

    private String expansionCacheKey(String query, int count) {
        return "expand:" + normalize(query) + ":" + count;
    }

    private TranslationBases resolveTranslationBases(String query) {
        String trimmed = safeTrim(query);
        if (trimmed.isBlank()) {
            return new TranslationBases(trimmed, trimmed, "");
        }
        List<String> preserveTokens = extractPreserveTokens(trimmed);
        boolean hasCyrillic = containsCyrillic(trimmed);
        boolean hasLatin = containsLatin(trimmed);
        String ruBase = trimmed;
        String enBase = trimmed;
        String translation = "";
        if (hasCyrillic && !hasLatin) {
            translation = translateQuery(trimmed, "English", "Latin");
            if (translation.isBlank() && preserveTokens.isEmpty()) {
                String translit = transliterateToLatin(trimmed);
                if (!translit.isBlank()) {
                    translation = translit;
                }
            }
            if (!translation.isBlank()) {
                enBase = translation;
            }
        } else if (hasLatin && !hasCyrillic) {
            translation = translateQuery(trimmed, "Russian", "Cyrillic");
            if (translation.isBlank() && preserveTokens.isEmpty()) {
                String translit = transliterateToCyrillic(trimmed);
                if (!translit.isBlank()) {
                    translation = translit;
                }
            }
            if (!translation.isBlank()) {
                ruBase = translation;
            }
        }
        return new TranslationBases(ruBase, enBase, translation);
    }

    private String translateQuery(String query, String targetLanguage, String alphabetHint) {
        if (!properties.isEnabled()) {
            return "";
        }
        String trimmed = safeTrim(query);
        if (trimmed.isBlank()) {
            return "";
        }
        String key = translationCacheKey(trimmed, targetLanguage);
        Optional<String> cached = translationCache.get(key);
        if (cached.isPresent()) {
            return cached.get();
        }
        String preserveLine = buildPreserveTokensPrompt(trimmed);
        String prompt = "Translate the search query into " + targetLanguage + " (" + alphabetHint + "). " +
                "Return a short 1-3 word translation. Preserve brand names and numbers. " +
                "Do not translate product codes, acronyms, or model names. " +
                preserveLine +
                "No quotes, no explanations.";
        List<OpenRouterClient.OpenRouterMessage> messages = List.of(
                new OpenRouterClient.OpenRouterMessage("system", prompt),
                new OpenRouterClient.OpenRouterMessage("user", trimmed)
        );
        Optional<String> result = openRouterClient.complete(messages, properties.getExpansionMaxTokens(), "translate");
        List<String> preserveTokens = extractPreserveTokens(trimmed);
        String translatedRaw = normalizeExpansion(result.orElse(""));
        if (translatedRaw.isBlank()) {
            return "";
        }
        if (!preserveTokens.isEmpty() && !containsAllTokens(translatedRaw, preserveTokens)) {
            return "";
        }
        boolean requireCyrillic = targetLanguage != null && targetLanguage.toLowerCase(Locale.ROOT).contains("russian");
        boolean requireLatin = targetLanguage != null && targetLanguage.toLowerCase(Locale.ROOT).contains("english");
        if (requireCyrillic && !containsCyrillic(translatedRaw)) {
            return "";
        }
        if (requireLatin && !containsLatin(translatedRaw)) {
            return "";
        }
        String translated = ensureTokensPresent(translatedRaw, preserveTokens);
        translated = limitWords(translated, 4);
        if (translated.isBlank()) {
            return "";
        }
        translationCache.put(key, translated);
        return translated;
    }

    private String buildPreserveTokensPrompt(String query) {
        List<String> tokens = extractPreserveTokens(query);
        if (tokens.isEmpty()) {
            return "";
        }
        return "Keep these tokens unchanged: " + String.join(", ", tokens) + ". Include all of them in every variant. ";
    }

    private List<String> extractPreserveTokens(String query) {
        String trimmed = safeTrim(query);
        if (trimmed.isBlank()) {
            return List.of();
        }
        String[] parts = trimmed.split("\\s+");
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            String cleaned = part.replaceAll("^[^\\p{IsAlphabetic}\\p{IsDigit}]+|[^\\p{IsAlphabetic}\\p{IsDigit}]+$", "");
            if (cleaned.isBlank()) {
                continue;
            }
            String normalized = normalize(cleaned);
            if (normalized.isBlank() || STOP_WORDS.contains(normalized)) {
                continue;
            }
            if (containsDigit(cleaned) || containsUppercase(cleaned) || isShortLatinToken(cleaned)) {
                tokens.add(cleaned);
            }
        }
        return new ArrayList<>(tokens);
    }

    private boolean containsDigit(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsUppercase(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isUpperCase(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean isShortLatinToken(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank() || trimmed.length() > 4) {
            return false;
        }
        if (!isLatinWordToken(trimmed)) {
            return false;
        }
        return !STOP_WORDS.contains(normalize(trimmed));
    }

    private boolean isLatinWordToken(String value) {
        boolean hasLetter = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isLetter(ch)) {
                if (!isLatinChar(ch)) {
                    return false;
                }
                hasLetter = true;
            } else if (!Character.isDigit(ch)) {
                return false;
            }
        }
        return hasLetter;
    }

    private boolean isLatinChar(char value) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(value);
        return block == Character.UnicodeBlock.BASIC_LATIN
                || block == Character.UnicodeBlock.LATIN_1_SUPPLEMENT
                || block == Character.UnicodeBlock.LATIN_EXTENDED_A
                || block == Character.UnicodeBlock.LATIN_EXTENDED_B;
    }

    private boolean containsAllTokens(String value, List<String> tokens) {
        if (value == null || value.isBlank() || tokens == null || tokens.isEmpty()) {
            return false;
        }
        String normalized = normalize(value);
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            String normalizedToken = normalize(token);
            if (!normalized.contains(normalizedToken)) {
                return false;
            }
        }
        return true;
    }

    private void enforcePreserveTokens(LinkedHashMap<String, String> target, List<String> preserveTokens) {
        if (target == null || target.isEmpty() || preserveTokens == null || preserveTokens.isEmpty()) {
            return;
        }
        LinkedHashMap<String, String> adjusted = new LinkedHashMap<>();
        for (String value : target.values()) {
            String candidate = ensureTokensPresent(value, preserveTokens);
            if (!candidate.isBlank()) {
                putIfPresent(adjusted, candidate);
            }
        }
        target.clear();
        target.putAll(adjusted);
    }

    private String ensureTokensPresent(String value, List<String> preserveTokens) {
        String candidate = safeTrim(value);
        if (candidate.isBlank()) {
            return "";
        }
        candidate = removeTransliterations(candidate, preserveTokens);
        String normalized = normalize(candidate);
        List<String> missing = new ArrayList<>();
        for (String token : preserveTokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            String normalizedToken = normalize(token);
            if (normalizedToken.isBlank()) {
                continue;
            }
            if (!normalized.contains(normalizedToken)) {
                missing.add(token);
            }
        }
        if (missing.isEmpty()) {
            return candidate;
        }
        String expanded = candidate + " " + String.join(" ", missing);
        return expanded.replaceAll("\\s+", " ").trim();
    }

    private String removeTransliterations(String value, List<String> preserveTokens) {
        if (preserveTokens == null || preserveTokens.isEmpty()) {
            return value;
        }
        List<String> tokens = splitQueryTokens(value);
        if (tokens.isEmpty()) {
            return value;
        }
        Set<String> remove = new HashSet<>();
        for (String token : preserveTokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            String cyr = transliterateToCyrillic(token);
            if (!cyr.isBlank() && !cyr.equalsIgnoreCase(token)) {
                remove.add(normalize(cyr));
            }
            String lat = transliterateToLatin(token);
            if (!lat.isBlank() && !lat.equalsIgnoreCase(token)) {
                remove.add(normalize(lat));
            }
        }
        if (remove.isEmpty()) {
            return value;
        }
        List<String> kept = new ArrayList<>();
        for (String token : tokens) {
            if (!remove.contains(normalize(token))) {
                kept.add(token);
            }
        }
        if (kept.isEmpty()) {
            return value;
        }
        return String.join(" ", kept);
    }

    private String translationCacheKey(String query, String targetLanguage) {
        String lang = targetLanguage == null ? "unknown" : targetLanguage.toLowerCase(Locale.ROOT);
        return "translate:" + lang + ":" + normalize(query);
    }

    private String transliterateToLatin(String value) {
        String trimmed = safeTrim(value);
        if (trimmed.isBlank()) {
            return "";
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            switch (ch) {
                case '\u0430' -> sb.append("a");
                case '\u0431' -> sb.append("b");
                case '\u0432' -> sb.append("v");
                case '\u0433' -> sb.append("g");
                case '\u0434' -> sb.append("d");
                case '\u0435' -> sb.append("e");
                case '\u0451' -> sb.append("yo");
                case '\u0436' -> sb.append("zh");
                case '\u0437' -> sb.append("z");
                case '\u0438' -> sb.append("i");
                case '\u0439' -> sb.append("y");
                case '\u043a' -> sb.append("k");
                case '\u043b' -> sb.append("l");
                case '\u043c' -> sb.append("m");
                case '\u043d' -> sb.append("n");
                case '\u043e' -> sb.append("o");
                case '\u043f' -> sb.append("p");
                case '\u0440' -> sb.append("r");
                case '\u0441' -> sb.append("s");
                case '\u0442' -> sb.append("t");
                case '\u0443' -> sb.append("u");
                case '\u0444' -> sb.append("f");
                case '\u0445' -> sb.append("kh");
                case '\u0446' -> sb.append("ts");
                case '\u0447' -> sb.append("ch");
                case '\u0448' -> sb.append("sh");
                case '\u0449' -> sb.append("shch");
                case '\u044b' -> sb.append("y");
                case '\u044d' -> sb.append("e");
                case '\u044e' -> sb.append("yu");
                case '\u044f' -> sb.append("ya");
                case '\u044c', '\u044a' -> {
                }
                default -> sb.append(ch);
            }
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private String transliterateToCyrillic(String value) {
        String trimmed = safeTrim(value);
        if (trimmed.isBlank()) {
            return "";
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < lower.length()) {
            if (lower.startsWith("shch", i) || lower.startsWith("sch", i)) {
                sb.append("\u0449");
                i += lower.startsWith("shch", i) ? 4 : 3;
                continue;
            }
            if (lower.startsWith("zh", i)) {
                sb.append("\u0436");
                i += 2;
                continue;
            }
            if (lower.startsWith("kh", i)) {
                sb.append("\u0445");
                i += 2;
                continue;
            }
            if (lower.startsWith("ch", i)) {
                sb.append("\u0447");
                i += 2;
                continue;
            }
            if (lower.startsWith("sh", i)) {
                sb.append("\u0448");
                i += 2;
                continue;
            }
            if (lower.startsWith("ya", i)) {
                sb.append("\u044f");
                i += 2;
                continue;
            }
            if (lower.startsWith("yu", i)) {
                sb.append("\u044e");
                i += 2;
                continue;
            }
            if (lower.startsWith("yo", i)) {
                sb.append("\u0451");
                i += 2;
                continue;
            }
            if (lower.startsWith("ts", i)) {
                sb.append("\u0446");
                i += 2;
                continue;
            }
            char ch = lower.charAt(i);
            switch (ch) {
                case 'a' -> sb.append("\u0430");
                case 'b' -> sb.append("\u0431");
                case 'c' -> sb.append("\u043a");
                case 'd' -> sb.append("\u0434");
                case 'e' -> sb.append("\u0435");
                case 'f' -> sb.append("\u0444");
                case 'g' -> sb.append("\u0433");
                case 'h' -> sb.append("\u0445");
                case 'i' -> sb.append("\u0438");
                case 'j' -> sb.append("\u0439");
                case 'k' -> sb.append("\u043a");
                case 'l' -> sb.append("\u043b");
                case 'm' -> sb.append("\u043c");
                case 'n' -> sb.append("\u043d");
                case 'o' -> sb.append("\u043e");
                case 'p' -> sb.append("\u043f");
                case 'q' -> sb.append("\u043a");
                case 'r' -> sb.append("\u0440");
                case 's' -> sb.append("\u0441");
                case 't' -> sb.append("\u0442");
                case 'u' -> sb.append("\u0443");
                case 'v' -> sb.append("\u0432");
                case 'w' -> sb.append("\u0432");
                case 'x' -> sb.append("\u043a\u0441");
                case 'y' -> sb.append("\u0439");
                case 'z' -> sb.append("\u0437");
                default -> sb.append(value.charAt(i));
            }
            i++;
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private LanguageCounts countLanguage(Iterable<String> values) {
        int ru = 0;
        int en = 0;
        if (values == null) {
            return new LanguageCounts(ru, en);
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (containsCyrillic(value)) {
                ru++;
            } else if (containsLatin(value)) {
                en++;
            }
        }
        return new LanguageCounts(ru, en);
    }

    private boolean containsCyrillic(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.UnicodeBlock.of(value.charAt(i)) == Character.UnicodeBlock.CYRILLIC) {
                return true;
            }
        }
        return false;
    }

    private boolean containsLatin(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(value.charAt(i));
            if (block == Character.UnicodeBlock.BASIC_LATIN ||
                    block == Character.UnicodeBlock.LATIN_1_SUPPLEMENT ||
                    block == Character.UnicodeBlock.LATIN_EXTENDED_A ||
                    block == Character.UnicodeBlock.LATIN_EXTENDED_B) {
                return true;
            }
        }
        return false;
    }

    private List<String> limitExpansions(List<String> expansions, int count) {
        if (expansions == null || expansions.isEmpty()) {
            return List.of();
        }
        if (count <= 0 || expansions.size() <= count) {
            return expansions;
        }
        return new ArrayList<>(expansions.subList(0, count));
    }

    private List<String> splitFallback(String raw) {
        List<String> list = new ArrayList<>();
        for (String part : raw.split("[\\n,;]+")) {
            String value = stripQuotes(safeTrim(part));
            if (!value.isBlank()) {
                list.add(value);
            }
        }
        return list;
    }

    private List<String> extractUuids(List<String> parsed, String raw) {
        Set<String> ids = new HashSet<>();
        if (parsed != null) {
            for (String value : parsed) {
                String uuid = extractUuid(value);
                if (!uuid.isBlank()) {
                    ids.add(uuid);
                }
            }
        }
        if (ids.isEmpty() && raw != null) {
            Matcher matcher = UUID_PATTERN.matcher(raw);
            while (matcher.find()) {
                ids.add(normalize(matcher.group()));
            }
        }
        return new ArrayList<>(ids);
    }

    private String extractUuid(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        Matcher matcher = UUID_PATTERN.matcher(value);
        if (matcher.find()) {
            return normalize(matcher.group());
        }
        return "";
    }

    private String prepareContext(String context) {
        if (context == null) {
            return "";
        }
        String trimmed = context.trim();
        if (trimmed.isBlank()) {
            return "";
        }
        int maxChars = Math.max(properties.getRagMaxChars(), 0);
        if (maxChars > 0 && trimmed.length() > maxChars) {
            return trimmed.substring(0, maxChars);
        }
        return trimmed;
    }

    private List<String> normalizeAndFillExpansions(String query, List<String> parsed, int count) {
        LinkedHashMap<String, String> cleaned = new LinkedHashMap<>();
        if (parsed != null) {
            for (String item : parsed) {
                String value = normalizeExpansion(item);
                if (value.isBlank()) {
                    continue;
                }
                if (value.equalsIgnoreCase(query)) {
                    continue;
                }
                putIfPresent(cleaned, value);
            }
        }
        TranslationBases bases = resolveTranslationBases(query);
        if (bases != null) {
            String ruBase = safeTrim(bases.ruBase());
            if (!ruBase.isBlank()) {
                putIfPresent(cleaned, ruBase);
            }
            String enBase = safeTrim(bases.enBase());
            if (!enBase.isBlank()) {
                putIfPresent(cleaned, enBase);
            }
            String translation = safeTrim(bases.translation());
            if (!translation.isBlank() && !translation.equalsIgnoreCase(query)) {
                putIfPresent(cleaned, translation);
            }
        }
        enforcePreserveTokens(cleaned, extractPreserveTokens(query));
        LanguagePlan plan = languagePlan(query, count);
        LinkedHashMap<String, String> ordered = enforceLanguagePlan(cleaned.values(), plan, count);
        trimForLanguagePlan(ordered, plan, count);
        if (ordered.size() < count) {
            fillExpansions(ordered, bases, count, plan);
        } else {
            LanguageCounts counts = countLanguage(ordered.values());
            if (counts.ruCount() < plan.minRu() || counts.enCount() < plan.minEn()) {
                fillExpansions(ordered, bases, count, plan);
            }
        }
        enforcePreserveTokens(ordered, extractPreserveTokens(query));
        return limitExpansions(new ArrayList<>(ordered.values()), count);
    }

    private void addCorpusExpansions(LinkedHashMap<String, String> target,
                                     String rawQuery,
                                     List<FileMetadataDto> corpusCandidates,
                                     int limit) {
        if (target == null || corpusCandidates == null || corpusCandidates.isEmpty()) {
            return;
        }
        int remaining = remainingCorpusSlots(target, limit);
        if (remaining == 0) {
            return;
        }
        String query = safeTrim(rawQuery);
        if (query.isBlank()) {
            return;
        }
        List<String> queryTokens = splitQueryTokens(query);
        Set<String> queryTokenSet = new HashSet<>();
        for (String token : queryTokens) {
            String normalized = normalize(token);
            if (!normalized.isBlank()) {
                queryTokenSet.add(normalized);
            }
        }
        List<FileMetadataDto> source = selectCorpusCandidates(corpusCandidates, queryTokenSet);
        Map<String, Integer> tokenScores = new LinkedHashMap<>();
        Map<String, Integer> bigramScores = new LinkedHashMap<>();
        for (FileMetadataDto item : source) {
            collectTokenScores(tokenScores, item == null ? null : item.name(), 5, queryTokenSet);
            collectTokenScores(tokenScores, item == null ? null : item.description(), 2, queryTokenSet);
            collectTokenScores(tokenScores, item == null ? null : item.uploaderName(), 1, queryTokenSet);
            collectBigramScores(bigramScores, item == null ? null : item.name(), 3);
            collectBigramScores(bigramScores, item == null ? null : item.description(), 1);
        }
        if (tokenScores.isEmpty() && bigramScores.isEmpty()) {
            return;
        }
        int added = 0;
        List<String> topBigrams = topKeys(bigramScores, remaining);
        for (String phrase : topBigrams) {
            if (added >= remaining) {
                break;
            }
            int before = target.size();
            putIfPresent(target, phrase);
            if (target.size() > before) {
                added++;
            }
        }
        if (added >= remaining) {
            return;
        }
        String baseQuery = normalizeExpansion(query);
        List<String> topTokens = topKeys(tokenScores, remaining * 2);
        for (String token : topTokens) {
            if (added >= remaining) {
                break;
            }
            if (queryTokens.isEmpty()) {
                int before = target.size();
                putIfPresent(target, token);
                if (target.size() > before) {
                    added++;
                }
                continue;
            }
            String combined = baseQuery + " " + token;
            int before = target.size();
            putIfPresent(target, combined);
            if (target.size() > before) {
                added++;
            }
        }
    }

    private int remainingCorpusSlots(LinkedHashMap<String, String> target, int limit) {
        if (target == null || limit <= 0) {
            return 0;
        }
        int baseCount = target.isEmpty() ? 0 : 1;
        int expansionCount = Math.max(target.size() - baseCount, 0);
        int remaining = limit - expansionCount;
        return Math.max(remaining, 0);
    }

    private List<FileMetadataDto> selectCorpusCandidates(List<FileMetadataDto> candidates, Set<String> queryTokens) {
        if (queryTokens == null || queryTokens.isEmpty()) {
            return candidates;
        }
        List<FileMetadataDto> matched = new ArrayList<>();
        for (FileMetadataDto item : candidates) {
            if (item == null) {
                continue;
            }
            if (candidateContainsToken(item, queryTokens)) {
                matched.add(item);
            }
        }
        return matched.isEmpty() ? candidates : matched;
    }

    private boolean candidateContainsToken(FileMetadataDto item, Set<String> queryTokens) {
        if (item == null || queryTokens == null || queryTokens.isEmpty()) {
            return false;
        }
        List<String> tokens = splitTextTokens(item.name());
        tokens.addAll(splitTextTokens(item.description()));
        tokens.addAll(splitTextTokens(item.uploaderName()));
        for (String token : tokens) {
            if (queryTokens.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private List<String> splitTextTokens(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }
        List<String> rawTokens = splitQueryTokens(text);
        List<String> normalized = new ArrayList<>(rawTokens.size());
        for (String token : rawTokens) {
            String value = normalize(token);
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private void collectTokenScores(Map<String, Integer> scores,
                                    String text,
                                    int weight,
                                    Set<String> skipTokens) {
        if (scores == null || text == null || text.isBlank()) {
            return;
        }
        List<String> tokens = splitTextTokens(text);
        for (String token : tokens) {
            if (isNoiseToken(token)) {
                continue;
            }
            if (skipTokens != null && skipTokens.contains(token)) {
                continue;
            }
            scores.merge(token, weight, Integer::sum);
        }
    }

    private void collectBigramScores(Map<String, Integer> scores, String text, int weight) {
        if (scores == null || text == null || text.isBlank()) {
            return;
        }
        List<String> tokens = splitTextTokens(text);
        List<String> filtered = new ArrayList<>();
        for (String token : tokens) {
            if (!isNoiseToken(token)) {
                filtered.add(token);
            }
        }
        for (int i = 0; i + 1 < filtered.size(); i++) {
            String phrase = filtered.get(i) + " " + filtered.get(i + 1);
            scores.merge(phrase, weight, Integer::sum);
        }
    }

    private List<String> topKeys(Map<String, Integer> scores, int limit) {
        if (scores == null || scores.isEmpty() || limit <= 0) {
            return List.of();
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(scores.entrySet());
        entries.sort((left, right) -> {
            int byScore = Integer.compare(right.getValue(), left.getValue());
            if (byScore != 0) {
                return byScore;
            }
            return left.getKey().compareTo(right.getKey());
        });
        int size = Math.min(limit, entries.size());
        List<String> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(entries.get(i).getKey());
        }
        return result;
    }

    private boolean isNoiseToken(String token) {
        if (token == null || token.isBlank()) {
            return true;
        }
        String normalized = normalize(token);
        if (normalized.length() < 2) {
            return true;
        }
        if (normalized.length() == 2 && Character.isDigit(normalized.charAt(0)) && Character.isDigit(normalized.charAt(1))) {
            return false;
        }
        if (normalized.length() == 1 && Character.isDigit(normalized.charAt(0))) {
            return true;
        }
        if (STOP_WORDS.contains(normalized)) {
            return true;
        }
        if (EN_FALLBACK_SUFFIXES.contains(normalized) || RU_FALLBACK_SUFFIXES.contains(normalized)) {
            return true;
        }
        return normalized.length() > 40;
    }

    private void trimForLanguagePlan(LinkedHashMap<String, String> target, LanguagePlan plan, int count) {
        if (target == null || target.isEmpty()) {
            return;
        }
        LanguageCounts counts = countLanguage(target.values());
        int needRu = Math.max(plan.minRu() - counts.ruCount(), 0);
        int needEn = Math.max(plan.minEn() - counts.enCount(), 0);
        int needed = needRu + needEn;
        int freeSlots = Math.max(count - target.size(), 0);
        int toRemove = Math.max(needed - freeSlots, 0);
        if (toRemove == 0) {
            return;
        }
        List<String> values = new ArrayList<>(target.values());
        for (int i = values.size() - 1; i >= 0 && toRemove > 0; i--) {
            String value = values.get(i);
            if (value == null || value.isBlank()) {
                continue;
            }
            boolean isRu = containsCyrillic(value);
            boolean isEn = containsLatin(value);
            if (needRu > 0 && isEn) {
                target.remove(normalize(value));
                toRemove--;
                needRu--;
                continue;
            }
            if (needEn > 0 && isRu) {
                target.remove(normalize(value));
                toRemove--;
                needEn--;
                continue;
            }
            if (!isRu && !isEn) {
                target.remove(normalize(value));
                toRemove--;
            }
        }
        if (toRemove == 0) {
            return;
        }
        values = new ArrayList<>(target.values());
        for (int i = values.size() - 1; i >= 0 && toRemove > 0; i--) {
            String value = values.get(i);
            if (value == null || value.isBlank()) {
                continue;
            }
            target.remove(normalize(value));
            toRemove--;
        }
    }

    private String buildFilterContext(List<FileMetadataDto> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }
        int maxItems = Math.max(properties.getFilterMaxItems(), 0);
        if (maxItems == 0) {
            maxItems = candidates.size();
        }
        int maxChars = Math.max(properties.getFilterMaxChars(), 0);
        int maxField = Math.max(properties.getFilterMaxFieldLength(), 0);

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (FileMetadataDto item : candidates) {
            if (count >= maxItems) {
                break;
            }
            if (item == null || item.uuid() == null) {
                continue;
            }
            String uuid = item.uuid().toString();
            String name = cleanText(item.name(), maxField);
            String desc = cleanText(item.description(), maxField);
            String uploader = cleanText(item.uploaderName(), maxField);
            String line = joinParts(
                    "uuid=" + uuid,
                    name.isBlank() ? "" : "name=" + name,
                    desc.isBlank() ? "" : "desc=" + desc,
                    uploader.isBlank() ? "" : "uploader=" + uploader
            );
            if (line.isBlank()) {
                continue;
            }
            if (maxChars > 0 && sb.length() + line.length() + 2 > maxChars) {
                break;
            }
            sb.append("- ").append(line).append('\n');
            count++;
        }
        return sb.toString().trim();
    }

    private String contextKey(String context) {
        if (context == null || context.isBlank()) {
            return "none";
        }
        return Integer.toHexString(context.hashCode());
    }

    private String cleanText(String value, int maxLen) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String cleaned = value.replaceAll("\\s+", " ").trim();
        if (maxLen > 0 && cleaned.length() > maxLen) {
            return cleaned.substring(0, maxLen);
        }
        return cleaned;
    }

    private String joinParts(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(part.trim());
        }
        return sb.toString();
    }

    private void putIfPresent(Map<String, String> target, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        target.putIfAbsent(normalize(value), value.trim());
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }


    private String candidateKey(FileMetadataDto item) {
        if (item == null) {
            return null;
        }
        if (item.uuid() != null) {
            return normalize(item.uuid().toString());
        }
        if (item.id() != null) {
            return normalize(item.id().toString());
        }
        return null;
    }

    private String stripQuotes(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
                (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private record LanguagePlan(int minRu, int minEn) {
    }

    private record LanguageCounts(int ruCount, int enCount) {
    }

    private record TranslationBases(String ruBase, String enBase, String translation) {
    }
}
