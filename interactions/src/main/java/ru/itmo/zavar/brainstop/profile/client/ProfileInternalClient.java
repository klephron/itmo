package ru.itmo.zavar.brainstop.profile.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummaryDto;
import ru.itmo.zavar.brainstop.common.api.profile.dto.FollowCheckRequest;
import ru.itmo.zavar.brainstop.common.security.Role;

import java.util.List;
import java.util.Set;

@FeignClient(name = "profile-service", path = "/internal/profile")
public interface ProfileInternalClient {

    @GetMapping("/users/{id}/summary")
    UserSummaryDto getSummary(@PathVariable("id") Long id);

    @PostMapping(value = "/follow/check", consumes = MediaType.APPLICATION_JSON_VALUE)
    Set<Long> checkFollowing(@RequestBody FollowCheckRequest request);

    @GetMapping("/admin/users")
    List<ProfileUserDetails> findAllUsers();

    @GetMapping("/admin/users/{id}")
    ProfileUserDetails findUser(@PathVariable("id") Long id);

    @GetMapping("/admin/users/{id}/exists")
    Boolean exists(@PathVariable("id") Long id);

    @PostMapping("/admin/users/{id}/enabled")
    ProfileUserDetails setEnabled(@PathVariable("id") Long id, @RequestParam("enabled") boolean enabled);

    @PostMapping(value = "/admin/users/{id}/roles", consumes = MediaType.APPLICATION_JSON_VALUE)
    ProfileUserDetails updateRoles(@PathVariable("id") Long id, @RequestBody Set<Role> roles);
}
