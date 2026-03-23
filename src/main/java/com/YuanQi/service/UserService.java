package com.YuanQi.service;

import com.YuanQi.pojo.User;
import com.YuanQi.pojo.dto.UserDTO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户服务（包含邮件发送和认证）
 */
public interface UserService extends IService<User> {

    /**
     * 发送邮箱验证码
     */
    void sendEmailCode(String email);

    /**
     * 用户注册
     */
    void register(UserDTO userDTO);

    /**
     * 用户登录
     */
    void login(UserDTO userDTO);

    /**
     * 获取当前登录用户信息
     */
    User getCurrentUser();

    /**
     * 更新用户信息
     */
    User updateUserInfo(User user);

    /**
     * 管理员修改用户信息
     */
    void adminUpdateUser(User user);
}
