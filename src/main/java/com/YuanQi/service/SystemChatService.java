package com.YuanQi.service;

import com.YuanQi.pojo.SystemChatLog;
import com.YuanQi.pojo.dto.SystemChatDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 系统问答服务接口
 */
public interface SystemChatService {

    /**
     * 系统问答（流式输出）
     * @param chatDTO 请求参数
     * @return SSE发射器
     */
    SseEmitter chat(SystemChatDTO chatDTO);

    /**
     * 获取管理员的历史对话
     * @return 历史消息列表
     */
    List<SystemChatLog> getHistory();

    /**
     * 清除管理员的历史对话
     */
    void clearHistory();
}
