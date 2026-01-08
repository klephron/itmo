package ru.itmo.zavar.brainstop.profile.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.itmo.zavar.brainstop.common.api.profile.UserAccountDto;
import ru.itmo.zavar.brainstop.common.security.Role;

import java.util.List;
import java.util.Set;

@FeignClient(name = "brainstop-gateway", path = "/internal/users", fallbackFactory = AuthInternalClientFallbackFactory.class)
public interface AuthInternalClient {

    @GetMapping("/{id}")
    UserAccountDto getUser(@PathVariable("id") Long id);

    @GetMapping("/{id}/exists")
    Boolean exists(@PathVariable("id") Long id);

    @GetMapping
    List<UserAccountDto> listUsers();

    @PostMapping(value = "/batch", consumes = MediaType.APPLICATION_JSON_VALUE)
    List<UserAccountDto> getUsers(@RequestBody List<Long> ids);

    @PostMapping("/{id}/avatar")
    UserAccountDto updateAvatar(@PathVariable("id") Long id, @RequestParam("avatarKey") String avatarKey);

    @PostMapping("/{id}/enabled")
    UserAccountDto setEnabled(@PathVariable("id") Long id, @RequestParam("enabled") boolean enabled);

    @PostMapping(value = "/{id}/roles", consumes = MediaType.APPLICATION_JSON_VALUE)
    UserAccountDto updateRoles(@PathVariable("id") Long id, @RequestBody Set<Role> roles);
}
