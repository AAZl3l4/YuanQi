package com.YuanQi.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.YuanQi.pojo.ApiRelayConfig;
import com.YuanQi.service.ApiRelayConfigService;
import com.YuanQi.utils.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * API中转配置控制器
 */
@RestController
@RequestMapping("/api-relay/config")
@RequiredArgsConstructor
public class ApiRelayConfigController {

    private final ApiRelayConfigService apiRelayConfigService;

    /**
     * 创建配置
     */
    @PostMapping("/create")
    public Result<ApiRelayConfig> create(@RequestBody @Validated ApiRelayConfig config) {
        config.setUserId(StpUtil.getLoginIdAsLong());
        ApiRelayConfig created = apiRelayConfigService.create(config);
        return Result.success(created);
    }

    /**
     * 更新配置
     */
    @PutMapping("/update")
    public Result<ApiRelayConfig> update(@RequestBody ApiRelayConfig config) {
        ApiRelayConfig updated = apiRelayConfigService.update(config);
        return Result.success(updated);
    }

    /**
     * 删除自己的配置
     */
    @DeleteMapping("/my/{id}")
    public Result<Void> deleteMy(@PathVariable Long id) {
        apiRelayConfigService.deleteMyConfig(id);
        return Result.success();
    }

    /**
     * 获取配置详情
     */
    @GetMapping("/{id}")
    public Result<ApiRelayConfig> getById(@PathVariable Long id) {
        ApiRelayConfig config = apiRelayConfigService.getConfigById(id);
        return Result.success(config);
    }

    /**
     * 查询配置列表（自己的+公开的）
     * @param onlyMine 只看自己的（默认false）
     */
    @GetMapping("/list")
    public Result<IPage<ApiRelayConfig>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Boolean onlyMine,
            @RequestParam(required = false) Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        IPage<ApiRelayConfig> result = apiRelayConfigService.pageList(page, size, userId, onlyMine, id);
        return Result.success(result);
    }

    /**
     * 管理员查询配置列表
     * @param userId 指定用户ID（不传查全部）
     */
    @SaCheckRole("admin")
    @GetMapping("/admin/list")
    public Result<IPage<ApiRelayConfig>> adminList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long id) {
        IPage<ApiRelayConfig> result = apiRelayConfigService.pageList(page, size, userId, true, id);
        return Result.success(result);
    }

    /**
     * 管理员删除任意配置
     */
    @SaCheckRole("admin")
    @DeleteMapping("/admin/{id}")
    public Result<Void> adminDelete(@PathVariable Long id) {
        apiRelayConfigService.removeById(id);
        return Result.success();
    }
}
