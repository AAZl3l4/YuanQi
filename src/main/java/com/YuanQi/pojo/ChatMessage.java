package com.YuanQi.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 对话消息实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "chat_message", autoResultMap = true)
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
     * 智能体ID
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * 图片地址
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 文档地址
     */
    @TableField("document_url")
    private String documentUrl;

    /**
     * 知识库ID
     */
    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    /**
     * MCP工具ID列表
     */
    @TableField(value = "tool_ids", typeHandler = JacksonTypeHandler.class)
    private List<Long> toolIds;

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
