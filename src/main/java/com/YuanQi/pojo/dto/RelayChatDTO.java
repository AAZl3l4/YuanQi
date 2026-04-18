package com.YuanQi.pojo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 中转聊天请求DTO
 */
@Data
public class RelayChatDTO {

    /**
     * 消息内容（与imageUrl至少填一项）
     */
    private String message;

    /**
     * 图片URL（带图时使用视觉模型）
     */
    private String imageUrl;

    /**
     * 发送者标识（如QQ号，用于区分不同来源的对话上下文）
     */
    private String sender;

    /**
     * 上下文轮数（1轮=2条消息：用户+AI）
     * 最多20轮，默认0轮（不启用上下文）
     */
    @Min(value = 0, message = "上下文轮数不能为负数")
    @Max(value = 20, message = "上下文轮数最多20轮")
    private Integer contextRounds = 0;

    /**
     * 是否使用知识库（仅当API Key绑定了知识库时生效）
     * 默认false（不使用）
     */
    private Boolean useKnowledgeBase = false;

    /**
     * 是否启用联网搜索
     * 默认false（不启用）
     */
    private Boolean enableWebSearch = false;
}
