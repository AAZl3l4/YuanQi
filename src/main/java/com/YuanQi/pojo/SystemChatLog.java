package com.YuanQi.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统问答日志实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_chat_log")
public class SystemChatLog extends BaseEntity {

    /**
     * 管理员ID
     */
    @TableField("admin_id")
    private Long adminId;

    /**
     * 角色：user-用户 assistant-助手
     */
    @TableField("role")
    private String role;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 图片URL（带图提问时）
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 使用的模型
     */
    @TableField("model_used")
    private String modelUsed;

    /**
     * 输入Token
     */
    @TableField("input_tokens")
    private Integer inputTokens;

    /**
     * 输出Token
     */
    @TableField("output_tokens")
    private Integer outputTokens;
}
