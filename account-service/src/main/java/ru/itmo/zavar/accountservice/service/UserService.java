package ru.itmo.zavar.accountservice.service;

import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.accountservice.dto.UserDto;
import ru.itmo.zavar.accountservice.entity.UserEntity;
import ru.itmo.zavar.accountservice.mapper.UserMapper;
import ru.itmo.zavar.accountservice.repository.UserRepository;

import java.util.List;

@AllArgsConstructor
@Service
public class UserService implements UserDetailsService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final UserCache userCache;
    private final UserMapper userMapper;

    public UserDto loadUserById(String id) throws UsernameNotFoundException {
        UserEntity userEntity = repo.findById(id).orElseThrow(() -> new UsernameNotFoundException(id));
        return userMapper.toDto(userEntity);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails cached = userCache.getUserFromCache(username);
        if (cached != null) {
            return cached;
        }

        UserEntity userEntity = repo.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        UserDetails build = User.builder()
                    .username(userEntity.getUsername())
                    .password(userEntity.getPassword())
                    .authorities(userEntity.getAuthorities().toArray(new String[0]))
                    .build();
        userCache.putUserInCache(build);
        return build;
    }

    public UserDetails setAuthorities(String username, List<String> authorities) throws UsernameNotFoundException {
        UserEntity userEntity = repo.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        userEntity.setAuthorities(authorities);
        userEntity = repo.save(userEntity);
        return User.builder()
                .username(userEntity.getUsername())
                .password(userEntity.getPassword())
                .authorities(userEntity.getAuthorities().toArray(new String[0]))
                .build();
    }

    public List<UserDetails> getAllUsers() {
        List<UserEntity> all = repo.findAll();
        return all.stream().map(userEntity ->
                User.builder()
                .username(userEntity.getUsername())
                .password(userEntity.getPassword())
                .authorities(userEntity.getAuthorities().toArray(new String[0]))
                .build()).toList();
    }

    public void register(String username, String password, List<String> roles) throws RuntimeException {
        if (repo.findByUsername(username).isPresent())
            throw new RuntimeException("User exists");
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(username);
        userEntity.setPassword(encoder.encode(password));
        userEntity.setAuthorities(roles);
        repo.save(userEntity);
        userCache.removeUserFromCache(username);
    }

    public boolean checkPassword(String username, String rawPassword) {
        UserEntity userEntity = repo.findByUsername(username).orElseThrow();
        return encoder.matches(rawPassword, userEntity.getPassword());
    }
}

