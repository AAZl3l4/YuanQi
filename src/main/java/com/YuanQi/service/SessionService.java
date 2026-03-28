package com.YuanQi.service;

import com.YuanQi.pojo.ChatSession;
import com.YuanQi.pojo.vo.SessionVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 会话服务接口
 */
public interface SessionService extends IService<ChatSession> {

    /**
     * 创建新会话（支持智能体）
     */
    SessionVO createSession(Long agentId);

    /**
     * 获取用户的会话列表（分页）
     */
    IPage<ChatSession> listUserSessions(Integer page, Integer size);

    /**
     * 修改会话标题
     */
    void updateSessionTitle(String sessionId, String title);

    /**
     * 删除会话
     */
    void deleteSession(String sessionId);

    /**
     * 验证会话归属并返回会话
     */
    ChatSession checkSessionOwner(String sessionId);

    /**
     * 检测是否是首次对话
     */
    Boolean isFirstMessage(String sessionId);
}
