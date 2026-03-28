package com.YuanQi.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.YuanQi.pojo.Agent;
import com.YuanQi.service.AgentService;
import com.YuanQi.utils.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 智能体控制器
 */
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * 创建智能体
     */
    @PostMapping("/create")
    public Result<Agent> create(@RequestBody @Validated Agent agent) {
        Agent created = agentService.create(agent);
        return Result.success(created);
    }

    /**
     * 更新智能体
     */
    @PutMapping("/update")
    public Result<Agent> update(@RequestBody Agent agent) {
        Agent updated = agentService.update(agent);
        return Result.success(updated);
    }

    /**
     * 删除智能体
     */
    @DeleteMapping("/my/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        agentService.delete(id);
        return Result.success();
    }

    /**
     * 获取智能体详情
     */
    @GetMapping("/{id}")
    public Result<Agent> getById(@PathVariable Long id) {
        Agent agent = agentService.getById(id);
        return Result.success(agent);
    }

    /**
     * 获取智能体列表
     * @param onlyMine 只看自己的（默认false，查自己的+公开的）
     */
    @GetMapping("/list")
    public Result<IPage<Agent>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Boolean onlyMine) {
        Long userId = StpUtil.getLoginIdAsLong();
        IPage<Agent> result = agentService.pageList(page, size, userId, onlyMine);
        return Result.success(result);
    }

    /**
     * 管理员查询智能体列表
     * @param userId 指定用户ID（不传查全部）
     */
    @SaCheckRole("admin")
    @GetMapping("/admin/list")
    public Result<IPage<Agent>> adminList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long userId) {
        IPage<Agent> result = agentService.pageList(page, size, userId, true);
        return Result.success(result);
    }

    /**
     * 管理员删除任意智能体
     */
    @SaCheckRole("admin")
    @DeleteMapping("/admin/{id}")
    public Result<Void> adminDelete(@PathVariable Long id) {
        agentService.removeById(id);
        return Result.success();
    }
}
