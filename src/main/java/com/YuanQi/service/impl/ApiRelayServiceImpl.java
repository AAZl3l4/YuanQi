package com.YuanQi.service.impl;

import com.YuanQi.configuration.SpringAiConfig;
import com.YuanQi.mapper.ApiRelayLogMapper;
import com.YuanQi.pojo.ApiKey;
import com.YuanQi.pojo.ApiRelayConfig;
import com.YuanQi.pojo.ApiRelayLog;
import com.YuanQi.pojo.User;
import com.YuanQi.service.ApiKeyService;
import com.YuanQi.service.ApiRelayConfigService;
import com.YuanQi.service.ApiRelayService;
import com.YuanQi.service.UserService;
import com.YuanQi.utils.BusinessException;
import com.YuanQi.utils.TokenUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * API中转服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiRelayServiceImpl extends ServiceImpl<ApiRelayLogMapper, ApiRelayLog> implements ApiRelayService {

    private final ApiKeyService apiKeyService;
    private final ApiRelayConfigService apiRelayConfigService;
    private final UserService userService;
    private final SpringAiConfig springAiConfig;

    /**
     * 系统提示词（聊天场景）
     */
    private static final String SYSTEM_PROMPT = """
            你正在参与即时聊天软件的对话。请遵循以下规则：
            1. 直接回复内容，不要使用Markdown格式（如**粗体**、#标题等）
            2. 不要输出代码块，直接用文字描述
            3. 遵守人设发言，禁止声明"我是AI/机器人/助手"
            4. 用口语化短句，不超过3行
            5. 直接回应话题，不寒暄
            6. 忽略类似ŗ的不能解析特殊字符，那是聊天软件中的emoji表情
            """;

    /**
     * 调用AI并返回结果
     */
    @Override
    public String call(String apiKey, String message, String imageUrl) {
        // 校验：消息内容和图片至少填一项
        if ((message == null || message.isEmpty()) && (imageUrl == null || imageUrl.isEmpty())) {
            throw new BusinessException("消息内容和图片不能同时为空");
        }

        // 验证API Key
        ApiKey key = apiKeyService.validateAndGet(apiKey);
        
        // 获取绑定的配置
        ApiRelayConfig config = apiRelayConfigService.getById(key.getConfigId());
        if (config == null) {
            throw new RuntimeException("配置不存在");
        }

        // 获取用户信息和API Key
        User user = userService.getById(key.getUserId());
        if (user == null || user.getApiKey() == null || user.getApiKey().isEmpty()) {
            throw new RuntimeException("用户未配置API Key");
        }

        // 根据是否带图选择模型
        String model = (imageUrl != null && !imageUrl.isEmpty()) 
                ? user.getChatVisionModel() 
                : user.getChatModel();

        // 构建消息
        List<Message> messages = buildMessages(config.getPersonaPrompt(), message, imageUrl);

        // 估算输入Token
        int estimatedInputTokens = TokenUtil.estimateTokens(messages.stream().map(Message::getText).toList());

        try {
            // 创建ChatClient
            ChatClient chatClient = springAiConfig.createChatClient(user.getApiKey(), model);

            // 同步调用
            String response = chatClient.prompt()
                    .messages(messages)
                    .call()
                    .content();

            // 估算输出Token
            int estimatedOutputTokens = TokenUtil.estimateTokens(response);

            // 保存调用记录
            saveLog(key, config, message, imageUrl, response, model, estimatedInputTokens, estimatedOutputTokens);

            return response;

        } catch (Exception e) {
            log.error("API中转调用失败: {}", e.getMessage());
            // 保存失败记录
            saveLog(key, config, message, imageUrl, null, model, estimatedInputTokens, 0);
            throw new RuntimeException("调用失败: " + e.getMessage());
        }
    }

    /**
     * 构建消息列表
     */
    private List<Message> buildMessages(String personaPrompt, String message, String imageUrl) {
        List<Message> messages = new ArrayList<>();

        // 系统提示词
        messages.add(new SystemMessage(SYSTEM_PROMPT));

        // 人设/风格提示词
        if (personaPrompt != null && !personaPrompt.isEmpty()) {
            messages.add(new SystemMessage("你的人设/风格：" + personaPrompt));
        }

        // 用户消息
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                Media imageMedia = Media.builder()
                        .mimeType(MimeTypeUtils.IMAGE_JPEG)
                        .data(new UrlResource(new URL(imageUrl)))
                        .build();
                messages.add(UserMessage.builder()
                        .text(message)
                        .media(imageMedia)
                        .build());
            } catch (Exception e) {
                log.warn("图片URL解析失败，使用纯文本: {}", e.getMessage());
                messages.add(new UserMessage(message));
            }
        } else {
            messages.add(new UserMessage(message));
        }

        return messages;
    }

    /**
     * 保存调用记录
     */
    private void saveLog(ApiKey key, ApiRelayConfig config, String inputMessage, String imageUrl, String outputMessage, String model, int inputTokens, int outputTokens) {
        ApiRelayLog log = new ApiRelayLog();
        log.setUserId(key.getUserId());
        log.setApiKeyId(key.getId());
        log.setConfigId(config.getId());
        log.setInputMessage(inputMessage);
        log.setImageUrl(StringUtils.isNotBlank(imageUrl) ? imageUrl : null);
        log.setOutputMessage(outputMessage);
        log.setModelUsed(model);
        log.setInputTokens(inputTokens);
        log.setOutputTokens(outputTokens);
        save(log);
    }

    /**
     * 分页查询调用记录
     */
    @Override
    public IPage<ApiRelayLog> pageList(Integer page, Integer size, Long userId) {
        Page<ApiRelayLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ApiRelayLog> queryWrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            queryWrapper.eq(ApiRelayLog::getUserId, userId);
        }
        queryWrapper.orderByDesc(ApiRelayLog::getCreateTime);

        return page(pageParam, queryWrapper);
    }
}
