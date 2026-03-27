package com.YuanQi.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MCP工具配置实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mcp_tool")
public class McpTool extends BaseEntity {

    /**
     * 工具名称（方法名）
     */
    @NotBlank(message = "工具名称不能为空")
    @TableField("name")
    private String name;

    /**
     * 工具描述
     */
    @NotBlank(message = "工具描述不能为空")
    @TableField("description")
    private String description;

    /**
     * 是否启用：0-禁用 1-启用
     */
    @TableField("enabled")
    private Integer enabled;

    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
