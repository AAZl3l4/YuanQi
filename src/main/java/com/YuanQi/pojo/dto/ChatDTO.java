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
     * 上下文轮数（1轮=2条消息：用户+AI）
     * 最多20轮，默认10轮
     */
    @Min(value = 1, message = "上下文轮数最少1轮")
    @Max(value = 20, message = "上下文轮数最多20轮")
    private Integer contextRounds = 10;
}
