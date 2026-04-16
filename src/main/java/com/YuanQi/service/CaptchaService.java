package com.YuanQi.service;

import java.awt.image.BufferedImage;

/**
 * 图片验证码服务
 */
public interface CaptchaService {

    /**
     * 生成验证码图片
     * @param captchaId 验证码唯一标识
     * @return 验证码图片
     */
    BufferedImage generateCaptcha(String captchaId);

    /**
     * 验证验证码
     * @param captchaId 验证码唯一标识
     * @param userInput 用户输入的答案
     * @return 是否正确
     */
    boolean verifyCaptcha(String captchaId, String userInput);
}
