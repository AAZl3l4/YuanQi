package com.YuanQi.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.YuanQi.mapper.ChatMessageMapper;
import com.YuanQi.mapper.ChatSessionMapper;
import com.YuanQi.pojo.ChatMessage;
import com.YuanQi.pojo.ChatSession;
import com.YuanQi.service.SessionService;
import com.YuanQi.utils.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 会话服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements SessionService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;

    /**
     * 创建新会话
     */
    @Override
    public ChatSession createSession() {
        Long userId = StpUtil.getLoginIdAsLong();

        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setTitle("新会话");

        chatSessionMapper.insert(session);
        log.info("用户 {} 创建会话: {}", userId, session.getSessionId());

        return session;
    }

    /**
     * 获取用户的会话列表（分页）
     */
    @Override
    public IPage<ChatSession> listUserSessions(Integer page, Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();

        Page<ChatSession> pageParam = new Page<>(page, size);
        return chatSessionMapper.selectPage(pageParam,
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .orderByDesc(ChatSession::getCreateTime)
        );
    }

    /**
     * 修改会话标题
     */
    @Override
    public void updateSessionTitle(String sessionId, String title) {
        ChatSession session = checkSessionOwner(sessionId);
        session.setTitle(title);
        chatSessionMapper.updateById(session);
        log.info("修改会话标题: {} -> {}", sessionId, title);
    }

    /**
     * 删除会话
     */
    @Override
    public void deleteSession(String sessionId) {
        ChatSession session = checkSessionOwner(sessionId);

        chatSessionMapper.deleteById(session.getId());

        chatMessageMapper.delete(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
        );

        log.info("删除会话: {}", sessionId);
    }

    /**
     * 验证会话归属
     */
    @Override
    public ChatSession checkSessionOwner(String sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();

        ChatSession session = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getSessionId, sessionId)
        );

        if (session == null) {
            throw new BusinessException("会话不存在");
        }

        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该会话");
        }

        return session;
    }
}
