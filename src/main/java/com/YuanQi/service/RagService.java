package com.YuanQi.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * RAG服务接口
 */
public interface RagService {

    /**
     * 处理文档并存储到向量库
     */
    List<String> processAndStoreDocument(String url);

    /**
     * 删除向量库中的文档
     */
    void deleteDocuments(List<String> ids);

    /**
     * 相似度检索 根据问题检索相关内容
     */
    List<Document> similaritySearch(String query, int topK);

    /**
     * 构建RAG上下文提示
     */
    String buildRagContext(String query, int topK);

}
