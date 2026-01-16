package ru.itmo.zavar.brainstop.search.llm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "llm.search")
public class SearchLlmProperties {
    private boolean enabled = false;
    private boolean rewriteEnabled = true;
    private boolean expansionEnabled = true;
    private int expansionCount = 3;
    private int minQueryLength = 3;
    private int poolMultiplier = 2;
    private int maxPoolSize = 60;
    private int suggestMultiplier = 2;
    private int rewriteMaxTokens = 80;
    private int expansionMaxTokens = 120;
    private int shortQueryMaxLength = 4;
    private boolean filterEnabled = false;
    private int filterMaxTokens = 120;
    private int filterMaxItems = 20;
    private int filterMaxChars = 2000;
    private int filterMaxFieldLength = 140;
    private Duration cacheTtl = Duration.ofMinutes(30);
    private boolean ragEnabled = false;
    private int ragMaxItems = 20;
    private int ragMaxChars = 2000;
    private int ragMaxFieldLength = 140;
}
