package com.YuanQi.service;

import com.YuanQi.pojo.McpTool;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * MCP工具服务接口
 */
public interface McpToolService extends IService<McpTool> {

    /**
     * 获取启用的工具列表
     */
    List<McpTool> getEnabledTools();

    /**
     * 更新工具状态
     */
    void updateStatus(Long id, Integer enabled);

    /**
     * 添加新工具（管理员添加）
     */
    McpTool addTool(McpTool mcpTool);
}
