package ru.itmo.zavar.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.itmo.zavar.account.dto.UserDto;
import ru.itmo.zavar.account.entity.UserEntity;
import ru.itmo.zavar.account.mapper.UserMapper;
import ru.itmo.zavar.account.repository.UserRepository;
import ru.itmo.zavar.account.security.CustomUserDetails;
import ru.itmo.zavar.account.security.Roles;
import ru.itmo.zavar.account.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
public class UserServiceTest {

    private UserRepository repo;
    private PasswordEncoder encoder;
    private UserMapper userMapper;
    private UserService userService;

    @BeforeEach
    void setUp() {
        repo = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);
        userMapper = mock(UserMapper.class);

        userService = new UserService(repo, encoder, userMapper);
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
    void testRegisterSuccess() {
        when(repo.findByUsername("john")).thenReturn(Optional.empty());
        when(encoder.encode("pwd")).thenReturn("encoded");

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

        assertThrows(IllegalArgumentException.class, () -> userService.setAuthorities("john", List.of("ROLE_BLA")));
    }
}
