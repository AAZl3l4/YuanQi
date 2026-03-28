package com.YuanQi.pojo.vo;

import com.YuanQi.pojo.ChatSession;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会话创建结果VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SessionVO extends ChatSession {

    /**
     * 智能体名称
     */
    private String agentName;

    /**
     * 智能体头像
     */
    private String agentAvatar;

    /**
     * 智能体描述
     */
    private String agentDescription;

    /**
     * 智能体欢迎语
     */
    private String welcomeMessage;
}
