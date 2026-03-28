package com.YuanQi.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会话实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_session")
public class ChatSession extends BaseEntity {

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 会话唯一标识UUID
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 会话标题
     */
    @TableField("title")
    private String title;

    /**
     * 智能体ID（NULL表示普通会话）
     */
    @TableField("agent_id")
    private Long agentId;
}
