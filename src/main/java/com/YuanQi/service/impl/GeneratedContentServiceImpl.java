package com.YuanQi.service.impl;

import com.YuanQi.mapper.GeneratedContentMapper;
import com.YuanQi.pojo.GeneratedContent;
import com.YuanQi.service.GeneratedContentService;
import com.YuanQi.utils.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 生成内容服务实现
 */
@Slf4j
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

    /**
     * 删除指定用户的生成内容
     */
    @Override
    public void deleteByIdAndUserId(Long id, Long userId) {
        GeneratedContent content = generatedContentMapper.selectById(id);
        if (content == null) {
            throw new BusinessException("内容不存在");
        }
        if (!content.getUserId().equals(userId)) {
            throw new BusinessException("无权删除该内容");
        }
        generatedContentMapper.deleteById(id);
        log.info("用户 {} 删除生成内容: {}", userId, id);
    }
}
