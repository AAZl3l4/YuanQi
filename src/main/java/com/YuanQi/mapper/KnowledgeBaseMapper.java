package com.YuanQi.mapper;

import com.YuanQi.pojo.KnowledgeBase;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 知识库Mapper接口
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    /**
     * 分页查询知识库（带用户名）
     */
    IPage<KnowledgeBase> selectPageWithUsername(Page<KnowledgeBase> page, @Param("userId") Long userId);
}
