package com.YuanQi.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 智能体实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "agent", autoResultMap = true)
public class Agent extends BaseEntity {

    /**
     * 用户ID（NULL表示系统预设）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 创建者用户名（展示用，非数据库字段）
     */
    @TableField(exist = false)
    private String username;

    /**
     * 智能体名称
     */
    @NotBlank(message = "智能体名称不能为空")
    @TableField("name")
    private String name;

    /**
     * 智能体描述
     */
    @TableField("description")
    private String description;

    /**
     * 智能体头像URL
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 系统提示词
     */
    @NotBlank(message = "系统提示词不能为空")
    @TableField("system_prompt")
    private String systemPrompt;

    /**
     * 欢迎语
     */
    @TableField("welcome_message")
    private String welcomeMessage;

    /**
     * 关联知识库ID
     */
    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    /**
     * 工具ID列表
     */
    @TableField(value = "tool_ids", typeHandler = JacksonTypeHandler.class)
    private List<Long> toolIds;

    /**
     * 是否公开：0-私有 1-公开
     */
    @TableField("is_public")
    private Integer isPublic;
    /**
     * 排序序号
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
