package com.YuanQi.pojo.vo;

import lombok.Data;

/**
 * 用量统计VO
 */
@Data
public class UsageVO {

    /**
     * 聊天调用次数
     */
    private Long chatCount;

    /**
     * 聊天输入Token数
     */
    private Long inputTokens;

    /**
     * 聊天输出Token数
     */
    private Long outputTokens;

    /**
     * 生图次数
     */
    private Long imageCount;

    /**
     * 生视频次数
     */
    private Long videoCount;

    /**
     * API中转调用次数
     */
    private Long relayCount;

    /**
     * 中转输入Token数
     */
    private Long relayInputTokens;

    /**
     * 中转输出Token数
     */
    private Long relayOutputTokens;
}
