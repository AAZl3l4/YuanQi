package com.YuanQi.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.YuanQi.pojo.dto.SystemChatDTO;
import com.YuanQi.service.SystemChatService;
import com.YuanQi.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 系统问答控制器
 * 管理后台RAG问答功能，支持通过自然语言查询和操作系统数据
 */
@RestController
@RequestMapping("/admin/system-chat")
@RequiredArgsConstructor
public class SystemChatController {

    private final SystemChatService systemChatService;

    /**
     * 系统问答（流式输出）
     * SSE接口需要手动校验，以SSE格式返回错误
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody SystemChatDTO chatDTO) {
        SseEmitter emitter = new SseEmitter(300000L);
        
        // 手动校验登录和管理员权限
        try {
            StpUtil.checkLogin();
            if (!StpUtil.hasRole("admin")) {
                emitter.send(SseEmitter.event().name("error").data("无权限访问"));
                emitter.complete();
                return emitter;
            }
        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event().name("error").data("请先登录"));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
            return emitter;
        }
        
        // 权限校验通过，调用服务
        try {
            return systemChatService.chat(chatDTO);
        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
            return emitter;
        }
    }

    /**
     * 获取历史记录
     */
    @SaCheckRole("admin")
    @GetMapping("/history")
    public Result<?> getHistory() {
        return Result.success(systemChatService.getHistory());
    }

    /**
     * 清除历史对话
     */
    @SaCheckRole("admin")
    @DeleteMapping("/history")
    public Result<Void> clearHistory() {
        systemChatService.clearHistory();
        return Result.success();
    }
}
