package ru.itmo.zavar.brainstop.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.common.api.profile.UserAccountDto;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.user.UserEntity;
import ru.itmo.zavar.brainstop.user.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@Tag(name = "Пользователи (внутренние)", description = "Внутренний API аккаунтов пользователей (auth-service)")
public class UserInternalController {

    private final UserRepository userRepository;

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по id")
    public ResponseEntity<UserAccountDto> getUser(@PathVariable Long id) {
        return this.userRepository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/exists")
    @Operation(summary = "Проверить существование пользователя")
    public Boolean exists(@PathVariable Long id) {
        return this.userRepository.existsById(id);
    }

    @GetMapping
    @Operation(summary = "Список всех пользователей")
    public List<UserAccountDto> listUsers() {
        return this.userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping("/batch")
    @Operation(summary = "Получить пользователей по списку id")
    public List<UserAccountDto> getUsers(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return this.userRepository.findAllById(ids).stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping("/{id}/avatar")
    @Operation(summary = "Обновить ключ аватара пользователя")
    public ResponseEntity<UserAccountDto> updateAvatar(@PathVariable Long id, @RequestParam("avatarKey") String avatarKey) {
        return this.userRepository.findById(id)
                .map(user -> {
                    user.setAvatarKey(avatarKey);
                    return this.userRepository.save(user);
                })
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/enabled")
    @Operation(summary = "Включить/отключить пользователя")
    public ResponseEntity<UserAccountDto> setEnabled(@PathVariable Long id, @RequestParam("enabled") boolean enabled) {
        return this.userRepository.findById(id)
                .map(user -> {
                    user.setEnabled(enabled);
                    return this.userRepository.save(user);
                })
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/roles")
    @Operation(summary = "Обновить роли пользователя")
    public ResponseEntity<UserAccountDto> updateRoles(@PathVariable Long id, @RequestBody Set<Role> roles) {
        return this.userRepository.findById(id)
                .map(user -> {
                    user.getRoles().clear();
                    if (roles != null && !roles.isEmpty()) {
                        user.getRoles().addAll(roles);
                    } else {
                        user.getRoles().add(Role.USER);
                    }
                    return this.userRepository.save(user);
                })
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private UserAccountDto toDto(UserEntity user) {
        return new UserAccountDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.isEnabled(),
                user.getRoles(),
                user.getCreatedAt(),
                user.getAvatarKey()
        );
    }
}
