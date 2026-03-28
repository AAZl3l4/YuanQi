package com.YuanQi.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.YuanQi.mapper.AgentMapper;
import com.YuanQi.pojo.Agent;
import com.YuanQi.service.AgentService;
import com.YuanQi.utils.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 智能体服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl extends ServiceImpl<AgentMapper, Agent> implements AgentService {

    /**
     * 创建智能体
     */
    @Override
    public Agent create(Agent agent) {
        Long userId = StpUtil.getLoginIdAsLong();
        agent.setUserId(userId);
        agent.setIsPublic(agent.getIsPublic() != null ? agent.getIsPublic() : 0);
        save(agent);
        log.info("创建智能体: id={}, name={}", agent.getId(), agent.getName());
        return agent;
    }

    /**
     * 更新智能体
     */
    @Override
    public Agent update(Agent agent) {
        checkOwner(agent.getId());
        updateById(agent);
        log.info("更新智能体: id={}", agent.getId());
        return agent;
    }

    /**
     * 删除智能体
     */
    @Override
    public void delete(Long id) {
        checkOwner(id);
        removeById(id);
        log.info("删除智能体: id={}", id);
    }

    /**
     * 分页获取智能体列表
     */
    @Override
    public IPage<Agent> pageList(Integer page, Integer size, Long userId, Boolean onlyMine) {
        Page<Agent> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Agent> wrapper = new LambdaQueryWrapper<>();

        if (userId != null) {
            if (onlyMine != null && onlyMine) {
                // 只查该用户的
                wrapper.eq(Agent::getUserId, userId);
            } else {
                // 查该用户的 + 公开的
                wrapper.and(w -> w.eq(Agent::getUserId, userId).or().eq(Agent::getIsPublic, 1));
            }
        }
        // userId为null时查全部

        wrapper.orderByAsc(Agent::getSortOrder).orderByDesc(Agent::getCreateTime);
        return page(pageParam, wrapper);
    }

    /**
     * 检查智能体是否属于当前用户
     */
    @Override
    public Agent checkOwner(Long id) {
        Agent agent = getById(id);
        if (agent == null) {
            throw new BusinessException("智能体不存在");
        }
        // 系统预设的智能体（userId为null）不允许用户修改删除
        if (agent.getUserId() == null) {
            throw new BusinessException("系统预设智能体不可修改");
        }
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!agent.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权操作此智能体");
        }
        return agent;
    }

    /**
     * 检查智能体是否可用
     */
    @Override
    public Agent checkAvailable(Long id) {
        Agent agent = getById(id);
        if (agent == null) {
            throw new BusinessException("智能体不存在");
        }
        // 公开的或系统预设的智能体，所有人可用
        if (agent.getIsPublic() == 1 || agent.getUserId() == null) {
            return agent;
        }
        // 私有的，只有创建者可用
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!agent.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权使用此智能体");
        }
        return agent;
    }
}
