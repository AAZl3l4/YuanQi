package com.YuanQi.service.impl;

import com.YuanQi.configuration.SpringAiConfig;
import com.YuanQi.mapper.ChatMessageMapper;
import com.YuanQi.pojo.ChatMessage;
import com.YuanQi.pojo.User;
import com.YuanQi.pojo.dto.ChatDTO;
import com.YuanQi.service.MessageService;
import com.YuanQi.service.SessionService;
import com.YuanQi.service.UserService;
import com.YuanQi.utils.BusinessException;
import com.YuanQi.utils.TokenUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 消息服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final ChatMessageMapper chatMessageMapper;
    private final SessionService sessionService;
    private final UserService userService;
    private final SpringAiConfig springAiConfig;

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

        sessionService.checkSessionOwner(sessionId);
        User user = userService.getCurrentUser();

        if (user.getApiKey() == null || user.getApiKey().isEmpty()) {
            throw new BusinessException("请先配置API Key");
        }
        String apiKey = user.getApiKey();

        // 根据是否带图选择模型
        String model = (imageUrl != null && !imageUrl.isEmpty()) ? user.getChatVisionModel() : user.getChatModel();

        // 保存用户消息
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setRole("user");
        userMessage.setContent(message);
        userMessage.setModelUsed(model);
        chatMessageMapper.insert(userMessage);

        // 构建消息历史
        List<Message> messages = buildMessageHistory(sessionId, message, imageUrl);

        SseEmitter emitter = new SseEmitter(300000L);
        log.info("开始对话: sessionId={}, hasImage={}, model={}", sessionId, imageUrl != null, model);

        CompletableFuture.runAsync(() -> {
            try {
                // 创建动态ChatClient（使用用户的API Key和选择的模型）
                ChatClient chatClient = springAiConfig.createChatClient(apiKey, model);

                // 流式调用
                Flux<ChatResponse> stream = chatClient.prompt()
                        .messages(messages)  // 传入历史消息(含本条)
                        .stream() // 启用流式模式
                        .chatResponse(); // 获取 ChatResponse 流

                StringBuilder fullResponse = new StringBuilder();
                // 使用TikToken估算输入Token（历史消息 + 当前消息）
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
                                    emitter.send(SseEmitter.event()
                                            .name("message")
                                            .data(content));
                                }

                            } catch (Exception e) {
                                log.error("发送SSE消息失败", e);
                            }
                        },
                        error -> { // 发生错误时执行
                            log.error("流式对话失败", error);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data("对话失败: " + error.getMessage()));
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
                                emitter.send(SseEmitter.event()
                                        .name("complete")
                                        .data("done"));
                                emitter.complete();
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        }
                );

            } catch (Exception e) {
                log.error("对话处理失败", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("系统错误: " + e.getMessage()));
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
     */
    private List<Message> buildMessageHistory(String sessionId, String currentMessage, String imageUrl) {
        List<Message> messages = new ArrayList<>();

        // 获取最近20条历史消息作为上下文
        Page<ChatMessage> page = new Page<>(1, 20);
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
}
