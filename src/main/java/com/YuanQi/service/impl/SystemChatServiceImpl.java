package com.YuanQi.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.YuanQi.configuration.SpringAiConfig;
import com.YuanQi.mapper.SystemChatLogMapper;
import com.YuanQi.pojo.SystemChatLog;
import com.YuanQi.pojo.User;
import com.YuanQi.pojo.dto.SystemChatDTO;
import com.YuanQi.service.SystemChatService;
import com.YuanQi.service.UserService;
import com.YuanQi.MCP.SystemTools;
import com.YuanQi.utils.BusinessException;
import com.YuanQi.utils.TokenUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 系统问答服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemChatServiceImpl implements SystemChatService {

    private final SystemChatLogMapper systemChatLogMapper;
    private final UserService userService;
    private final SpringAiConfig springAiConfig;
    private final SystemTools systemTools;

    private static final String SYSTEM_PROMPT = """
            你是元启AI平台的管理助手，专门帮助管理员查询和操作系统数据。

            你可以使用以下工具来完成任务：
            1. queryUserStats - 查询用户统计
               - type参数: total(总用户数)、new(今日/本周/本月新增)、role(角色分布)、status(状态分布)、all(全部统计)
               - 如果不确定查询类型，不传参数，会返回全部统计数据
            2. queryActiveUserStats - 查询活跃用户统计（活跃用户指当天有聊天/生成/中转操作的用户）
               - period参数: yesterday(昨日日活)、today(今日日活)、week(本周活跃)、month(本月活跃)、all(全部活跃统计)
               - 如果不确定查询类型，不传参数，会返回全部统计数据
            3. queryUsageStats - 查询用量统计
               - period参数: today(今日)、week(本周)、month(本月)、total(总计)、all(全部统计)
               - 如果不确定查询类型，不传参数，会返回全部统计数据
            4. queryKnowledgeBaseStats - 查询知识库统计
               - type参数: total(总数)、status(状态分布)、new(新增统计)、all(全部统计)
               - 如果不确定查询类型，不传参数，会返回全部统计数据
            5. queryAgentStats - 查询智能体统计
               - type参数: total(总数)、public(公开/私有分布)、new(新增统计)、all(全部统计)
               - 如果不确定查询类型，不传参数，会返回全部统计数据
            6. queryApiKeyStats - 查询API Key统计
               - type参数: total(总数)、status(状态分布)、new(新增统计)、all(全部统计)
               - 如果不确定查询类型，不传参数，会返回全部统计数据
            7. queryRelayConfigStats - 查询API中转配置统计
               - type参数: total(总数)、public(公开/私有分布)、new(新增统计)、all(全部统计)
               - 如果不确定查询类型，不传参数，会返回全部统计数据
            8. queryRelayLogStats - 查询API中转调用记录统计
               - period参数: today(今日)、week(本周)、month(本月)、total(总计)、all(全部统计)
               - 如果不确定查询类型，不传参数，会返回全部统计数据
            9. queryMcpToolStats - 查询MCP工具状态
            10. toggleMcpTool - 启用/禁用MCP工具（这是唯一允许的操作）
            11. queryGeneratedContentStats - 查询生成内容统计
               - period参数: today(今日)、week(本周)、month(本月)、total(总计)、all(全部统计)
               - 如果不确定查询类型，不传参数，会返回全部统计数据

            回答规则：
            1. 当管理员询问数据时，主动调用相应工具获取信息
            2. 用简洁友好的方式呈现数据，可以使用表格或列表
            3. 如果管理员要求启用/禁用工具，先确认意图后再执行
            4. 不要编造数据，所有数据都来自工具查询结果
            5. 当用户的问题不明确具体统计类型时（如"最近的情况怎么样"），直接调用相关工具不传入参数，获取全部数据后自行分析回答
            """;

    /**
     * 系统问答（流式输出）
     */
    @Override
    public SseEmitter chat(SystemChatDTO chatDTO) {
        Long adminId = StpUtil.getLoginIdAsLong();
        String message = chatDTO.getMessage();
        String imageUrl = chatDTO.getImageUrl();
        int contextRounds = chatDTO.getContextRounds() != null ? chatDTO.getContextRounds() : 10;

        // 校验：消息内容和图片URL至少有一个不为空
        if ((message == null || message.trim().isEmpty()) && (imageUrl == null || imageUrl.trim().isEmpty())) {
            throw new BusinessException("消息内容或图片URL至少需要一个");
        }

        User user = userService.getCurrentUser();
        if (user.getApiKey() == null || user.getApiKey().isEmpty()) {
            throw new BusinessException("请先配置API Key");
        }
        String apiKey = user.getApiKey();

        // 根据是否带图选择模型
        String model = (imageUrl != null && !imageUrl.isEmpty()) ? user.getChatVisionModel() : user.getChatModel();

        // 保存用户消息
        SystemChatLog userLog = new SystemChatLog();
        userLog.setAdminId(adminId);
        userLog.setRole("user");
        userLog.setContent(message);
        userLog.setImageUrl(imageUrl);
        userLog.setModelUsed(model);
        systemChatLogMapper.insert(userLog);

        // 构建消息历史
        List<Message> messages = buildMessageHistory(adminId, message, imageUrl, contextRounds);

        // 获取系统工具回调
        List<ToolCallback> tools = getSystemToolCallbacks();

        SseEmitter emitter = new SseEmitter(300000L);
        log.debug("系统问答开始: adminId={}, model={}, hasImage={}", adminId, model, imageUrl != null);

        CompletableFuture.runAsync(() -> {
            try {
                ChatClient chatClient = springAiConfig.createChatClient(apiKey, model);

                Flux<ChatResponse> stream = chatClient.prompt()
                        .messages(messages)
                        .toolCallbacks(tools)
                        .stream()
                        .chatResponse();

                StringBuilder fullResponse = new StringBuilder();
                AtomicBoolean emitterCompleted = new AtomicBoolean(false);
                int estimatedInputTokens = TokenUtil.estimateTokens(
                        messages.stream().map(Message::getText).toList()
                );

                stream.subscribe(
                        response -> {
                            if (emitterCompleted.get()) return;
                            try {
                                String content = response.getResult().getOutput().getText();
                                if (content != null && !content.isEmpty()) {
                                    fullResponse.append(content);
                                    emitter.send(SseEmitter.event().name("message").data(content, MediaType.TEXT_PLAIN));
                                }
                            } catch (IllegalStateException e) {
                                emitterCompleted.set(true);
                                log.debug("SSE连接已断开");
                            } catch (Exception e) {
                                log.error("发送SSE消息失败", e);
                            }
                        },
                        error -> {
                            if (emitterCompleted.get()) return;
                            log.error("系统问答失败", error);
                            try {
                                emitter.send(SseEmitter.event().name("error").data("系统错误: " + error.getMessage(), MediaType.TEXT_PLAIN));
                                emitter.complete();
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        },
                        () -> {
                            int estimatedOutputTokens = TokenUtil.estimateTokens(fullResponse.toString());
                            saveAssistantMessage(adminId, fullResponse.toString(), model, estimatedInputTokens, estimatedOutputTokens);

                            if (emitterCompleted.get()) {
                                return;
                            }
                            try {
                                emitter.send(SseEmitter.event().name("complete").data("done", MediaType.TEXT_PLAIN));
                                emitter.complete();
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        }
                );

            } catch (Exception e) {
                log.error("系统问答处理失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("系统错误: " + e.getMessage(), MediaType.TEXT_PLAIN));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        return emitter;
    }

    /**
     * 清除管理员的历史对话
     */
    @Override
    public void clearHistory() {
        Long adminId = StpUtil.getLoginIdAsLong();
        systemChatLogMapper.delete(
                new LambdaQueryWrapper<SystemChatLog>()
                        .eq(SystemChatLog::getAdminId, adminId)
        );
        log.debug("已清除管理员历史对话: adminId={}", adminId);
    }

    /**
     * 获取管理员的历史对话
     */
    @Override
    public List<SystemChatLog> getHistory() {
        Long adminId = StpUtil.getLoginIdAsLong();
        return systemChatLogMapper.selectList(
                new LambdaQueryWrapper<SystemChatLog>()
                        .eq(SystemChatLog::getAdminId, adminId)
                        .orderByAsc(SystemChatLog::getCreateTime)
        );
    }

    /**
     * 构建消息历史
     */
    private List<Message> buildMessageHistory(Long adminId, String currentMessage, String imageUrl, int contextRounds) {
        List<Message> messages = new ArrayList<>();

        // 系统提示词
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss E", Locale.CHINA));
        String systemPrompt = SYSTEM_PROMPT + "\n\n【重要】当前时间：" + currentTime;
        messages.add(new SystemMessage(systemPrompt));

        // 加载历史消息
        int messageLimit = contextRounds * 2;
        if (messageLimit > 0) {
            List<SystemChatLog> history = systemChatLogMapper.selectList(
                    new LambdaQueryWrapper<SystemChatLog>()
                            .eq(SystemChatLog::getAdminId, adminId)
                            .orderByDesc(SystemChatLog::getCreateTime)
                            .last("LIMIT " + messageLimit)
            );

            // 反转顺序，按时间正序排列
            for (int i = history.size() - 1; i >= 0; i--) {
                SystemChatLog chatLog = history.get(i);
                if ("user".equals(chatLog.getRole())) {
                    if (chatLog.getImageUrl() != null && !chatLog.getImageUrl().isEmpty()) {
                        try {
                            Media imageMedia = Media.builder()
                                    .mimeType(MimeTypeUtils.IMAGE_JPEG)
                                    .data(new UrlResource(new URL(chatLog.getImageUrl())))
                                    .build();
                            messages.add(UserMessage.builder()
                                    .text(chatLog.getContent())
                                    .media(imageMedia)
                                    .build());
                        } catch (MalformedURLException e) {
                            messages.add(new UserMessage(chatLog.getContent()));
                        }
                    } else {
                        messages.add(new UserMessage(chatLog.getContent()));
                    }
                } else if ("assistant".equals(chatLog.getRole())) {
                    messages.add(new AssistantMessage(chatLog.getContent()));
                }
            }
        }

        // 当前消息
        if (imageUrl != null && !imageUrl.isEmpty()) {
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
                messages.add(new UserMessage(currentMessage));
            }
        } else {
            messages.add(new UserMessage(currentMessage));
        }

        return messages;
    }

    /**
     * 获取系统工具回调
     */
    private List<ToolCallback> getSystemToolCallbacks() {
        MethodToolCallbackProvider provider = MethodToolCallbackProvider.builder()
                .toolObjects(systemTools)
                .build();
        return List.of(provider.getToolCallbacks());
    }

    /**
     * 保存AI回复消息
     */
    private void saveAssistantMessage(Long adminId, String content, String model, int inputTokens, int outputTokens) {
        SystemChatLog chatlog = new SystemChatLog();
        chatlog.setAdminId(adminId);
        chatlog.setRole("assistant");
        chatlog.setContent(content);
        chatlog.setModelUsed(model);
        chatlog.setInputTokens(inputTokens);
        chatlog.setOutputTokens(outputTokens);
        systemChatLogMapper.insert(chatlog);
        log.debug("保存系统问答AI回复: adminId={}, length={}", adminId, content.length());
    }
}
