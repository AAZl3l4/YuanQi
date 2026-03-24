package com.YuanQi.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 对话消息实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message")
public class ChatMessage extends BaseEntity {

    /**
     * 会话ID
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 角色：user-用户 assistant-助手 system-系统
     */
    @TableField("role")
    private String role;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 实际使用的模型
     */
    @TableField("model_used")
    private String modelUsed;

    /**
     * 使用的工具列表（JSON数组）
     */
    @TableField("tools_used")
    private String toolsUsed;

    /**
     * 工具返回结果（JSON格式）
     */
    @TableField("tool_results")
    private String toolResults;

    /**
     * 输入Token数
     */
    @TableField("input_tokens")
    private Integer inputTokens;

    /**
     * 输出Token数
     */
    @TableField("output_tokens")
    private Integer outputTokens;
}
