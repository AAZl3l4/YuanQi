package com.YuanQi.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.YuanQi.pojo.KnowledgeBase;
import com.YuanQi.mapper.KnowledgeBaseMapper;
import com.YuanQi.service.KnowledgeBaseService;
import com.YuanQi.service.RagService;
import com.YuanQi.utils.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 知识库服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {

    private final RagService ragService;
    private final Cache<String, String> caffeineCache;

    private static final String CACHE_PREFIX = "kb:";

    /**
     * 创建知识库
     * 异步处理文档，存入向量库
     */
    @Override
    @Transactional
    public KnowledgeBase create(KnowledgeBase knowledgeBase) {
        // 初始状态为处理中
        knowledgeBase.setStatus(0);
        knowledgeBase.setChunkCount(0);
        save(knowledgeBase);

        // 异步处理文档
        processDocument(knowledgeBase);
        return knowledgeBase;
    }

    /**
     * 处理文档并存入向量库
     */
    private void processDocument(KnowledgeBase knowledgeBase) {
        try {
            // 调用RAG服务处理文档
            List<String> chunkIds = ragService.processAndStoreDocument(knowledgeBase.getDocumentUrl(), knowledgeBase.getId());

            // 更新知识库信息
            knowledgeBase.setChunkIds(String.join(",", chunkIds));
            knowledgeBase.setChunkCount(chunkIds.size());
            knowledgeBase.setStatus(1);
            updateById(knowledgeBase);

            // 缓存已加载标记
            caffeineCache.put(CACHE_PREFIX + knowledgeBase.getId(), "loaded");

            log.info("知识库文档处理完成，知识库ID: {}, 分块数: {}", knowledgeBase.getId(), chunkIds.size());
        } catch (Exception e) {
            // 处理失败
            knowledgeBase.setStatus(2);
            updateById(knowledgeBase);
            log.error("知识库文档处理失败，知识库ID: {}, 错误: {}", knowledgeBase.getId(), e.getMessage());
        }
    }

    /**
     * 更新知识库
     * 如果文档URL变更，重新处理文档
     */
    @Override
    @Transactional
    public KnowledgeBase update(KnowledgeBase knowledgeBase) {
        // 检查权限并获取现有知识库
        KnowledgeBase existing = checkOwner(knowledgeBase.getId());

        // 如果文档URL变更，需要重新处理
        if (!existing.getDocumentUrl().equals(knowledgeBase.getDocumentUrl())) {
            // 删除旧的向量数据
            if (existing.getChunkIds() != null && !existing.getChunkIds().isEmpty()) {
                List<String> oldChunkIds = Arrays.asList(existing.getChunkIds().split(","));
                ragService.deleteDocuments(oldChunkIds);
            }
            // 清除缓存
            caffeineCache.invalidate(CACHE_PREFIX + knowledgeBase.getId());
            // 重新处理
            knowledgeBase.setStatus(0);
            updateById(knowledgeBase);
            processDocument(knowledgeBase);
        } else {
            // 只更新基本信息
            updateById(knowledgeBase);
        }
        return knowledgeBase;
    }

    /**
     * 删除知识库
     * 同时删除向量库中的数据
     */
    @Override
    @Transactional
    public void delete(Long id) {
        // 检查权限并获取知识库
        KnowledgeBase knowledgeBase = checkOwner(id);
        doDelete(knowledgeBase);
    }

    /**
     * 管理员删除任意知识库
     */
    @Override
    @Transactional
    public void adminDelete(Long id) {
        KnowledgeBase knowledgeBase = getById(id);
        if (knowledgeBase == null) {
            throw new BusinessException("知识库不存在");
        }
        doDelete(knowledgeBase);
    }

    /**
     * 执行删除操作
     */
    private void doDelete(KnowledgeBase knowledgeBase) {
        // 删除向量库中的数据
        if (knowledgeBase.getChunkIds() != null && !knowledgeBase.getChunkIds().isEmpty()) {
            List<String> chunkIds = Arrays.asList(knowledgeBase.getChunkIds().split(","));
            ragService.deleteDocuments(chunkIds);
        }

        // 清除缓存
        caffeineCache.invalidate(CACHE_PREFIX + knowledgeBase.getId());

        // 逻辑删除
        removeById(knowledgeBase.getId());
        log.info("知识库已删除，ID: {}", knowledgeBase.getId());
    }

    /**
     * 分页获取知识库列表
     */
    @Override
    public IPage<KnowledgeBase> pageList(Integer page, Integer size, Long userId) {
        Page<KnowledgeBase> pageParam = new Page<>(page, size);
        return baseMapper.selectPageWithUsername(pageParam, userId);
    }

    /**
     * 检查知识库是否属于当前用户 并返回知识库对象
     */
    @Override
    public KnowledgeBase checkOwner(Long id) {
        KnowledgeBase knowledgeBase = getById(id);
        if (knowledgeBase == null) {
            throw new BusinessException("知识库不存在");
        }
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!knowledgeBase.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权操作此知识库");
        }
        return knowledgeBase;
    }

    /**
     * 确保知识库已加载到向量库
     * 用于重启后向量库数据丢失的情况
     */
    @Override
    public void ensureLoaded(KnowledgeBase knowledgeBase) {
        // 检查缓存
        String cached = caffeineCache.getIfPresent(CACHE_PREFIX + knowledgeBase.getId());
        if ("loaded".equals(cached)) {
            return;
        }

        // 缓存中没有，重新加载
        log.info("缓存未命中，重新加载知识库，ID: {}", knowledgeBase.getId());
        processDocument(knowledgeBase);
    }
}
