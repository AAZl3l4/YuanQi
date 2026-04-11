package com.YuanQi.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API中转调用记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("api_relay_log")
public class ApiRelayLog extends BaseEntity {

    /**
     * 调用用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * API Key ID
     */
    @TableField("api_key_id")
    private Long apiKeyId;

    /**
     * 配置ID
     */
    @TableField("config_id")
    private Long configId;

    /**
     * 发送者标识（如QQ号，用于区分不同来源的对话上下文）
     */
    @TableField("sender")
    private String sender;

    /**
     * 使用的知识库ID
     */
    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    /**
     * 输入消息
     */
    @TableField("input_message")
    private String inputMessage;

    /**
     * 图片URL
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 输出消息
     */
    @TableField("output_message")
    private String outputMessage;

    /**
     * 使用的模型
     */
    @TableField("model_used")
    private String modelUsed;

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
