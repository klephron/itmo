package ru.itmo.zavar.account.security.jwt;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.account.security.CustomUserDetails;
import ru.itmo.zavar.account.service.UserService;

@Component
public class UserDetailsJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserService userService;

    public UserDetailsJwtConverter(UserService userService) {
        this.userService = userService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String username = jwt.getSubject();
        CustomUserDetails userDetails = userService.loadUserByUsername(username);

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }
}
