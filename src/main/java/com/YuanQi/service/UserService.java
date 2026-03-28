package com.YuanQi.service;

import com.YuanQi.pojo.User;
import com.YuanQi.pojo.dto.UserDTO;
import com.YuanQi.pojo.vo.OnlineUserVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

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
    User updateUser(User user);

    /**
     * 管理员修改用户信息
     */
    void adminUpdateUser(User user);

    /**
     * 获取在线用户列表
     */
    List<OnlineUserVO> listOnlineUsers();

    /**
     * 管理员发送邮件给指定用户
     */
    void sendEmailToUser(Long userId, String subject, String content);
}
