package com.YuanQi.service;

import com.YuanQi.pojo.KnowledgeBase;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 知识库服务接口
 */
public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    /**
     * 创建知识库
     */
    KnowledgeBase create(KnowledgeBase knowledgeBase);

    /**
     * 更新知识库
     */
    KnowledgeBase update(KnowledgeBase knowledgeBase);

    /**
     * 删除知识库
     */
    void delete(Long id);

    /**
     * 管理员删除任意知识库
     */
    void adminDelete(Long id);

    /**
     * 分页获取知识库列表
     */
    IPage<KnowledgeBase> pageList(Integer page, Integer size, Long userId, Long id);

    /**
     * 检查知识库是否属于当前用户 并返回知识库对象
     */
    KnowledgeBase checkOwner(Long id);

    /**
     * 确保知识库已加载到向量库
     */
    void ensureLoaded(KnowledgeBase knowledgeBase);
}
