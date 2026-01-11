package ru.itmo.zavar.brainstop.transcoding.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingCommand;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingResult;

@Component
@Slf4j
public class TranscodingClientFallbackFactory implements FallbackFactory<TranscodingClient> {

    @Override
    public TranscodingClient create(Throwable cause) {
        log.warn("TranscodingClient fallback triggered: {}", cause == null ? "unknown" : cause.toString());
        return new TranscodingClient() {
            @Override
            public TranscodingResult transcode(TranscodingCommand command) {
                return null;
            }
        };
    }
}
