package com.YuanQi.controller;

import com.YuanQi.pojo.ChatSession;
import com.YuanQi.service.SessionService;
import com.YuanQi.utils.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 会话控制器
 */
@RestController
@RequestMapping("/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    /**
     * 创建新会话
     */
    @PostMapping
    public Result<ChatSession> createSession() {
        ChatSession session = sessionService.createSession();
        return Result.success(session);
    }

    /**
     * 获取用户的会话列表（分页）
     */
    @GetMapping("/list")
    public Result<IPage<ChatSession>> listSessions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        IPage<ChatSession> sessions = sessionService.listUserSessions(page, size);
        return Result.success(sessions);
    }

    /**
     * 修改会话标题
     */
    @PutMapping("/{sessionId}/title")
    public Result<Void> updateSessionTitle(@RequestBody ChatSession session) {
        sessionService.updateSessionTitle(session.getSessionId(), session.getTitle());
        return Result.success();
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        sessionService.deleteSession(sessionId);
        return Result.success();
    }
}
