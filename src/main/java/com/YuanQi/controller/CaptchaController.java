package com.YuanQi.controller;

import com.YuanQi.service.CaptchaService;
import com.YuanQi.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 验证码控制器
 */
@RestController
@RequestMapping("/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    /**
     * 获取验证码
     */
    @GetMapping("/get")
    public Result<Map<String, String>> getCaptcha() {
        // 生成唯一标识
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        
        // 生成验证码图片
        BufferedImage image = captchaService.generateCaptcha(captchaId);
        
        // 转换为Base64
        String base64Image = imageToBase64(image);
        
        Map<String, String> result = new HashMap<>();
        result.put("captchaId", captchaId);
        result.put("captchaImage", "data:image/png;base64," + base64Image);
        
        return Result.success(result);
    }

    /**
     * 将图片转换为Base64字符串
     */
    private String imageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("验证码图片生成失败", e);
        }
    }
}
