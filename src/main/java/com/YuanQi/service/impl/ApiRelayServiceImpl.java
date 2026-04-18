package com.YuanQi.service.impl;

import com.YuanQi.configuration.SpringAiConfig;
import com.YuanQi.mapper.ApiRelayLogMapper;
import com.YuanQi.pojo.*;
import com.YuanQi.pojo.dto.RelayChatDTO;
import com.YuanQi.service.*;
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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.content.Media;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Locale.CHINA;

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
    private final KnowledgeBaseService knowledgeBaseService;
    private final RagService ragService;
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
     * 调用AI并返回结果（支持上下文）
     */
    @Override
    public String call(String apiKey, RelayChatDTO chatDTO) {
        String message = chatDTO.getMessage();
        String imageUrl = chatDTO.getImageUrl();
        String sender = chatDTO.getSender();
        Integer contextRounds = chatDTO.getContextRounds();

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

        List<ApiRelayLog> historyLogs = null;
        if (contextRounds != null && contextRounds > 0) {
            historyLogs = getHistoryLogs(key.getUserId(), config.getId(), key.getId(), sender, contextRounds);
        }

        // 构建知识库上下文（需要开关开启且绑定了知识库）
        String ragContext = "";
        if (Boolean.TRUE.equals(chatDTO.getUseKnowledgeBase()) && key.getKnowledgeBaseId() != null) {
            KnowledgeBase kb = knowledgeBaseService.getById(key.getKnowledgeBaseId());
            if (kb != null && kb.getStatus() == 1) {
                knowledgeBaseService.ensureLoaded(kb);
                ragContext = ragService.buildRagContext(message, 3, key.getKnowledgeBaseId());
            }
        }

        // 构建消息（包含历史上下文和知识库上下文）
        List<Message> messages = buildMessagesWithHistory(config.getPersonaPrompt(), message, imageUrl, historyLogs, ragContext);

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
            Long usedKnowledgeBaseId = (Boolean.TRUE.equals(chatDTO.getUseKnowledgeBase()) && key.getKnowledgeBaseId() != null) 
                    ? key.getKnowledgeBaseId() : null;
            saveLog(key, config, sender, message, imageUrl, response, model, estimatedInputTokens, estimatedOutputTokens, usedKnowledgeBaseId);

            return response;
        } catch (Exception e) {
                log.error("AI调用失败", e);
                throw new RuntimeException("AI调用失败: " + e.getMessage());
        }
    }

    /**
     * 获取历史对话记录
     */
    private List<ApiRelayLog> getHistoryLogs(Long userId, Long configId, Long apiKeyId, String sender, Integer contextRounds) {
        if (contextRounds == null || contextRounds <= 0) {
            return new ArrayList<>();
        }
        
        LambdaQueryWrapper<ApiRelayLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiRelayLog::getUserId, userId)
                .eq(ApiRelayLog::getConfigId, configId)
                .eq(ApiRelayLog::getApiKeyId, apiKeyId);
        // 如果有sender，则按sender过滤
        if (StringUtils.isNotBlank(sender)) {
            queryWrapper.eq(ApiRelayLog::getSender, sender);
        } else {
            queryWrapper.isNull(ApiRelayLog::getSender);
        }
        
        queryWrapper.orderByDesc(ApiRelayLog::getCreateTime);
        queryWrapper.last("LIMIT " + contextRounds);
        
        return list(queryWrapper);
    }

    /**
     * 构建消息（包含历史上下文和知识库上下文）
     */
    private List<Message> buildMessagesWithHistory(String personaPrompt, String message, String imageUrl, List<ApiRelayLog> historyLogs, String ragContext) {
        List<Message> messages = new ArrayList<>();

        // 系统提示词
        messages.add(new SystemMessage(SYSTEM_PROMPT));

        // 注入当前时间
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss E", CHINA));
        messages.add(new SystemMessage("当前时间：" + currentTime));

        // 人设/风格提示词
        if (personaPrompt != null && !personaPrompt.isEmpty()) {
            messages.add(new SystemMessage("你的人设/风格：" + personaPrompt));
        }

        // 知识库上下文
        if (ragContext != null && !ragContext.isEmpty()) {
            messages.add(new SystemMessage(ragContext));
        }

        // 添加历史对话（按时间正序）
        if (historyLogs != null && !historyLogs.isEmpty()) {
            List<ApiRelayLog> orderedLogs = new ArrayList<>(historyLogs);
            Collections.reverse(orderedLogs);
            
            for (ApiRelayLog log : orderedLogs) {
                        // 添加历史用户消息
                        if (StringUtils.isNotBlank(log.getInputMessage())) {
                                messages.add(new UserMessage(log.getInputMessage()));
                        }
                        // 添加历史AI回复
                        if (StringUtils.isNotBlank(log.getOutputMessage())) {
                                messages.add(new AssistantMessage(log.getOutputMessage()));
                        }
                }
        }

        // 当前用户消息
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                Media imageMedia = Media.builder()
                        .mimeType(MessageServiceImpl.getImageMimeType(imageUrl))
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
    private void saveLog(ApiKey key, ApiRelayConfig config, String sender, String inputMessage, String imageUrl, String outputMessage, String model, int inputTokens, int outputTokens, Long knowledgeBaseId) {
        ApiRelayLog log = new ApiRelayLog();
        log.setUserId(key.getUserId());
        log.setApiKeyId(key.getId());
        log.setConfigId(config.getId());
        log.setSender(sender);
        log.setKnowledgeBaseId(knowledgeBaseId);
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
    public IPage<ApiRelayLog> pageList(Integer page, Integer size, Long userId, String sender, Long configId, Long knowledgeBaseId) {
        Page<ApiRelayLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ApiRelayLog> queryWrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            queryWrapper.eq(ApiRelayLog::getUserId, userId);
        }
        if (StringUtils.isNotBlank(sender)) {
            queryWrapper.eq(ApiRelayLog::getSender, sender);
        }
        if (configId != null) {
            queryWrapper.eq(ApiRelayLog::getConfigId, configId);
        }
        if (knowledgeBaseId != null) {
            queryWrapper.eq(ApiRelayLog::getKnowledgeBaseId, knowledgeBaseId);
        }
        queryWrapper.orderByDesc(ApiRelayLog::getCreateTime);

        return page(pageParam, queryWrapper);
    }
}
