package com.YuanQi.mapper;

import com.YuanQi.pojo.Agent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 智能体Mapper
 */
@Mapper
public interface AgentMapper extends BaseMapper<Agent> {

    /**
     * 分页查询智能体（带用户名）
     */
    IPage<Agent> selectPageWithUsername(Page<Agent> page, @Param("userId") Long userId, @Param("onlyMine") Boolean onlyMine);
}
