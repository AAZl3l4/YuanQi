package com.YuanQi.configuration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置类
 */
@Configuration
public class SpringAiConfig {

    @Value("${spring.ai.zhipuai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.zhipuai.chat.options.temperature}")
    private Double temperature;

    /**
     * 根据用户API Key和模型创建 ChatClient
     */
    public ChatClient createChatClient(String apiKey, String model) {
        ZhiPuAiApi zhiPuAiApi = new ZhiPuAiApi(baseUrl, apiKey);
        // 创建模型选项，设置模型名称
        ZhiPuAiChatOptions options = ZhiPuAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build();

        ZhiPuAiChatModel chatModel = new ZhiPuAiChatModel(zhiPuAiApi, options);
        return ChatClient.create(chatModel);
    }
}
