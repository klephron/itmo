package ru.itmo.zavar.account.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.account.dto.request.AuthorityRequest;
import ru.itmo.zavar.account.security.CustomUserDetails;
import ru.itmo.zavar.account.service.UserService;

import java.util.List;

@AllArgsConstructor
@RestController
@Tag(name = "Admin API", description = "Управление пользователями и их правами")
public class AdminController {

    private final UserService userService;

    @Operation(summary = "Получить всех пользователей", description = "Возвращает список всех зарегистрированных пользователей")
    @ApiResponse(responseCode = "200", description = "Список пользователей успешно получен")
    @GetMapping("/admin/user")
    public ResponseEntity<List<UserDetails>> getAllUsers() {
        return ResponseEntity.ok(this.userService.getAllUsers());
    }

    @Operation(summary = "Получить пользователя по имени", description = "Возвращает данные пользователя по его имени пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь найден", content = @Content(schema = @Schema(implementation = CustomUserDetails.class))),
            @ApiResponse(responseCode = "400", description = "Пользователь не найден", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/admin/user/{username}")
    public ResponseEntity<?> getUser(
            @Parameter(description = "Имя пользователя", required = true)
            @PathVariable String username) {
        try {
            return ResponseEntity.ok(this.userService.loadUserByUsername(username));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Назначить роли пользователю", description = "Обновляет список ролей (authorities) для указанного пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Роли успешно обновлены", content = @Content(schema = @Schema(implementation = CustomUserDetails.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка при обновлении ролей", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PutMapping("/admin/user/{username}/authorities")
    public ResponseEntity<?> setAuthorities(
            @Parameter(description = "Имя пользователя", required = true)
            @PathVariable String username,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Новый список ролей пользователя",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AuthorityRequest.class))
            )
            @RequestBody AuthorityRequest authorityRequest) {
        try {
            return ResponseEntity.ok(this.userService.setAuthorities(username, authorityRequest.getAuthorities()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
