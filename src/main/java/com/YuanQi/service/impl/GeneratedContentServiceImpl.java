package com.YuanQi.service.impl;

import com.YuanQi.mapper.GeneratedContentMapper;
import com.YuanQi.pojo.GeneratedContent;
import com.YuanQi.service.GeneratedContentService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 生成内容服务实现
 */
@Service
@RequiredArgsConstructor
public class GeneratedContentServiceImpl extends ServiceImpl<GeneratedContentMapper, GeneratedContent> implements GeneratedContentService {

    private final GeneratedContentMapper generatedContentMapper;

    /**
     * 分页查询生成内容
     */
    @Override
    public IPage<GeneratedContent> pageList(Integer page, Integer size, Long userId, String type, Integer status) {
        Page<GeneratedContent> pageParam = new Page<>(page, size);
        return generatedContentMapper.selectPageWithUsername(pageParam, userId, type, status);
    }
}
