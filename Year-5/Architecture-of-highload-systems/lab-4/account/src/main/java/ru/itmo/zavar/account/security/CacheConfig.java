package ru.itmo.zavar.account.security;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.cache.SpringCacheBasedUserCache;

import java.util.Objects;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("users");
    }

    @Bean
    public UserCache userCache(CacheManager cacheManager) {
        return new SpringCacheBasedUserCache(Objects.requireNonNull(cacheManager.getCache("users")));
    }
}
