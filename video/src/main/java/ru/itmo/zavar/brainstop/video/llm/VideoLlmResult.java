package ru.itmo.zavar.brainstop.video.llm;

import java.util.List;
import java.util.Locale;

public record VideoLlmResult(List<String> tags, boolean flagged, List<String> categories) {

    public static VideoLlmResult empty() {
        return new VideoLlmResult(List.of(), false, List.of());
    }

    public String tagsText() {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return String.join(" ", tags);
    }

    public String categoriesText() {
        if (categories == null || categories.isEmpty()) {
            return "";
        }
        return String.join(",", categories);
    }

    public static String normalizeCategory(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
