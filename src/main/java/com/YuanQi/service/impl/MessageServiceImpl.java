package com.YuanQi.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.YuanQi.configuration.McpTools;
import com.YuanQi.configuration.SpringAiConfig;
import com.YuanQi.mapper.AgentMapper;
import com.YuanQi.mapper.ChatMessageMapper;
import com.YuanQi.mapper.GeneratedContentMapper;
import com.YuanQi.pojo.*;
import com.YuanQi.pojo.dto.ChatDTO;
import com.YuanQi.pojo.dto.ImageDTO;
import com.YuanQi.pojo.dto.VideoDTO;
import com.YuanQi.pojo.vo.VideoTaskVO;
import com.YuanQi.service.*;
import com.YuanQi.utils.BusinessException;
import com.YuanQi.utils.TokenUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 消息服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements MessageService {

    private final ChatMessageMapper chatMessageMapper;
    private final GeneratedContentMapper generatedContentMapper;
    private final AgentMapper agentMapper;
    private final SessionService sessionService;
    private final UserService userService;
    private final SpringAiConfig springAiConfig;
    private final DocumentParseService documentParseService;
    private final RagService ragService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final McpTools mcpTools;
    private final McpToolService mcpToolService;

    @Value("${spring.ai.zhipuai.base-url}")
    private String zhipuBaseUrl;

    @Value("${yuanqi.rag.small-doc-threshold}")
    private int smallDocThreshold;

    @Value("${yuanqi.rag.rag-top-k}")
    private int ragTopK;

    /**
     * 获取会话历史消息（分页）
     */
    @Override
    public IPage<ChatMessage> getSessionMessages(String sessionId, Integer page, Integer size) {
        sessionService.checkSessionOwner(sessionId);

        Page<ChatMessage> pageParam = new Page<>(page, size);
        return chatMessageMapper.selectPage(pageParam,
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreateTime)
        );
    }

    /**
     * 发送消息并获取流式响应（SSE）
     */
    @Override
    public SseEmitter chat(ChatDTO chatDTO) {
        String sessionId = chatDTO.getSessionId();
        String message = chatDTO.getMessage();
        String imageUrl = chatDTO.getImageUrl();
        String documentUrl = chatDTO.getDocumentUrl();
        Long knowledgeBaseId = chatDTO.getKnowledgeBaseId();
        Integer contextRounds = chatDTO.getContextRounds();
        List<Long> enabledTools = chatDTO.getEnabledTools();

        // 验证会话归属，获取会话信息
        ChatSession session = sessionService.checkSessionOwner(sessionId);
        Agent agent = null;
        String systemPrompt = null;
        if (session.getAgentId() != null) {
            agent = agentMapper.selectById(session.getAgentId());
            if (agent != null) {
                // 使用智能体的系统提示词
                systemPrompt = agent.getSystemPrompt();
                // 使用智能体的知识库（覆盖）
                if (agent.getKnowledgeBaseId() != null) {
                    knowledgeBaseId = agent.getKnowledgeBaseId();
                }
                // 使用智能体的工具（覆盖）
                if (agent.getToolIds() != null && !agent.getToolIds().isEmpty()) {
                    enabledTools = agent.getToolIds();
                }
            }
        }

        User user = userService.getCurrentUser();

        if (user.getApiKey() == null || user.getApiKey().isEmpty()) {
            throw new BusinessException("请先配置API Key");
        }
        String apiKey = user.getApiKey();

        // 首次对话，自动生成会话标题（智能体会话已有标题，跳过）
        if (agent == null){
            Boolean isFirstMessage = sessionService.isFirstMessage(sessionId);
            if (isFirstMessage) {
                String title = generateSessionTitle(apiKey, user.getChatModel(), message);
                sessionService.updateSessionTitle(sessionId, title);
            }
        }

        // 根据是否带图选择模型
        String model = (imageUrl != null && !imageUrl.isEmpty()) ? user.getChatVisionModel() : user.getChatModel();

        // 保存用户消息
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setRole("user");
        userMessage.setContent(message);
        userMessage.setModelUsed(model);
        chatMessageMapper.insert(userMessage);

        // 构建消息历史（支持文档和知识库）
        List<Message> messages = buildMessageHistory(sessionId, message, imageUrl, documentUrl, knowledgeBaseId, contextRounds, systemPrompt, enabledTools);

        // 获取用户选择的工具
        List<ToolCallback> tools = getTools(enabledTools);

        SseEmitter emitter = new SseEmitter(300000L);
        log.info("开始对话: sessionId={}, model={}, hasImage={}, hasDocument={}, knowledgeBaseId={}, enabledTools={}, agentId={}",
                sessionId, model, imageUrl != null && !imageUrl.isEmpty(), documentUrl != null && !documentUrl.isEmpty(), knowledgeBaseId != null, enabledTools != null && !enabledTools.isEmpty(), agent != null);

        CompletableFuture.runAsync(() -> {
            try {
                // 创建动态ChatClient（使用用户的API Key和选择的模型）
                ChatClient chatClient = springAiConfig.createChatClient(apiKey, model);

                // 流式调用
                Flux<ChatResponse> stream = chatClient.prompt()
                        .messages(messages)  // 传入历史消息(含本条)
                        .toolCallbacks(tools)        // 注册工具
                        .stream() // 启用流式模式
                        .chatResponse(); // 获取 ChatResponse 流

                StringBuilder fullResponse = new StringBuilder();
                // 使用Tik Token估算输入Token（历史消息 + 当前消息）
                int estimatedInputTokens = TokenUtil.estimateTokens(
                        messages.stream().map(Message::getText).toList()
                );
                log.debug("估算输入Token: {}", estimatedInputTokens);

                // 订阅流式响应
                stream.subscribe(
                        response -> { // 收到数据块时执行
                            try {
                                String content = response.getResult().getOutput().getText();
                                if (content != null && !content.isEmpty()) {
                                    fullResponse.append(content);
                                    emitter.send(SseEmitter.event().name("message").data(content));
                                }

                            } catch (Exception e) {
                                log.error("发送SSE消息失败", e);
                            }
                        },
                        error -> { // 发生错误时执行
                            log.error("流式对话失败", error);
                            String errorMsg = parseApiError(error);
                            try {
                                emitter.send(SseEmitter.event().name("").data(errorMsg));
                                emitter.complete();
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        },
                        () -> { // 流完成时执行
                            // 估算输出Token（AI回复内容）
                            int estimatedOutputTokens = TokenUtil.estimateTokens(fullResponse.toString());
                            log.debug("估算输出Token: {}", estimatedOutputTokens);
                            // 保存AI回复（携带Token统计）
                            saveAssistantMessage(sessionId, fullResponse.toString(), model, estimatedInputTokens, estimatedOutputTokens);
                            try {
                                emitter.send(SseEmitter.event().name("complete").data("done"));
                                emitter.complete();
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        }
                );

            } catch (Exception e) {
                log.error("对话处理失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("系统错误: " + e.getMessage()));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        return emitter;
    }

    /**
     * 构建消息历史上下文
     * 支持图片、文档、知识库
     */
    private List<Message> buildMessageHistory(String sessionId, String currentMessage, String imageUrl, 
            String documentUrl, Long knowledgeBaseId, Integer contextRounds, String systemPrompt, List<Long> enabledTools) {
        List<Message> messages = new ArrayList<>();

        // 构建系统提示词
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            systemPrompt = "你是元启AI助手的智能助手。元启AI是一个集成多种AI能力的智能对话平台，" +
                    "支持文字对话、图文理解、知识库检索、MCP调用等多种功能。请用简洁友好的方式回答用户问题。";
        }

        messages.add(new SystemMessage(systemPrompt));

        // 根据轮数计算消息条数（1轮=2条消息）
        int messageLimit = contextRounds * 2;
        if (messageLimit > 0) {
            Page<ChatMessage> page = new Page<>(1, messageLimit);
            IPage<ChatMessage> history = chatMessageMapper.selectPage(page,
                    new LambdaQueryWrapper<ChatMessage>()
                            .eq(ChatMessage::getSessionId, sessionId)
                            .orderByDesc(ChatMessage::getCreateTime)
            );

            // 反转顺序，按时间正序排列
            List<ChatMessage> records = history.getRecords();
            for (int i = records.size() - 1; i >= 0; i--) {
                ChatMessage msg = records.get(i);
                if ("user".equals(msg.getRole())) {
                    messages.add(new UserMessage(msg.getContent()));
                } else if ("assistant".equals(msg.getRole())) {
                    messages.add(new AssistantMessage(msg.getContent()));
                }
            }
        }

        // 处理文档或知识库上下文
        String ragContext = buildRagContext(documentUrl, knowledgeBaseId, currentMessage);
        if (!ragContext.isEmpty()) {
            messages.add(new UserMessage(ragContext));
        }

        // 强调使用MCP工具
        if (enabledTools != null && !enabledTools.isEmpty()) {
            messages.add(new UserMessage("[系统提示]当用户的问题需要查询外部信息时，请主动使用MCP工具获取数据"));
        }

        // 根据是否带图构建当前消息
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // 图文消息：带图片的UserMessage
            try {
                Media imageMedia = Media.builder()
                        .mimeType(MimeTypeUtils.IMAGE_JPEG)
                        .data(new UrlResource(new URL(imageUrl)))
                        .build();
                messages.add(UserMessage.builder()
                        .text(currentMessage)
                        .media(imageMedia)
                        .build());
            } catch (MalformedURLException e) {
                log.error("图片URL格式错误: {}", imageUrl, e);
                // URL错误时退化为纯文本消息
                messages.add(new UserMessage(currentMessage));
            }
        } else {
            // 纯文本消息
            messages.add(new UserMessage(currentMessage));
        }

        return messages;
    }

    /**
     * 构建RAG上下文
     * 处理文档和知识库
     */
    private String buildRagContext(String documentUrl, Long knowledgeBaseId, String query) {
        StringBuilder context = new StringBuilder();

        // 基于知识库聊天
        if (knowledgeBaseId != null) {
            // 检查知识库是否属于当前用户并获取知识库对象
            KnowledgeBase knowledgeBase = knowledgeBaseService.checkOwner(knowledgeBaseId);
            // 确保知识库已加载到向量库
            knowledgeBaseService.ensureLoaded(knowledgeBase);
            // 构建RAG上下文
            String ragContext = ragService.buildRagContext(query, ragTopK);
            if (!ragContext.isEmpty()) {
                context.append(ragContext);
            }
            log.info("知识库RAG上下文构建完成，知识库ID: {}", knowledgeBaseId);
        }

        // 携带文档聊天
        if (documentUrl != null && !documentUrl.isEmpty()) {
            try {
                // 先解析文档 得到分块后的文档
                List<Document> documents = documentParseService.parseDocument(documentUrl);

                if (documents.size() < smallDocThreshold) {
                    // 小文档：直接注入上下文
                    String fullText = documentParseService.extractText(documents);
                    context.append("【用户提供的文档内容】\n").append(fullText).append("\n\n");
                    log.info("小文档直接注入上下文，分块数: {}", documents.size());
                } else {
                    // 大文档：临时RAG检索
                    // 使用已解析的文档存入向量库
                    List<String> chunkIds = ragService.processAndStoreDocuments(documents, documentUrl);
                    // 检索相关内容
                    String ragContext = ragService.buildRagContext(query, ragTopK);
                    if (!ragContext.isEmpty()) {
                        context.append(ragContext);
                    }
                    // 删除临时向量数据
                    ragService.deleteDocuments(chunkIds);
                    log.info("大文档临时RAG检索完成，分块数: {}", chunkIds.size());
                }
            } catch (Exception e) {
                log.error("文档处理失败: {}", e.getMessage());
            }
        }

        return context.toString();
    }

    /**
     * 获取筛选后的工具
     */
    private List<ToolCallback> getTools(List<Long> enabledTools) {
        if (enabledTools == null || enabledTools.isEmpty()) {
            return new ArrayList<>();
        }

        // 根据ID查询数据库中的工具
        List<McpTool> dbTools = mcpToolService.listByIds(enabledTools);
        if (dbTools.isEmpty()) {
            throw new BusinessException("未找到指定的工具");
        }

        // 检查是否启用
        Set<String> enabledToolNames = new HashSet<>();
        for (McpTool tool : dbTools) {
            if (tool.getEnabled() == 1) {
                enabledToolNames.add(tool.getName());
            } else {
                throw new BusinessException(tool.getName() + "工具未启用");
            }
        }

        // 从 McpTools 获取所有工具回调并过滤
        List<ToolCallback> allCallbacks = mcpTools.getAllToolCallbacks();
        return allCallbacks.stream()
                .filter(callback -> enabledToolNames.contains(callback.getToolDefinition().name()))
                .collect(Collectors.toList());
    }
    /**
     * 保存AI回复消息
     */
    private void saveAssistantMessage(String sessionId, String content, String model, int inputTokens, int outputTokens) {
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(content);
        assistantMessage.setModelUsed(model);
        assistantMessage.setInputTokens(inputTokens);
        assistantMessage.setOutputTokens(outputTokens);
        chatMessageMapper.insert(assistantMessage);
        log.info("保存AI回复: sessionId={}, length={}, inputTokens={}, outputTokens={}",
                sessionId, content.length(), inputTokens, outputTokens);
    }

    /**
     * 生成图片
     */
    @Override
    public String generateImage(ImageDTO imageDTO) {
        User user = userService.getCurrentUser();

        if (user.getApiKey() == null || user.getApiKey().isEmpty()) {
            throw new BusinessException("请先配置API Key");
        }

        String apiKey = user.getApiKey();
        String model = user.getImageModel();
        log.info("开始生成图片: model={}, prompt={}", model, imageDTO.getPrompt());

        // 创建ImageModel
        ImageModel imageModel = springAiConfig.createImageModel(apiKey);

        // 构建图片选项
        ImageOptionsBuilder optionsBuilder = ImageOptionsBuilder.builder()
                .model(model);
        
        // 设置尺寸（可选）
        if (imageDTO.getSize() != null && !imageDTO.getSize().isEmpty()) {
            String[] sizeParts = imageDTO.getSize().split("x");
            optionsBuilder.width(Integer.parseInt(sizeParts[0]))
                    .height(Integer.parseInt(sizeParts[1]));
        }

        // 调用生图API
        ImageResponse response = imageModel.call(new ImagePrompt(imageDTO.getPrompt(), optionsBuilder.build()));
        // 获取图片URL
        String imageUrl = response.getResult().getOutput().getUrl();

        GeneratedContent content = new GeneratedContent();
        content.setUserId(user.getId());
        content.setType("image");
        content.setPrompt(imageDTO.getPrompt());
        content.setResultUrl(imageUrl);
        content.setModelUsed(model);

        if (imageUrl == null || imageUrl.isEmpty()) {
            log.error("图片生成失败: {}", response);
            // 保存失败记录到数据库
            content.setStatus(2);
            content.setErrorMsg(response.toString());
            generatedContentMapper.insert(content);
            throw new BusinessException("图片生成失败，请重试");
        }else{
            log.info("图片生成成功: url={}", imageUrl);
            // 保存生成记录到数据库
            content.setStatus(1);
            generatedContentMapper.insert(content);
            return imageUrl;
        }
    }

    /**
     * 提交视频生成任务（异步）
     */
    @Override
    public String submitVideoTask(VideoDTO videoDTO) {
        User user = userService.getCurrentUser();

        if (user.getApiKey() == null || user.getApiKey().isEmpty()) {
            throw new BusinessException("请先配置API Key");
        }

        String apiKey = user.getApiKey();
        String model = user.getVideoModel();
        log.info("提交视频生成任务: model={}, prompt={}", model, videoDTO.getPrompt());

        // Spring Ai 暂时还不支持视频生成 直接调用智谱API生成视频
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("prompt", videoDTO.getPrompt());
        requestBody.put("quality", videoDTO.getQuality());
        requestBody.put("size", videoDTO.getSize());
        requestBody.put("duration", videoDTO.getDuration());
        requestBody.put("fps", videoDTO.getFps());
        requestBody.put("with_audio", videoDTO.getWithAudio());
        // 图生视频时传入图片URL
        if (videoDTO.getImageUrl() != null && !videoDTO.getImageUrl().isEmpty()) {
            requestBody.put("image_url", videoDTO.getImageUrl());
        }

        // 调用智谱视频生成API
        String response = HttpRequest.post(zhipuBaseUrl + "/v4/videos/generations")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(requestBody))
                .execute()
                .body();

        JSONObject json = JSONUtil.parseObj(response);
        String taskId = json.getStr("id");
        log.info("视频任务提交成功: taskId={}", taskId);

        // 保存生成记录到数据库（状态：处理中）
        GeneratedContent content = new GeneratedContent();
        content.setUserId(user.getId());
        content.setType("video");
        content.setPrompt(videoDTO.getPrompt());
        content.setTaskId(taskId);
        content.setStatus(0);
        content.setModelUsed(model);
        generatedContentMapper.insert(content);

        return taskId;
    }

    /**
     * 查询视频任务结果
     */
    @Override
    public VideoTaskVO queryVideoTask(String taskId) {
        User user = userService.getCurrentUser();

        if (user.getApiKey() == null || user.getApiKey().isEmpty()) {
            throw new BusinessException("请先配置API Key");
        }

        String apiKey = user.getApiKey();
        log.info("查询视频任务: taskId={}", taskId);

        // 调用智谱查询API
        String response = HttpRequest.get(zhipuBaseUrl + "/v4/async-result/" + taskId)
                .header("Authorization", "Bearer " + apiKey)
                .execute()
                .body();

        JSONObject json = JSONUtil.parseObj(response);
        
        VideoTaskVO vo = new VideoTaskVO();
        vo.setTaskId(taskId);
        vo.setStatus(json.getStr("task_status"));

        // 成功时获取视频URL
        if ("SUCCESS".equals(vo.getStatus())) {
            JSONArray videoResult = json.getJSONArray("video_result");
            if (videoResult != null && !videoResult.isEmpty()) {
                JSONObject video = videoResult.getJSONObject(0);
                vo.setVideoUrl(video.getStr("url"));
                vo.setCoverImageUrl(video.getStr("cover_image_url"));

                // 更新数据库记录（状态：成功）
                generatedContentMapper.update(new LambdaUpdateWrapper<GeneratedContent>()
                        .eq(GeneratedContent::getTaskId, taskId)
                        .set(GeneratedContent::getStatus, 1)
                        .set(GeneratedContent::getResultUrl, video.getStr("url"))
                        .set(GeneratedContent::getCoverUrl, video.getStr("cover_image_url")));
            }
        }

        // 失败时获取错误信息
        if ("FAIL".equals(vo.getStatus())) {
            String errorMsg = json.getStr("message", "视频生成失败");
            vo.setErrorMessage(errorMsg);

            // 更新数据库记录（状态：失败）
            generatedContentMapper.update(new LambdaUpdateWrapper<GeneratedContent>()
                    .eq(GeneratedContent::getTaskId, taskId)
                    .set(GeneratedContent::getStatus, 2)
                    .set(GeneratedContent::getErrorMsg, errorMsg));
        }

        return vo;
    }

    /**
     * 生成会话标题（使用AI根据用户第一条消息生成简短标题）
     */
    private String generateSessionTitle(String apiKey, String model, String message) {
        try {
            ChatClient chatClient = springAiConfig.createChatClient(apiKey, model);

            String prompt = "请根据用户消息，生成一个简短的能概括对话主题的会话标题（不超过10个字）。只返回标题内容，不要有任何解释。";

            String title = chatClient.prompt()
                    .system(prompt)
                    .user(message)
                    .call()
                    .content();

            // 清理标题（去除引号、换行等）
            title = title.replace("\"", "").replace("'", "").trim();
            if (title.length() > 20) {
                title = title.substring(0, 20);
            }

            log.info("生成会话标题: {}", title);
            return title.isEmpty() ? "新会话" : title;
        } catch (Exception e) {
            log.warn("生成会话标题失败，使用默认标题: {}", e.getMessage());
            return message.length() > 20 ? message.substring(0, 20) : message;
        }
    }

    /**
     * 解析API错误信息
     */
    private String parseApiError(Throwable error) {
        String message = error.getMessage();
        if (message == null) {
            return "对话失败，请稍后重试";
        }
        // 400错误：模型不存在
        if (message.contains("400") || message.contains("Bad Request")) {
            return "模型不存在或参数错误，请检查模型配置";
        }
        // 401错误：API Key无效
        if (message.contains("401") || message.contains("Unauthorized")) {
            return "API Key无效，请检查配置";
        }
        // 429错误：限速或无权限
        if (message.contains("429") || message.contains("Too Many Requests") || message.contains("Rate limit")) {
            return "请求过于频繁或该模型无调用权限，请稍后重试";
        }
        // 500错误：服务端错误
        if (message.contains("500") || message.contains("Internal Server Error")) {
            return "AI服务暂时不可用，请稍后重试";
        }
        // 其他错误
        return "对话失败: " + message;
    }
}
