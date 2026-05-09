package com.YuanQi.service.impl;

import cn.hutool.http.HttpUtil;
import com.YuanQi.MCP.McpTools;
import com.YuanQi.configuration.SpringAiConfig;
import com.YuanQi.mapper.ApiRelayLogMapper;
import com.YuanQi.pojo.*;
import com.YuanQi.pojo.dto.RelayChatDTO;
import com.YuanQi.pojo.dto.StickerResponseDTO;
import com.YuanQi.service.*;
import com.YuanQi.utils.BusinessException;
import com.YuanQi.utils.TokenUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.tool.ToolCallback;
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
    private final McpTools mcpTools;

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
     * 表情包关键词提取提示词
     */
    private static final String STICKER_KEYWORD_PROMPT = """
            请根据以下对话内容，提取一个最适合搜索表情包的中文关键词（2-4个字）。
            要求：
            1. 只返回关键词本身，不要有任何解释
            2. 优先选择情绪类词汇（如：开心、无语、尴尬、生气、谢谢、点赞、加油、摸头、比心等）
            3. 其次选择描述类词汇（如：摸鱼、加班、干饭、睡觉、打工人等）
            """;

    /**
     * 表情包搜索API地址
     */
    private static final String STICKER_API_URL = "https://api.yuafeng.cn/API/ly/stickersSearch.php?msg=";

    /**
     * 调用AI并返回结果（支持上下文）
     */
    @Override
    public Object call(String apiKey, RelayChatDTO chatDTO) {
        String message = chatDTO.getMessage();
        String imageUrl = chatDTO.getImageUrl();
        String sender = chatDTO.getSender();
        Integer contextRounds = chatDTO.getContextRounds();
        Boolean enableWebSearch = chatDTO.getEnableWebSearch();
        Boolean enableSticker = chatDTO.getEnableSticker();

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
        List<Message> messages = buildMessagesWithHistory(config.getPersonaPrompt(), message, imageUrl, historyLogs, ragContext, enableWebSearch);

        // 获取联网搜索工具（如果启用）
        List<ToolCallback> tools = new ArrayList<>();
        if (Boolean.TRUE.equals(enableWebSearch)) {
            tools = mcpTools.getAllToolCallbacks().stream()
                    .filter(t -> t.getToolDefinition().name().equals("webSearch"))
                    .toList();
        }
        // 估算输入Token
        int estimatedInputTokens = TokenUtil.estimateTokens(messages.stream().map(Message::getText).toList());

        try {
            // 创建ChatClient
            ChatClient chatClient = springAiConfig.createChatClient(user.getApiKey(), model);

            // 同步调用
            String response = chatClient.prompt()
                    .messages(messages)
                    .toolCallbacks(tools)
                    .call()
                    .content();

            // 估算输出Token
            int estimatedOutputTokens = TokenUtil.estimateTokens(response);

            // 如果启用了表情包回复，提取关键词并搜索表情包
            String stickerUrl = null;
            if (Boolean.TRUE.equals(enableSticker)) {
                stickerUrl = fetchStickerUrl(chatClient, response);
            }

            // 保存调用记录
            Long usedKnowledgeBaseId = (Boolean.TRUE.equals(chatDTO.getUseKnowledgeBase()) && key.getKnowledgeBaseId() != null)
                    ? key.getKnowledgeBaseId() : null;
            saveLog(key, config, sender, message, imageUrl, response, model, estimatedInputTokens, estimatedOutputTokens, usedKnowledgeBaseId, enableWebSearch, stickerUrl);

            if (Boolean.TRUE.equals(enableSticker) && stickerUrl != null) {
                return new StickerResponseDTO(response, stickerUrl);
            }

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
    private List<Message> buildMessagesWithHistory(String personaPrompt, String message, String imageUrl, List<ApiRelayLog> historyLogs, String ragContext, Boolean enableWebSearch) {
        List<Message> messages = new ArrayList<>();

        // 系统提示词（注入当前时间）
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss E", CHINA));
        String systemPrompt = SYSTEM_PROMPT + "\n\n【重要】当前时间：" + currentTime + "。请直接使用这个时间回答，不要说'2023年'或'无法提供'或'不知道'。";
        messages.add(new SystemMessage(systemPrompt));

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

        // 强调使用MCP工具
        if (Boolean.TRUE.equals(enableWebSearch)) {
            messages.add(new SystemMessage("[系统提示]当用户的问题需要查询外部信息时，请主动使用MCP工具获取数据，不要说'无法提供'或'不知道'或'请稍等'"));
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
    private void saveLog(ApiKey key, ApiRelayConfig config, String sender, String inputMessage, String imageUrl, String outputMessage, String model, int inputTokens, int outputTokens, Long knowledgeBaseId, Boolean enableWebSearch, String stickerUrl) {
        ApiRelayLog log = new ApiRelayLog();
        log.setUserId(key.getUserId());
        log.setApiKeyId(key.getId());
        log.setConfigId(config.getId());
        log.setSender(sender);
        log.setKnowledgeBaseId(knowledgeBaseId);
        log.setEnableWebSearch(Boolean.TRUE.equals(enableWebSearch) ? 1 : 0);
        log.setEnableSticker(StringUtils.isNotBlank(stickerUrl) ? 1 : 0);
        log.setStickerUrl(StringUtils.isNotBlank(stickerUrl) ? stickerUrl : null);
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
    public IPage<ApiRelayLog> pageList(Integer page, Integer size, Long userId, String sender, Long configId, Long knowledgeBaseId, String keyword) {
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
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.and(qw -> qw.like(ApiRelayLog::getInputMessage, keyword)
                    .or().like(ApiRelayLog::getOutputMessage, keyword)
                    .or().like(ApiRelayLog::getModelUsed, keyword));
        }
        queryWrapper.orderByDesc(ApiRelayLog::getCreateTime);

        return page(pageParam, queryWrapper);
    }

    /**
     * 提取关键词并搜索表情包URL
     * @param chatClient ChatClient实例
     * @param response AI回复内容
     * @return 表情包URL，失败返回null
     */
    private String fetchStickerUrl(ChatClient chatClient, String response) {
        try {
            // 调用AI提取关键词
            String keyword = chatClient.prompt()
                    .system(STICKER_KEYWORD_PROMPT)
                    .user("对话内容：" + response)
                    .call()
                    .content();

            if (StringUtils.isBlank(keyword)) {
                log.warn("表情包关键词提取结果为空");
                return null;
            }

            // 清理关键词（去除标点、空格等）
            keyword = keyword.trim().replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
            log.debug("表情包搜索关键词：{}", keyword);

            if (keyword.isEmpty()) {
                return null;
            }

            // 调用表情包搜索API
            String apiResponse = HttpUtil.get(STICKER_API_URL + keyword, 10000);
            if (StringUtils.isBlank(apiResponse)) {
                log.warn("表情包API返回为空");
                return null;
            }

            // 解析JSON获取第一个jpg表情包的URL
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(apiResponse);

            if (root.get("Code").asInt() != 0) {
                log.warn("表情包API返回错误：{}", root.get("msg").asText());
                return null;
            }

            JsonNode dataArray = root.get("data");
            if (dataArray == null || !dataArray.isArray() || dataArray.isEmpty()) {
                log.warn("表情包API返回数据为空");
                return null;
            }

            // 找第一个jpg格式的表情包
            for (JsonNode item : dataArray) {
                String format = item.get("sticker_format").asText("");
                if ("jpg".equalsIgnoreCase(format)) {
                    String stickerUrl = item.get("sticker_url").asText();
                    log.debug("获取到表情包URL：{}", stickerUrl);
                    return stickerUrl;
                }
            }

            log.warn("未找到jpg格式的表情包");
            return null;
        } catch (Exception e) {
            log.error("获取表情包失败", e);
            return null;
        }
    }
}
