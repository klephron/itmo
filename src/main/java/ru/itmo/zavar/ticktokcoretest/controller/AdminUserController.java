package ru.itmo.zavar.ticktokcoretest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.ticktokcoretest.dto.UpdateUserRolesRequest;
import ru.itmo.zavar.ticktokcoretest.model.UserEntity;
import ru.itmo.zavar.ticktokcoretest.repo.UserRepository;
import ru.itmo.zavar.ticktokcoretest.security.Role;

import java.util.Set;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // посмотреть роли пользователя
    @GetMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Set<Role>> getUserRoles(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(UserEntity::getRoles)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // обновить роли пользователя (полный сет)
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Set<Role>> updateUserRoles(@PathVariable Long id,
                                                     @RequestBody UpdateUserRolesRequest req) {
        return userRepository.findById(id)
                .map(u -> {
                    u.getRoles().clear();
                    if (req.roles() != null && !req.roles().isEmpty()) {
                        u.getRoles().addAll(req.roles());
                    } else {
                        // на всякий случай гарантируем хотя бы USER
                        u.getRoles().add(Role.USER);
                    }
                    userRepository.save(u);
                    return ResponseEntity.ok(u.getRoles());
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
