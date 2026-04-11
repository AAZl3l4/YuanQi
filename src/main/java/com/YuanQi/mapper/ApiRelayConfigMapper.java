package com.YuanQi.mapper;

import com.YuanQi.pojo.ApiRelayConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * API中转配置 Mapper接口
 */
@Mapper
public interface ApiRelayConfigMapper extends BaseMapper<ApiRelayConfig> {

    /**
     * 分页查询API中转配置（带用户名）
     */
    IPage<ApiRelayConfig> selectPageWithUsername(
            Page<ApiRelayConfig> page,
            @Param("userId") Long userId,
            @Param("onlyMine") Boolean onlyMine,
            @Param("id") Long id);
}
