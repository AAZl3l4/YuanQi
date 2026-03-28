package com.YuanQi.mapper;

import com.YuanQi.pojo.ApiKey;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * API Key Mapper接口
 */
@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKey> {

    /**
     * 分页查询API Key（带用户名）
     */
    IPage<ApiKey> selectPageWithUsername(Page<ApiKey> page, @Param("userId") Long userId);
}
