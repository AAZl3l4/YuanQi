package com.YuanQi.mapper;

import com.YuanQi.pojo.Agent;
import com.YuanQi.pojo.vo.AgentVO;
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
     * 分页查询智能体（带用户名和统计信息）
     */
    IPage<AgentVO> selectPageWithUsername(Page<AgentVO> page, @Param("userId") Long userId, @Param("onlyMine") Boolean onlyMine, @Param("keyword") String keyword);
}
