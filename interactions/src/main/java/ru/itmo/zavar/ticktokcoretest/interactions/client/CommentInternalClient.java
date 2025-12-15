package ru.itmo.zavar.ticktokcoretest.interactions.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.itmo.zavar.ticktokcoretest.comment.dto.CommentDto;
import ru.itmo.zavar.ticktokcoretest.comment.dto.CreateCommentRequest;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "core-service", contextId = "commentInternalClient", path = "/internal/interactions")
public interface CommentInternalClient {

    @GetMapping("/videos/{uuid}/comments")
    List<CommentDto> list(@PathVariable("uuid") UUID videoUuid,
                          @RequestParam(value = "userId", required = false) Long userId);

    @PostMapping("/videos/{uuid}/comments")
    CommentDto add(@PathVariable("uuid") UUID videoUuid,
                   @RequestParam("userId") Long userId,
                   @RequestParam("displayName") String displayName,
                   @RequestParam(value = "avatarUrl", required = false) String avatarUrl,
                   @RequestBody CreateCommentRequest req);

    @DeleteMapping("/videos/{uuid}/comments/{commentId}")
    void delete(@PathVariable("uuid") UUID videoUuid,
                @PathVariable("commentId") Long commentId,
                @RequestParam("userId") Long userId,
                @RequestParam("moderator") boolean moderator);
}
