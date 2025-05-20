package ru.itmo.zavar.account.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.account.dto.UserDto;
import ru.itmo.zavar.account.entity.UserEntity;
import ru.itmo.zavar.account.mapper.UserMapper;
import ru.itmo.zavar.account.repository.UserRepository;
import ru.itmo.zavar.account.security.CustomUserDetails;
import ru.itmo.zavar.account.security.Roles;

import java.util.List;

@Slf4j(topic = "AS-UserService")
@AllArgsConstructor
@Service
public class UserService implements UserDetailsService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final UserMapper userMapper;

    public UserDto loadUserById(String id) throws UsernameNotFoundException {
        UserEntity userEntity = repo.findById(id).orElseThrow(() -> {
            log.debug("User not found by id: {}", id);
            return new UsernameNotFoundException("User not found");
        });
        log.info("Loaded user by id: {}", userEntity);
        return userMapper.toDto(userEntity);
    }

    @Override
    public CustomUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = repo.findByUsername(username).orElseThrow(() -> {
            log.debug("User not found by name: {}", username);
            return new UsernameNotFoundException("User not found");
        });
        log.info("Loaded user by name: {}", userEntity);
        CustomUserDetails build = new CustomUserDetails(userEntity.getId(),
                userEntity.getUsername(),
                userEntity.getPassword(),
                userEntity.getAuthorities().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList());
        return build;
    }

    public CustomUserDetails setAuthorities(String username, List<String> authorities) throws UsernameNotFoundException, IllegalArgumentException {
        UserEntity userEntity = repo.findByUsername(username).orElseThrow(() -> {
            log.debug("User not found by name: {}", username);
            return new UsernameNotFoundException("User not found");
        });
        if (!authorities.stream().allMatch(Roles::isValid))
            throw new IllegalArgumentException("Invalid authorities");
        userEntity.setAuthorities(authorities);
        userEntity = repo.save(userEntity);
        log.info("Saved user: {}", userEntity);
        return new CustomUserDetails(userEntity.getId(),
                userEntity.getUsername(),
                userEntity.getPassword(),
                userEntity.getAuthorities().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList());
    }

    public List<UserDetails> getAllUsers() {
        List<UserEntity> all = repo.findAll();
        log.info("Found {} users", all.size());
        return all.stream().map(userEntity ->
                User.builder()
                .username(userEntity.getUsername())
                .password(userEntity.getPassword())
                .authorities(userEntity.getAuthorities().toArray(new String[0]))
                .build()).toList();
    }

    public void register(String username, String password, String email, List<String> roles) throws RuntimeException {
        if (repo.findByUsername(username).isPresent()) {
            log.info("Username already in use: {}", username);
            throw new RuntimeException("User exists");
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(username);
        userEntity.setPassword(encoder.encode(password));
        userEntity.setAuthorities(roles);
        userEntity.setEmail(email);
        repo.save(userEntity);
        log.info("Saved user: {}", userEntity);
    }

    public boolean checkPassword(String username, String rawPassword) throws UsernameNotFoundException {
        UserEntity userEntity = repo.findByUsername(username).orElseThrow(() -> {
            log.debug("User not found by name: {}", username);
            return new UsernameNotFoundException("User not found");
        });
        return encoder.matches(rawPassword, userEntity.getPassword());
    }
}
