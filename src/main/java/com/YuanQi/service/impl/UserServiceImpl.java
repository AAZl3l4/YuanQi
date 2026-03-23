package com.YuanQi.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import com.YuanQi.mapper.UserMapper;
import com.YuanQi.pojo.User;
import com.YuanQi.pojo.dto.UserDTO;
import com.YuanQi.service.UserService;
import com.YuanQi.utils.BusinessException;
import com.YuanQi.utils.CryptoUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现（包含邮件发送和认证）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JavaMailSender mailSender;
    private final Cache<String, String> caffeineCache;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${yuanqi.email.code-expire}")
    private Long codeExpire;

    @Value("${yuanqi.email.code-length}")
    private Integer codeLength;

    /**
     * 发送邮箱验证码
     */
    @Override
    public void sendEmailCode(String email) {
        // 生成验证码
        String code = RandomUtil.randomNumbers(codeLength);

        // 发送邮件
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("元启AI - 邮箱验证码");
        message.setText("您的验证码是：" + code + "，有效期" + (codeExpire / 60) + "分钟，请勿泄露给他人。");
        mailSender.send(message);

        // 缓存验证码
        caffeineCache.put("email:code:" + email, code);

        log.info("已向 {} 发送验证码", email);
    }

    /**
     * 用户注册
     */
    @Override
    public void register(UserDTO userDTO) {
        // 校验验证码
        String cacheCode = caffeineCache.getIfPresent("email:code:" + userDTO.getEmail());
        if (cacheCode == null || !cacheCode.equals(userDTO.getVerifyCode())) {
            throw new BusinessException("验证码错误或已过期");
        }

        // 检查邮箱是否已注册
        User existUser = getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, userDTO.getEmail()));
        if (existUser != null) {
            throw new BusinessException("邮箱已被注册");
        }

        // 创建用户
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPassword(userDTO.getPassword());
        user.setRole("user");
        user.setStatus(1);

        // 使用系统默认模型配置
        user.setChatModel("glm-4.7-flash");
        user.setChatVisionModel("glm-4.6v-flash");
        user.setImageModel("cogview-3-flash");
        user.setVideoModel("cogvideo-x-flash");

        save(user);

        // 清除验证码
        caffeineCache.invalidate("email:code:" + userDTO.getEmail());

        log.info("用户 {} 注册成功", userDTO.getEmail());
    }

    /**
     * 用户登录
     */
    @Override
    public void login(UserDTO userDTO) {
        // 查询用户
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, userDTO.getEmail()));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 检查状态
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        // 校验密码（加密后对比）
        String encryptedPassword = CryptoUtil.encrypt(userDTO.getPassword());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        // 登录并缓存角色（Token自动写入Cookie）
        StpUtil.login(user.getId());
        StpUtil.getSession().set("role", user.getRole());

        log.info("用户 {} 登录成功, 角色: {}", userDTO.getEmail(), user.getRole());
    }

    /**
     * 获取当前登录用户信息
     */
    @Override
    public User getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 清除密码
        user.setPassword(null);
        return user;
    }

    /**
     * 更新用户信息
     */
    @Override
    public User updateUserInfo(User user) {
        Long userId = StpUtil.getLoginIdAsLong();

        User existUser = getById(userId);
        if (existUser == null) {
            throw new BusinessException("用户不存在");
        }

        // 设置用户ID，确保更新的是当前登录用户
        user.setId(userId);
        // 不允许修改邮箱
        user.setEmail(null);

        updateById(user);
        user.setPassword(null);
        return user;
    }

    /**
     * 管理员修改用户信息
     */
    @Override
    public void adminUpdateUser(User user) {
        User existUser = getById(user.getId());
        if (existUser == null) {
            throw new BusinessException("用户不存在");
        }

        updateById(user);
    }
}
