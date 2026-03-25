package com.YuanQi.service;

import com.YuanQi.pojo.GeneratedContent;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 生成内容服务接口
 */
public interface GeneratedContentService extends IService<GeneratedContent> {

    /**
     * 分页查询生成内容
     */
    IPage<GeneratedContent> pageList(Integer page, Integer size, Long userId, String type, Integer status);

    /**
     * 删除指定用户的生成内容
     */
    void deleteByIdAndUserId(Long id, Long userId);
}
