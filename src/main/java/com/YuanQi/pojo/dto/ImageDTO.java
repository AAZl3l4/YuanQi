package com.YuanQi.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 生图请求DTO
 */
@Data
public class ImageDTO {

    /**
     * 提示词
     */
    @NotBlank(message = "提示词不能为空")
    private String prompt;

    /**
     * 图片尺寸（可选）
     * cogview-3-flash推荐: 1024x1024(默认), 768x1344, 864x1152, 1344x768, 1152x864, 1440x720, 720x1440
     */
    private String size = "1024x1024";
}
