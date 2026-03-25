package com.YuanQi.service;

import com.YuanQi.pojo.ChatMessage;
import com.YuanQi.pojo.dto.ChatDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 消息服务接口
 */
public interface MessageService {

    /**
     * 获取会话历史消息（分页）
     */
    IPage<ChatMessage> getSessionMessages(String sessionId, Integer page, Integer size);

    /**
     * 发送消息并获取流式响应（SSE）
     * @param chatDTO 聊天请求DTO
     */
    SseEmitter chat(ChatDTO chatDTO);
}
