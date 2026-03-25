package com.YuanQi.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.YuanQi.pojo.ChatMessage;
import com.YuanQi.pojo.dto.ChatDTO;
import com.YuanQi.pojo.dto.ImageDTO;
import com.YuanQi.service.MessageService;
import com.YuanQi.utils.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 消息控制器
 */
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 获取会话历史消息（分页）
     */
    @GetMapping("/list")
    public Result<IPage<ChatMessage>> getMessages(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        IPage<ChatMessage> messages = messageService.getSessionMessages(sessionId, page, size);
        return Result.success(messages);
    }

    /**
     * 发送消息（SSE流式响应）
     * 带图自动使用视觉模型，不带图使用普通模型
     */
    @PostMapping("/stream")
    public SseEmitter chat(@Validated @RequestBody ChatDTO chatDTO) {
        // 5分钟超时
        SseEmitter emitter = new SseEmitter(300000L);
        // 手动校验登录，以SSE格式返回错误
        try {
            StpUtil.checkLogin();
        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("请先登录"));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
            return emitter;
        }

        // 登录校验通过，调用服务
        return messageService.chat(chatDTO);
    }

    /**
     * 生成图片 返回图片URL(30天有效期)
     */
    @PostMapping("/image")
    public Result<String> generateImage(@Validated @RequestBody ImageDTO imageDTO) {
        StpUtil.checkLogin();
        String imageUrl = messageService.generateImage(imageDTO);
        return Result.success(imageUrl);
    }
}
