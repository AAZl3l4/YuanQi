package com.YuanQi.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.YuanQi.pojo.vo.UsageVO;
import com.YuanQi.service.UsageService;
import com.YuanQi.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 用量统计控制器
 */
@RestController
@RequestMapping("/usage")
@RequiredArgsConstructor
public class UsageController {

    private final UsageService usageService;

    /**
     * 查询用户自己的用量统计
     */
    @GetMapping("/my")
    public Result<UsageVO> getMyUsage(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        UsageVO usage = usageService.getUsage(StpUtil.getLoginIdAsLong(), startDate, endDate);
        return Result.success(usage);
    }

    /**
     * 管理员查询用量统计（userId为null查全部，有值查指定用户）
     */
    @GetMapping("/list")
    @SaCheckRole("admin")
    public Result<UsageVO> getUsage(
            @RequestParam(required = false) Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        UsageVO usage = usageService.getUsage(userId, startDate, endDate);
        return Result.success(usage);
    }
}
