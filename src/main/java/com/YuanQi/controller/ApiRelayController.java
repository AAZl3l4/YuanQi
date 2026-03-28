package com.YuanQi.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.YuanQi.pojo.ApiRelayLog;
import com.YuanQi.pojo.dto.ChatDTO;
import com.YuanQi.service.ApiRelayService;
import com.YuanQi.utils.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * API中转控制器
 */
@RestController
@RequiredArgsConstructor
public class ApiRelayController {

    private final ApiRelayService apiRelayService;

    /**
     * API中转调用接口（免登录，API Key验证）
     */
    @PostMapping("/relay/call")
    public Result<String> call(@RequestHeader("X-API-Key") String apiKey, @RequestBody ChatDTO chatDTO) {
        String response = apiRelayService.call(apiKey, chatDTO.getMessage(), chatDTO.getImageUrl());
        return Result.success(response);
    }

    /**
     * 查看自己的调用记录
     */
    @GetMapping("/relay/my/logs")
    public Result<IPage<ApiRelayLog>> myLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        IPage<ApiRelayLog> result = apiRelayService.pageList(page, size, userId);
        return Result.success(result);
    }

    /**
     * 管理员查看调用记录
     */
    @SaCheckRole("admin")
    @GetMapping("/relay/admin/logs")
    public Result<IPage<ApiRelayLog>> adminLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long userId) {
        IPage<ApiRelayLog> result = apiRelayService.pageList(page, size, userId);
        return Result.success(result);
    }
}
