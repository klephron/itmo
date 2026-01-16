package ru.itmo.zavar.brainstop.video.llm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "llm.video")
public class VideoLlmProperties {
    private boolean enabled = false;
    private boolean taggingEnabled = true;
    private boolean moderationEnabled = true;
    private int tagCount = 8;
    private int tagMaxTokens = 120;
    private int moderationMaxTokens = 120;
    private Duration cacheTtl = Duration.ofHours(6);
}
