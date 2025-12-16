package ru.itmo.zavar.brainstop.search.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.itmo.zavar.brainstop.common.api.PageResponse;
import ru.itmo.zavar.brainstop.common.api.video.dto.FileMetadataDto;

import java.util.List;

@FeignClient(name = "video-service", contextId = "searchInternalClient", path = "/internal/search")
public interface SearchInternalClient {

    @GetMapping("/videos")
    PageResponse<FileMetadataDto> searchVideos(@RequestParam("vec") String vecLiteral,
                                               @RequestParam("page") int page,
                                               @RequestParam("size") int size);

    @GetMapping("/videos/suggest")
    List<FileMetadataDto> suggest(@RequestParam("vec") String vecLiteral,
                                  @RequestParam("limit") int limit);
}
