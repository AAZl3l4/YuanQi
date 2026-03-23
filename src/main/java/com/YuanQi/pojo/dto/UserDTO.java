package com.YuanQi.pojo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户操作DTO（注册/登录/更新）
 */
@Data
public class UserDTO {

    /**
     * 用户名
     */
    @Size(min = 3, max = 20, message = "用户名长度3-20位")
    private String username;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 密码
     */
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;

    /**
     * 验证码（注册时使用）
     */
    private String verifyCode;
}
