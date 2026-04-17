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
     * 缓存（知识库加载标记，不过期）
     */
    @Bean
    public Cache<String, String> caffeineCache() {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(maxSize);
        if (expireMinutes != null && expireMinutes > 0) {
            builder.expireAfterWrite(expireMinutes, TimeUnit.MINUTES);
        }
        return builder.build();
    }
}
