package com.YuanQi.mapper;

import com.YuanQi.pojo.GeneratedContent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AI生成内容Mapper
 */
@Mapper
public interface GeneratedContentMapper extends BaseMapper<GeneratedContent> {

    /**
     * 分页查询生成内容（关联用户表获取用户名）
     */
    IPage<GeneratedContent> selectPageWithUsername(IPage<GeneratedContent> page,
                                                    @Param("userId") Long userId,
                                                    @Param("type") String type,
                                                    @Param("status") Integer status);
}
