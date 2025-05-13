package ru.itmo.zavar.log;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;

@Slf4j(topic = "RuleEngine")
public final class LoggerWrapper {
    public static void log(Level level, String message, Object... objects) {
        log.atLevel(level).log(message, objects);
    }
}
