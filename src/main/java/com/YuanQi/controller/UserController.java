package com.YuanQi.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.YuanQi.pojo.User;
import com.YuanQi.pojo.dto.UserDTO;
import com.YuanQi.pojo.vo.OnlineUserVO;
import com.YuanQi.service.CaptchaService;
import com.YuanQi.service.UserService;
import com.YuanQi.utils.BusinessException;
import com.YuanQi.utils.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CaptchaService captchaService;

    /**
     * 发送邮箱验证码（需要图片验证码验证）
     */
    @PostMapping("/send-code")
    public Result<Void> sendEmailCode(@RequestBody @Validated(UserDTO.SendCode.class) UserDTO userDTO) {
        // 先验证图片验证码
        if (!captchaService.verifyCaptcha(userDTO.getCaptchaId(), userDTO.getCaptchaAnswer())) {
            throw new BusinessException("验证码错误");
        }
        userService.sendEmailCode(userDTO.getEmail());
        return Result.success();
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Validated(UserDTO.Register.class) UserDTO userDTO) {
        userService.register(userDTO);
        return Result.success();
    }

    /**
     * 用户登录（图片验证码登录）
     */
    @PostMapping("/login")
    public Result<Void> login(@RequestBody @Validated(UserDTO.Login.class) UserDTO userDTO) {
        // 验证图片验证码
        if (!captchaService.verifyCaptcha(userDTO.getCaptchaId(), userDTO.getCaptchaAnswer())) {
            throw new BusinessException("验证码错误");
        }
        userService.login(userDTO);
        return Result.success();
    }

    /**
     * 修改密码（需要登录，通过用户ID修改）
     */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody @Validated(UserDTO.ChangePassword.class) UserDTO userDTO) {
        // 从登录态获取用户ID
        Long userId = StpUtil.getLoginIdAsLong();
        userService.changePassword(userId, userDTO);
        return Result.success();
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<User> getCurrentUser() {
        User user = userService.getCurrentUser();
        return Result.success(user);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/updata")
    public Result<User> updateUser(@RequestBody User user) {
        User updatedUser = userService.updateUser(user);
        return Result.success(updatedUser);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }

    /**
     * 获取所有用户列表（管理员）
     */
    @SaCheckRole("admin")
    @GetMapping("/list")
    public Result<IPage<User>> listUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<User> pageParam = new Page<>(page, size);
        IPage<User> userPage = userService.page(pageParam);
        // 清除密码
        userPage.getRecords().forEach(user -> {
                user.setPassword(null);
                user.setApiKey(null);
            }
        );
        return Result.success(userPage);
    }

    /**
     * 管理员修改用户信息
     */
    @SaCheckRole("admin")
    @PutMapping("/{userId}")
    public Result<Void> adminUpdateUser(@PathVariable Long userId, @RequestBody User user) {
        user.setId(userId);
        userService.adminUpdateUser(user);
        return Result.success();
    }

    /**
     * 管理员删除用户
     */
    @SaCheckRole("admin")
    @DeleteMapping("/{userId}")
    public Result<Void> adminDeleteUser(@PathVariable Long userId) {
        userService.removeById(userId);
        return Result.success();
    }

    /**
     * 获取在线用户列表（管理员）
     */
    @SaCheckRole("admin")
    @GetMapping("/online")
    public Result<List<OnlineUserVO>> listOnlineUsers() {
        List<OnlineUserVO> onlineUsers = userService.listOnlineUsers();
        return Result.success(onlineUsers);
    }

    /**
     * 踢出指定用户（管理员）
     */
    @SaCheckRole("admin")
    @PostMapping("/kickout/{userId}")
    public Result<Void> kickoutUser(@PathVariable Long userId) {
        // 强制下线该用户
        StpUtil.logout(userId);
        return Result.success();
    }

    /**
     * 发送邮件给指定用户（管理员）
     */
    @SaCheckRole("admin")
    @PostMapping("/send-email/{userId}")
    public Result<Void> sendEmailToUser(
            @PathVariable Long userId,
            @RequestParam String subject,
            @RequestParam String content) {
        userService.sendEmailToUser(userId, subject, content);
        return Result.success();
    }
}
