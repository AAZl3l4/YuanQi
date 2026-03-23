package com.YuanQi.configuration;

import com.YuanQi.utils.JacksonObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.List;

// MVC配置类
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // 拦截器配置
    @Autowired
    Blocker blocker;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(blocker)
                .addPathPatterns("/**")
                .excludePathPatterns("/user/login","/user/register","/file/**");
    }

    // 跨域配置
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }

    // 配置消息转换器 影响 Spring MVC 处理 HTTP 请求和响应时的序列化和反序列化行为。
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        //创建消息转换器对象
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        //设置对象转换器(自定义的)
        messageConverter.setObjectMapper(new JacksonObjectMapper());
        //将消息转换器追加的mvc科技的转换器集合中 并设置到第一位
        converters.add(0,messageConverter);

    }
}