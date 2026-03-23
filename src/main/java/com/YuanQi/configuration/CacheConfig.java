package com.YuanQi.configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 */
@Configuration
public class CacheConfig {

    @Value("${yuanqi.cache.expire-minutes}")
    private Long expireMinutes;

    @Value("${yuanqi.cache.max-size}")
    private Long maxSize;

    /**
     * 验证码缓存
     */
    @Bean
    public Cache<String, String> caffeineCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(expireMinutes, TimeUnit.MINUTES)
                .maximumSize(maxSize)
                .build();
    }
}
