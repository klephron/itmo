package ru.itmo.zavar.ticktokcoretest.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.itmo.zavar.ticktokcoretest.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.ticktokcoretest.common.security.Role;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@FeignClient(name = "profile-service", contextId = "profileAdminClient", path = "/internal/profile/admin")
public interface ProfileAdminClient {

    @GetMapping("/users")
    List<ProfileUserDetails> findAllUsers();

    @GetMapping("/users/{id}")
    ProfileUserDetails findUser(@PathVariable("id") Long id);

    @GetMapping("/users/{id}/exists")
    Boolean exists(@PathVariable("id") Long id);

    @PostMapping("/users/{id}/enabled")
    ProfileUserDetails setEnabled(@PathVariable("id") Long id, @RequestParam("enabled") boolean enabled);

    @PostMapping("/users/{id}/roles")
    ProfileUserDetails updateRoles(@PathVariable("id") Long id, @RequestBody Set<Role> roles);
}
