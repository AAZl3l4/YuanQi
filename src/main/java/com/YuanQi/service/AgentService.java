package com.YuanQi.service;

import com.YuanQi.pojo.Agent;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 智能体服务接口
 */
public interface AgentService extends IService<Agent> {

    /**
     * 创建智能体
     */
    Agent create(Agent agent);

    /**
     * 更新智能体
     */
    Agent update(Agent agent);

    /**
     * 删除智能体（用户只能删除自己的）
     */
    void delete(Long id);

    /**
     * 分页获取智能体列表
     * @param userId 指定用户ID
     * @param onlyMine 只看自己的
     */
    IPage<Agent> pageList(Integer page, Integer size, Long userId, Boolean onlyMine);

    /**
     * 检查智能体是否属于当前用户
     */
    Agent checkOwner(Long id);

    /**
     * 检查智能体是否可用（存在且公开或属于当前用户）
     */
    Agent checkAvailable(Long id);
}
