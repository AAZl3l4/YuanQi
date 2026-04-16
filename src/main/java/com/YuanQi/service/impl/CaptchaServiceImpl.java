package com.YuanQi.service.impl;

import com.YuanQi.service.CaptchaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图片验证码服务实现
 */
@Slf4j
@Service
public class CaptchaServiceImpl implements CaptchaService {

    // 存储验证码答案，key为captchaId，value为答案
    private final ConcurrentHashMap<String, CaptchaData> captchaStore = new ConcurrentHashMap<>();
    
    // 验证码有效期（5分钟）
    private static final long EXPIRE_TIME = 5 * 60 * 1000;

    private static final Random random = new Random();

    @Override
    public BufferedImage generateCaptcha(String captchaId) {
        // 生成算术表达式
        String[] operators = {"+", "-", "×", "÷"};
        String operator = operators[random.nextInt(4)];
        
        int num1, num2, answer;
        String expression;
        
        // 根据运算符生成数字和答案
        switch (operator) {
            case "+":
                num1 = random.nextInt(50) + 1;
                num2 = random.nextInt(50) + 1;
                answer = num1 + num2;
                expression = num1 + " + " + num2 + " = ?";
                break;
            case "-":
                num1 = random.nextInt(50) + 10;
                num2 = random.nextInt(num1);
                answer = num1 - num2;
                expression = num1 + " - " + num2 + " = ?";
                break;
            case "×":
                num1 = random.nextInt(10) + 1;
                num2 = random.nextInt(10) + 1;
                answer = num1 * num2;
                expression = num1 + " × " + num2 + " = ?";
                break;
            case "÷":
                num2 = random.nextInt(9) + 1;
                answer = random.nextInt(10) + 1;
                num1 = num2 * answer;
                expression = num1 + " ÷ " + num2 + " = ?";
                break;
            default:
                num1 = random.nextInt(50) + 1;
                num2 = random.nextInt(50) + 1;
                answer = num1 + num2;
                expression = num1 + " + " + num2 + " = ?";
        }
        
        // 存储答案
        captchaStore.put(captchaId, new CaptchaData(String.valueOf(answer), System.currentTimeMillis()));
        
        // 生成图片
        return createCaptchaImage(expression);
    }

    @Override
    public boolean verifyCaptcha(String captchaId, String userInput) {
        if (captchaId == null || userInput == null) {
            return false;
        }
        
        CaptchaData data = captchaStore.get(captchaId);
        if (data == null) {
            return false;
        }
        
        // 检查是否过期
        if (System.currentTimeMillis() - data.createTime > EXPIRE_TIME) {
            captchaStore.remove(captchaId);
            return false;
        }
        
        // 验证后删除（一次性使用）
        captchaStore.remove(captchaId);
        
        return data.answer.equals(userInput.trim());
    }

    /**
     * 创建验证码图片
     */
    private BufferedImage createCaptchaImage(String expression) {
        int width = 160;
        int height = 50;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        // 设置抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 背景色
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, width, height);
        
        // 绘制干扰线
        for (int i = 0; i < 8; i++) {
            g.setColor(new Color(random.nextInt(150), random.nextInt(150), random.nextInt(150)));
            g.drawLine(random.nextInt(width), random.nextInt(height), 
                       random.nextInt(width), random.nextInt(height));
        }
        
        // 绘制干扰点
        for (int i = 0; i < 50; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            g.fillOval(random.nextInt(width), random.nextInt(height), 2, 2);
        }
        
        // 绘制文字
        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.setColor(new Color(30, 30, 30));
        
        // 计算文字位置（居中）
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(expression);
        int x = (width - textWidth) / 2;
        int y = (height - fm.getHeight()) / 2 + fm.getAscent();
        
        // 绘制表达式（每个字符稍微偏移，增加识别难度）
        char[] chars = expression.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            // 随机颜色
            g.setColor(new Color(30 + random.nextInt(60), 30 + random.nextInt(60), 30 + random.nextInt(60)));
            // 随机偏移
            int offsetY = random.nextInt(10) - 5;
            g.drawString(String.valueOf(chars[i]), x, y + offsetY);
            x += fm.charWidth(chars[i]);
        }
        
        g.dispose();
        return image;
    }

    /**
     * 验证码数据
     */
    private static class CaptchaData {
        String answer;
        long createTime;

        CaptchaData(String answer, long createTime) {
            this.answer = answer;
            this.createTime = createTime;
        }
    }
}
