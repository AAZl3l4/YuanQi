package com.YuanQi.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 视频生成请求DTO
 */
@Data
public class VideoDTO {

    /**
     * 视频描述提示词
     */
    @NotBlank(message = "提示词不能为空")
    private String prompt;

    /**
     * 图片URL（可选，图生视频时使用）
     */
    private String imageUrl;

    /**
     * 视频尺寸（可选）
     * 支持: 1280x720, 720x1280, 1024x1024, 1920x1080, 1080x1920, 2048x1080, 3840x2160
     */
    private String size = "1024x1024";

    /**
     * 输出质量（可选）
     * speed: 速度优先（默认）
     * quality: 质量优先
     */
    private String quality = "speed";

    /**
     * 视频时长 可选（秒），支持5或10，默认5
     * cogvideox-flash模型仅支持5秒时长，默认不需要指定时长
     */
    private Integer duration = 5;

    /**
     * 帧率FPS，支持30或60，默认30
     */
    private Integer fps = 30;

    /**
     * 是否生成AI音效，默认false
     */
    private Boolean withAudio = false;
}
