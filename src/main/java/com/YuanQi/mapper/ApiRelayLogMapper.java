package com.YuanQi.mapper;

import com.YuanQi.pojo.ApiRelayLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * API中转调用记录 Mapper接口
 */
@Mapper
public interface ApiRelayLogMapper extends BaseMapper<ApiRelayLog> {
}
