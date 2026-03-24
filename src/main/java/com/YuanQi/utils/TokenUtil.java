package com.YuanQi.utils;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;

import java.util.List;

/**
 * Token估算工具类
 * 使用tiktoken算法估算文本的Token数量
 */
public class TokenUtil {

    private static final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

    /**
     * 估算单条文本的Token数
     * @param text 文本内容
     * @return Token数量
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 使用GPT-4编码（与GLM相近）
        Encoding encoding = registry.getEncodingForModel(ModelType.GPT_4);
        return encoding.countTokens(text);
    }

    /**
     * 估算多条文本的总Token数
     * @param texts 文本列表
     * @return Token总数
     */
    public static int estimateTokens(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return 0;
        }
        return texts.stream().mapToInt(TokenUtil::estimateTokens).sum();
    }
}
