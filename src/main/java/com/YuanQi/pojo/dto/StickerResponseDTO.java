package com.YuanQi.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表情包回复响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StickerResponseDTO {

    /**
     * AI回复的文本内容
     */
    private String text;

    /**
     * 表情包图片URL
     */
    private String stickerUrl;
}
