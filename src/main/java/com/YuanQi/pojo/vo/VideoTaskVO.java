package com.YuanQi.pojo.vo;

import lombok.Data;

/**
 * 视频任务响应VO
 */
@Data
public class VideoTaskVO {

    /**
     * 任务ID（用于查询结果）
     */
    private String taskId;

    /**
     * 任务状态：PROCESSING-处理中, SUCCESS-成功, FAIL-失败
     */
    private String status;

    /**
     * 视频URL（成功时返回）
     */
    private String videoUrl;

    /**
     * 视频封面图URL（成功时返回）
     */
    private String coverImageUrl;

    /**
     * 错误信息（失败时返回）
     */
    private String errorMessage;
}
