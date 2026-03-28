package com.YuanQi.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.YuanQi.pojo.ApiKey;
import com.YuanQi.service.ApiKeyService;
import com.YuanQi.utils.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * API Key控制器
 */
@RestController
@RequestMapping("/api-key")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * 创建API Key
     */
    @PostMapping("/create")
    public Result<ApiKey> create(@RequestBody @Validated ApiKey apiKey) {
        apiKey.setUserId(StpUtil.getLoginIdAsLong());
        ApiKey created = apiKeyService.create(apiKey);
        return Result.success(created);
    }

    /**
     * 查看自己的API Key列表
     */
    @GetMapping("/my")
    public Result<IPage<ApiKey>> listMy(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        IPage<ApiKey> result = apiKeyService.pageList(page, size, userId);
        return Result.success(result);
    }

    /**
     * 删除自己的API Key
     */
    @DeleteMapping("/my/{id}")
    public Result<Void> deleteMy(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        apiKeyService.deleteMyKey(id, userId);
        return Result.success();
    }

    /**
     * 管理员查看全部API Key
     */
    @SaCheckRole("admin")
    @GetMapping("/admin/list")
    public Result<IPage<ApiKey>> adminList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long userId) {
        IPage<ApiKey> result = apiKeyService.pageList(page, size, userId);
        return Result.success(result);
    }

    /**
     * 管理员删除任意API Key
     */
    @SaCheckRole("admin")
    @DeleteMapping("/admin/{id}")
    public Result<Void> adminDelete(@PathVariable Long id) {
        apiKeyService.removeById(id);
        return Result.success();
    }
}
