package com.YuanQi.pojo.dto;

import lombok.Data;

/**
 * 系统问答请求DTO
 */
@Data
public class SystemChatDTO {

    /**
     * 消息内容
     */
    private String message;

    /**
     * 图片URL（带图提问时使用视觉模型）
     */
    private String imageUrl;

    /**
     * 上下文轮数（默认10轮）
     */
    private Integer contextRounds = 10;
}
