package ru.itmo.zavar.accountservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.itmo.zavar.accountservice.dto.UserDto;
import ru.itmo.zavar.accountservice.entity.UserEntity;
import ru.itmo.zavar.accountservice.mapper.UserMapper;
import ru.itmo.zavar.accountservice.repository.UserRepository;
import ru.itmo.zavar.accountservice.security.CustomUserDetails;
import ru.itmo.zavar.accountservice.security.Roles;
import ru.itmo.zavar.accountservice.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
public class UserServiceTest {

    private UserRepository repo;
    private PasswordEncoder encoder;
    private UserCache userCache;
    private UserMapper userMapper;
    private UserService userService;

    @BeforeEach
    void setUp() {
        repo = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);
        userCache = mock(UserCache.class);
        userMapper = mock(UserMapper.class);

        userService = new UserService(repo, encoder, userCache, userMapper);
    }

    @Test
    void testLoadUserByIdSuccess() {
        UserEntity entity = new UserEntity();
        entity.setId("user-1");
        entity.setUsername("john");
        when(repo.findById("user-1")).thenReturn(Optional.of(entity));

        UserDto dto = new UserDto("user-1", "john", "em@il", List.of("ROLE_USER"));
        when(userMapper.toDto(entity)).thenReturn(dto);

        UserDto loaded = userService.loadUserById("user-1");
        assertEquals("user-1", loaded.getId());
        verify(repo).findById("user-1");
    }

    @Test
    void testLoadUserByUsernameCached() {
        CustomUserDetails cached = new CustomUserDetails(
                "user-1", "john", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userCache.getUserFromCache("john")).thenReturn(cached);

        CustomUserDetails result = userService.loadUserByUsername("john");
        assertEquals("john", result.getUsername());
        verify(repo, never()).findByUsername(anyString());
    }

    @Test
    void testLoadUserByUsernameNoCache() {
        when(userCache.getUserFromCache("john")).thenReturn(null);

        UserEntity entity = new UserEntity();
        entity.setId("user-1");
        entity.setUsername("john");
        entity.setPassword("encoded");
        entity.setAuthorities(List.of("ROLE_ADMIN"));

        when(repo.findByUsername("john")).thenReturn(Optional.of(entity));

        CustomUserDetails result = userService.loadUserByUsername("john");
        assertEquals("john", result.getUsername());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        verify(userCache).putUserInCache(any(CustomUserDetails.class));
    }

    @Test
    void testRegisterSuccess() {
        when(repo.findByUsername("john")).thenReturn(Optional.empty());
        when(encoder.encode("pwd")).thenReturn("encoded");
        doNothing().when(userCache).removeUserFromCache("john");

        userService.register("john", "pwd", "john@exampl.com", List.of("ROLE_USER"));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(repo).save(captor.capture());
        UserEntity saved = captor.getValue();
        assertEquals("john", saved.getUsername());
        assertEquals("encoded", saved.getPassword());
        assertEquals(List.of("ROLE_USER"), saved.getAuthorities());
    }

    @Test
    void testSetAuthoritiesSuccess() {
        UserEntity entity = new UserEntity();
        entity.setId("user-1");
        entity.setUsername("john");
        entity.setAuthorities(List.of(Roles.USER.toString()));
        when(repo.findByUsername("john")).thenReturn(Optional.of(entity));
        when(repo.save(entity)).thenReturn(entity);

        userService.setAuthorities("john", List.of(Roles.ADMIN.toString()));
        verify(userCache).removeUserFromCache("john");
        verify(repo).save(entity);
        assertEquals(List.of(Roles.ADMIN.toString()), entity.getAuthorities());
    }

    @Test
    void testSetAuthoritiesInvalid() {
        UserEntity entity = new UserEntity();
        entity.setId("user-1");
        entity.setUsername("john");
        entity.setAuthorities(List.of(Roles.USER.toString()));
        when(repo.findByUsername("john")).thenReturn(Optional.of(entity));

        assertThrows(IllegalArgumentException.class, () ->
                userService.setAuthorities("john", List.of("ROLE_BLA")));
    }
}
