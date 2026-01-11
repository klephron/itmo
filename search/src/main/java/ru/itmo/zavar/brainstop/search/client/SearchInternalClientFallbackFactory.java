package ru.itmo.zavar.brainstop.search.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.PageResponse;
import ru.itmo.zavar.brainstop.common.api.video.dto.FileMetadataDto;

import java.util.List;

@Component
@Slf4j
public class SearchInternalClientFallbackFactory implements FallbackFactory<SearchInternalClient> {

    @Override
    public SearchInternalClient create(Throwable cause) {
        log.warn("SearchInternalClient fallback triggered: {}", cause == null ? "unknown" : cause.toString());
        return new SearchInternalClient() {
            @Override
            public PageResponse<FileMetadataDto> searchVideos(String vecLiteral, int page, int size) {
                return new PageResponse<>(List.of(), page, size, 0);
            }

            @Override
            public List<FileMetadataDto> suggest(String vecLiteral, int limit) {
                return List.of();
            }
        };
    }
}
