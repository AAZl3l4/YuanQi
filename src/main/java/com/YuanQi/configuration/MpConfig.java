package com.YuanQi.configuration;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MybatisPlus配置类
 */
@Configuration
public class MpConfig {
    //分页插件
     @Bean
     public MybatisPlusInterceptor mpInterceptor() {
         MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
         interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
         return interceptor;
     }
}
