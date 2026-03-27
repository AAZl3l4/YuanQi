package com.YuanQi.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.YuanQi.pojo.McpTool;
import com.YuanQi.service.McpToolService;
import com.YuanQi.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MCP工具控制器
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpToolController {

    private final McpToolService mcpToolService;

    /**
     * 获取启用的工具列表（用户可用）
     */
    @GetMapping("/tools")
    public Result<List<McpTool>> getEnabledTools() {
        List<McpTool> tools = mcpToolService.getEnabledTools();
        return Result.success(tools);
    }

    /**
     * 获取所有工具列表（管理员）
     */
    @SaCheckRole("admin")
    @GetMapping("/list")
    public Result<List<McpTool>> getAllTools() {
        List<McpTool> tools = mcpToolService.list();
        return Result.success(tools);
    }

    /**
     * 添加新工具（管理员）
     */
    @SaCheckRole("admin")
    @PostMapping
    public Result<McpTool> addTool(@RequestBody @Validated McpTool mcpTool) {
        McpTool tool = mcpToolService.addTool(mcpTool);
        return Result.success(tool);
    }

    /**
     * 更新工具状态（管理员）
     */
    @SaCheckRole("admin")
    @PutMapping("/{id}")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer enabled) {
        mcpToolService.updateStatus(id, enabled);
        return Result.success();
    }
}
