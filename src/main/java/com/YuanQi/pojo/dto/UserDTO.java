package com.YuanQi.pojo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户操作DTO（注册/登录/更新）
 */
@Data
public class UserDTO {

    /**
     * 用户名（注册时必填）
     */
    @NotBlank(message = "用户名不能为空", groups = Register.class)
    @Size(min = 3, max = 20, message = "用户名长度3-20位", groups = {Register.class, Update.class})
    private String username;

    /**
     * 邮箱（注册/登录时必填）
     */
    @NotBlank(message = "邮箱不能为空", groups = {Register.class, Login.class})
    @Email(message = "邮箱格式不正确", groups = {Register.class, Login.class, Update.class})
    private String email;

    /**
     * 密码（注册/登录时必填）
     */
    @NotBlank(message = "密码不能为空", groups = {Register.class, Login.class})
    @Size(min = 6, max = 20, message = "密码长度6-20位", groups = {Register.class, Login.class, Update.class})
    private String password;

    /**
     * 验证码（注册/登录时必填）
     */
    @NotBlank(message = "验证码不能为空", groups = {Register.class, Login.class})
    private String verifyCode;

    /**
     * API Key（注册时必填）
     */
    @NotBlank(message = "API Key不能为空", groups = Register.class)
    private String apiKey;

    // 验证分组接口
    public interface Register {}
    public interface Login {}
    public interface Update {}
}
