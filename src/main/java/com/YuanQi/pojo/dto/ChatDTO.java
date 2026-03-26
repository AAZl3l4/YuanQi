package com.YuanQi.pojo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 聊天请求DTO
 */
@Data
public class ChatDTO {

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String message;

    /**
     * 图片URL（带图时使用视觉模型）
     */
    private String imageUrl;

    /**
     * 文档URL（携带文档聊天）
     */
    private String documentUrl;

    /**
     * 知识库ID（基于知识库聊天）
     */
    private Long knowledgeBaseId;

    /**
     * 上下文轮数（1轮=2条消息：用户+AI）
     * 最多20轮，默认10轮
     */
    @Min(value = 0, message = "上下文轮数不能为负数")
    @Max(value = 20, message = "上下文轮数最多20轮")
    private Integer contextRounds = 10;
}
