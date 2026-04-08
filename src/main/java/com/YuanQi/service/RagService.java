package com.YuanQi.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * RAG服务接口
 */
public interface RagService {

    /**
     * 处理文档并存储到向量库
     *
     * @param url 文档URL地址
     * @param knowledgeBaseId 知识库ID（用于隔离）
     * @return 生成的文档块ID列表
     */
    List<String> processAndStoreDocument(String url, Long knowledgeBaseId);

    /**
     * 处理已解析的文档并存储到向量库
     *
     * @param documents 已分块的文档列表
     * @param url 文档URL地址
     * @param knowledgeBaseId 知识库ID（用于隔离）
     * @return 生成的文档块ID列表
     */
    List<String> processAndStoreDocuments(List<Document> documents, String url, Long knowledgeBaseId);

    /**
     * 删除向量库中的文档
     */
    void deleteDocuments(List<String> ids);

    /**
     * 相似度检索（带知识库隔离）
     *
     * @param query 查询文本
     * @param topK 返回数量
     * @param knowledgeBaseId 知识库ID（用于隔离）
     * @return 相似文档列表
     */
    List<Document> similaritySearch(String query, int topK, Long knowledgeBaseId);

    /**
     * 构建RAG上下文提示（带知识库隔离）
     *
     * @param query 用户查询问题
     * @param topK 检索的相关文档数量
     * @param knowledgeBaseId 知识库ID（用于隔离）
     * @return 构建好的上下文提示字符串
     */
    String buildRagContext(String query, int topK, Long knowledgeBaseId);

}
