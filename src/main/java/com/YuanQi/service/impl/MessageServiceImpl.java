package com.YuanQi.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.YuanQi.configuration.SpringAiConfig;
import com.YuanQi.mapper.ChatMessageMapper;
import com.YuanQi.mapper.GeneratedContentMapper;
import com.YuanQi.pojo.ChatMessage;
import com.YuanQi.pojo.GeneratedContent;
import com.YuanQi.pojo.User;
import com.YuanQi.pojo.dto.ChatDTO;
import com.YuanQi.pojo.dto.ImageDTO;
import com.YuanQi.pojo.dto.VideoDTO;
import com.YuanQi.pojo.vo.VideoTaskVO;
import com.YuanQi.service.MessageService;
import com.YuanQi.service.SessionService;
import com.YuanQi.service.UserService;
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
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 消息服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements MessageService {

    private final ChatMessageMapper chatMessageMapper;
    private final GeneratedContentMapper generatedContentMapper;
    private final SessionService sessionService;
    private final UserService userService;
    private final SpringAiConfig springAiConfig;

    @Value("${spring.ai.zhipuai.base-url}")
    private String zhipuBaseUrl;

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
                            try {
                                emitter.send(SseEmitter.event().name("error").data("对话失败: " + error.getMessage()));
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
}
