package com.YuanQi.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 自定义拦截器
 */
@Slf4j
@Component
public class Blocker implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        /*
            - 待完成
            - 请求日志 - 记录每个请求的IP、耗时、参数等
            - Token统计 - 统计接口调用次数，用于用量监控
            - 限流 - 控制请求频率
            - 黑名单 - 拦截恶意IP
        * */
        log.debug("请求路径: {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }
}
