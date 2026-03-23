package com.YuanQi.configuration;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token配置类
 */
@Configuration
public class SaTokenConfig {

    /**
     * 使用JWT模式
     */
    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }

    /**
     * 权限认证接口扩展
     */
    @Bean
    public StpInterface stpInterface() {
        return new StpInterface() {
            @Override
            public List<String> getPermissionList(Object loginId, String loginType) {
                // 返回权限列表（暂空）
                return new ArrayList<>();
            }

            @Override
            public List<String> getRoleList(Object loginId, String loginType) {
                List<String> roles = new ArrayList<>();
                // 从Session中获取角色（登录时已缓存）
                String role = (String) StpUtil.getSessionByLoginId(loginId).get("role");
                if (role != null) {
                    roles.add(role);
                }
                return roles;
            }
        };
    }
}
