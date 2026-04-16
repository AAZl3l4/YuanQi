package com.YuanQi.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.session.SaTerminalInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import com.YuanQi.mapper.UserMapper;
import com.YuanQi.pojo.User;
import com.YuanQi.pojo.dto.UserDTO;
import com.YuanQi.pojo.vo.OnlineUserVO;
import com.YuanQi.service.UserService;
import com.YuanQi.utils.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    @Value("${yuanqi.default-models.chat}")
    private String defaultChatModel;

    @Value("${yuanqi.default-models.chat-vision}")
    private String defaultChatVisionModel;

    @Value("${yuanqi.default-models.image}")
    private String defaultImageModel;

    @Value("${yuanqi.default-models.video}")
    private String defaultVideoModel;

    private static final String EMAIL_CACHE_PREFIX = "email:code:";

    /**
     * 发送邮箱验证码（异步）
     */
    @Override
    @Async
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
        caffeineCache.put(EMAIL_CACHE_PREFIX+ email, code);

        log.info("已向 {} 发送验证码", email);
    }

    /**
     * 用户注册
     */
    @Override
    public void register(UserDTO userDTO) {
        // 校验验证码
        String cacheCode = caffeineCache.getIfPresent(EMAIL_CACHE_PREFIX + userDTO.getEmail());
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
        user.setApiKey(userDTO.getApiKey());
        user.setRole("user");
        user.setStatus(1);

        // 使用系统默认模型配置
        user.setChatModel(defaultChatModel);
        user.setChatVisionModel(defaultChatVisionModel);
        user.setImageModel(defaultImageModel);
        user.setVideoModel(defaultVideoModel);

        save(user);

        // 清除验证码
        caffeineCache.invalidate(EMAIL_CACHE_PREFIX + userDTO.getEmail());

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

        // 校验密码
        if (!userDTO.getPassword().equals(user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        // 登录并缓存角色（Token自动写入Cookie）
        StpUtil.login(user.getId());
        StpUtil.getSession().set("role", user.getRole());

        log.info("用户 {} 登录成功, 角色: {}", userDTO.getEmail(), user.getRole());
    }

    /**
     * 修改密码
     */
    @Override
    public void changePassword(Long userId, UserDTO userDTO) {
        // 查询用户
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 校验邮件验证码
        String cacheCode = caffeineCache.getIfPresent(EMAIL_CACHE_PREFIX + user.getEmail());
        if (cacheCode == null || !cacheCode.equals(userDTO.getVerifyCode())) {
            throw new BusinessException("验证码错误或已过期");
        }

        // 更新密码
        user.setPassword(userDTO.getPassword());
        updateById(user);

        // 清除验证码
        caffeineCache.invalidate(EMAIL_CACHE_PREFIX + user.getEmail());

        log.info("用户 {} 修改密码成功", user.getEmail());
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
    public User updateUser(User user) {
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

    /**
     * 获取在线用户列表
     */
    @Override
    public List<OnlineUserVO> listOnlineUsers() {
        List<OnlineUserVO> onlineUsers = new ArrayList<>();

        // 获取所有登录的会话ID
        List<String> sessionIds = StpUtil.searchSessionId("", 0, -1, true);
        for (String sessionId : sessionIds) {
            SaSession session = StpUtil.getSessionBySessionId(sessionId);
            if (session == null) continue;

            // 获取登录ID
            Object loginIdObj = session.getLoginId();
            if (loginIdObj == null) continue;

            Long loginId;
            try {
                loginId = Long.parseLong(loginIdObj.toString());
            } catch (NumberFormatException e) {
                continue;
            }

            if (loginId == 0) continue;

            // 获取用户信息
            User user = getById(loginId);
            if (user == null) continue;

            // 获取登录终端信息
            List<SaTerminalInfo> terminalList = StpUtil.getTerminalListByLoginId(loginId);
            for (SaTerminalInfo terminal : terminalList) {
                OnlineUserVO vo = new OnlineUserVO();
                vo.setId(loginId);
                vo.setUsername(user.getUsername());
                vo.setEmail(user.getEmail());
                vo.setAvatar(user.getAvatar());
                vo.setRole(user.getRole());
                vo.setIndex(terminal.getIndex());
                vo.setDeviceType(terminal.getDeviceType());
                vo.setTokenValue(terminal.getTokenValue());
                vo.setCreateTime(new Date(terminal.getCreateTime()));
                vo.setDeviceId(terminal.getDeviceId());
                onlineUsers.add(vo);
            }
        }

        return onlineUsers;
    }

    /**
     * 管理员发送邮件给指定用户
     */
    @Override
    public void sendEmailToUser(Long userId, String subject, String content) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);

        log.info("管理员发送邮件给用户 {}: {}", user.getEmail(), subject);
    }
}
