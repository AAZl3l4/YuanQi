package com.YuanQi.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.YuanQi.pojo.User;
import com.YuanQi.pojo.dto.UserDTO;
import com.YuanQi.pojo.vo.OnlineUserVO;
import com.YuanQi.service.UserService;
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

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/send-code")
    public Result<Void> sendEmailCode(@RequestParam String email) {
        userService.sendEmailCode(email);
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
     * 用户登录（Token自动写入Cookie）
     */
    @PostMapping("/login")
    public Result<Void> login(@RequestBody @Validated(UserDTO.Login.class) UserDTO userDTO) {
        userService.login(userDTO);
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
