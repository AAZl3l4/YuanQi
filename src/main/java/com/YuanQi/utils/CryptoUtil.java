package com.YuanQi.utils;

import cn.hutool.crypto.symmetric.AES;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 加密工具类（AES加密）
 */
@Component
public class CryptoUtil {

    private static String staticSecretKey;

    @Value("${yuanqi.crypto.secret-key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        staticSecretKey = secretKey;
    }

    /**
     * 获取AES实例
     */
    private static AES getAES() {
        // 使用密钥前16位作为AES密钥
        byte[] keyBytes = staticSecretKey.getBytes(StandardCharsets.UTF_8);
        byte[] key16 = new byte[16];
        System.arraycopy(keyBytes, 0, key16, 0, Math.min(keyBytes.length, 16));
        return new AES(key16);
    }

    /**
     * 加密
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            return getAES().encryptBase64(plainText);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密
     */
    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            return getAES().decryptStr(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }
}
