package ru.itmo.zavar.brainstop.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.user.UserEntity;
import ru.itmo.zavar.brainstop.user.UserRepository;

import java.util.Collection;
import java.util.Set;

@Configuration
public class SecurityBeans {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByUsername(username)
                .map(UserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public static class UserPrincipal implements UserDetails {
        private final UserEntity user;

        public UserPrincipal(UserEntity user) {
            this.user = user;
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

        public String getAvatarUrl() {
            return user.getAvatarKey() == null ? null : "/api/users/" + user.getId() + "/avatar";
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            if (user.getRoles() == null) {
                return java.util.List.of();
            }
            return user.getRoles().stream()
                    .map(Role::authority)
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
        public boolean isEnabled() {
            return user.isEnabled();
        }
    }
}
