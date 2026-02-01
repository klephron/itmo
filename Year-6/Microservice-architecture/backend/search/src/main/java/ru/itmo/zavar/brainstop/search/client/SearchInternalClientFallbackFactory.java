package ru.itmo.zavar.brainstop.search.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.PageResponse;
import ru.itmo.zavar.brainstop.common.api.search.dto.SearchSuggestRequest;
import ru.itmo.zavar.brainstop.common.api.search.dto.SearchVideosRequest;
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
            public PageResponse<FileMetadataDto> searchVideos(SearchVideosRequest request) {
                int page = request != null && request.page() != null ? request.page() : 0;
                int size = request != null && request.size() != null ? request.size() : 0;
                return new PageResponse<>(List.of(), page, size, 0);
            }

            @Override
            public List<FileMetadataDto> suggest(SearchSuggestRequest request) {
                return List.of();
            }
        };
    }
}
