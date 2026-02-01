package ru.itmo.zavar.brainstop.search.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.itmo.zavar.brainstop.common.api.PageResponse;
import ru.itmo.zavar.brainstop.common.api.search.dto.SearchSuggestRequest;
import ru.itmo.zavar.brainstop.common.api.search.dto.SearchVideosRequest;
import ru.itmo.zavar.brainstop.common.api.video.dto.FileMetadataDto;

import java.util.List;

@FeignClient(name = "video-service", contextId = "searchInternalClient", path = "/internal/search", fallbackFactory = SearchInternalClientFallbackFactory.class)
public interface SearchInternalClient {

    @PostMapping("/videos")
    PageResponse<FileMetadataDto> searchVideos(@RequestBody SearchVideosRequest request);

    @PostMapping("/videos/suggest")
    List<FileMetadataDto> suggest(@RequestBody SearchSuggestRequest request);
}
