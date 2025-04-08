package ru.itmo.zavar.accountservice.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.accountservice.dto.request.AuthorityRequest;
import ru.itmo.zavar.accountservice.service.UserService;

import java.util.List;

@AllArgsConstructor
@RestController
public class AdminController {

    private final UserService userService;

    @GetMapping("/admin/user")
    public ResponseEntity<List<UserDetails>> getAllUsers() {
        return ResponseEntity.ok(this.userService.getAllUsers());
    }

    @GetMapping("/admin/user/{username}")
    public ResponseEntity<UserDetails> getUser(@PathVariable String username) {
        try {
            return ResponseEntity.ok(this.userService.loadUserByUsername(username));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/admin/user/{username}/authorities")
    public ResponseEntity<?> setAuthorities(@PathVariable String username, @RequestBody AuthorityRequest authorityRequest) {
        try {
            return ResponseEntity.ok(this.userService.setAuthorities(username, authorityRequest.getAuthorities()));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}