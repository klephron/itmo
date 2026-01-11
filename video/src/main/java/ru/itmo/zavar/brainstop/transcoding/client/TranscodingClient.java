package ru.itmo.zavar.brainstop.transcoding.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingCommand;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingResult;

@FeignClient(name = "transcoding-service", contextId = "transcodingClient", path = "/internal/transcoding", fallbackFactory = TranscodingClientFallbackFactory.class)
public interface TranscodingClient {

    @PostMapping("/jobs")
    TranscodingResult transcode(@RequestBody TranscodingCommand command);
}
