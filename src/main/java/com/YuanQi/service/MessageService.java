package com.YuanQi.service;

import com.YuanQi.pojo.ChatMessage;
import com.YuanQi.pojo.dto.ChatDTO;
import com.YuanQi.pojo.dto.ImageDTO;
import com.YuanQi.pojo.dto.VideoDTO;
import com.YuanQi.pojo.vo.VideoTaskVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 消息服务接口
 */
public interface MessageService extends IService<ChatMessage> {

    /**
     * 获取会话历史消息（分页）
     */
    IPage<ChatMessage> getSessionMessages(String sessionId, Integer page, Integer size);

    /**
     * 发送消息并获取流式响应（SSE）
     * @param chatDTO 聊天请求DTO
     */
    SseEmitter chat(ChatDTO chatDTO);

    /**
     * 生成图片
     * @param imageDTO 生图请求DTO
     * @return 图片URL
     */
    String generateImage(ImageDTO imageDTO);

    /**
     * 提交视频生成任务（异步）
     */
    String submitVideoTask(VideoDTO videoDTO);

    /**
     * 查询视频任务结果
     */
    VideoTaskVO queryVideoTask(String taskId);
}
