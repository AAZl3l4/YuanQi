package com.YuanQi.service.impl;

import com.YuanQi.mapper.McpToolMapper;
import com.YuanQi.pojo.McpTool;
import com.YuanQi.service.McpToolService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP工具服务实现
 */
@Slf4j
@Service
public class McpToolServiceImpl extends ServiceImpl<McpToolMapper, McpTool> implements McpToolService {

    /**
     * 获取启用的工具列表
     */
    @Override
    public List<McpTool> getEnabledTools() {
        return list(new LambdaQueryWrapper<McpTool>()
                .eq(McpTool::getEnabled, 1)
                .orderByAsc(McpTool::getSortOrder));
    }

    /**
     * 分页获取所有工具列表（管理员）
     */
    @Override
    public IPage<McpTool> pageList(Integer page, Integer size) {
        Page<McpTool> pageParam = new Page<>(page, size);
        return page(pageParam, new LambdaQueryWrapper<McpTool>()
                .orderByAsc(McpTool::getSortOrder));
    }

    /**
     * 更新工具状态
     */
    @Override
    public void updateStatus(Long id, Integer enabled) {
        McpTool tool = getById(id);
        if (tool != null) {
            tool.setEnabled(enabled);
            updateById(tool);
            log.info("更新工具状态: {}, enabled={}", tool.getName(), enabled);
        }
    }

    /**
     * 添加新工具
     */
    @Override
    public McpTool addTool(McpTool mcpTool) {
        mcpTool.setEnabled(1);
        save(mcpTool);
        log.info("添加新工具: {}", mcpTool.getName());
        return mcpTool;
    }
}
