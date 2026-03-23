package com.YuanQi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * 元启AI Agents平台启动类
 */
@EnableCaching
@SpringBootApplication
public class YuanQiApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuanQiApplication.class, args);
    }

}
