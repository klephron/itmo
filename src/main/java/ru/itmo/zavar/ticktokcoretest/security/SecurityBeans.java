package ru.itmo.zavar.ticktokcoretest.security;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.itmo.zavar.ticktokcoretest.model.UserEntity;
import ru.itmo.zavar.ticktokcoretest.repo.UserRepository;

@Configuration
public class SecurityBeans {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            UserEntity user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            return new UserPrincipal(user);
        };
    }

    public static class UserPrincipal implements UserDetails {

        private final UserEntity user;

        public UserPrincipal(UserEntity user) {
            this.user = user;
        }

        public String getAvatarUrl() {
            return user.getAvatarKey() == null
                    ? null
                    : "/api/users/" + user.getId() + "/avatar";
        }

        public Long getId() {
            return user.getId();
        }

        public String getDisplayName() {
            return user.getDisplayName();
        }

        public Set<Role> getRoles() {
            return user.getRoles();
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            if (user.getRoles() == null) {
                return java.util.List.of();
            }

            return user.getRoles().stream()
                    .map(Role::authority)                    // ROLE_...
                    .map(SimpleGrantedAuthority::new)
                    .toList();
        }

        @Override
        public String getPassword() {
            return user.getPassword();
        }

        @Override
        public String getUsername() {
            return user.getUsername();
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return user.isEnabled();
        }
    }
}

