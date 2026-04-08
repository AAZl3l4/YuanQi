package com.YuanQi.service.impl;

import com.YuanQi.service.DocumentParseService;
import com.YuanQi.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RAG服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final DocumentParseService documentParseService;
    private final VectorStore vectorStore;

    /**
     * 处理文档并存储到向量库
     * 流程：文档解析 -> 文本分块 -> 生成ID -> 添加到向量库
     *
     * @param url 文档URL地址
     * @return 生成的文档块ID列表
     */
    @Override
    public List<String> processAndStoreDocument(String url, Long knowledgeBaseId) {
        // 使用Tika解析文档，提取文本内容
        List<Document> documents = documentParseService.parseDocument(url);
        return processAndStoreDocuments(documents, url, knowledgeBaseId);
    }

    /**
     * 处理已解析的文档并存储到向量库
     * 注意：documents已经是分块后的文档
     *
     * @param documents 已分块的文档列表
     * @param url       文档URL地址
     * @return 生成的文档块ID列表
     */
    @Override
    public List<String> processAndStoreDocuments(List<Document> documents, String url, Long knowledgeBaseId) {
        // 为每个分块生成唯一ID，并添加元数据
        List<String> ids = new ArrayList<>();
        for (Document chunk : documents) {
            String id = UUID.randomUUID().toString();
            chunk.getMetadata().put("id", id);
            chunk.getMetadata().put("source", url);
            // 知识库ID用于隔离（必填）
            chunk.getMetadata().put("knowledgeBaseId", String.valueOf(knowledgeBaseId));
            ids.add(id);
        }

        // 将分块添加到向量存储，自动进行向量化
        vectorStore.add(documents);
        log.info("文档处理完成，URL: {}, 知识库ID: {}, 分块数: {}", url, knowledgeBaseId, documents.size());
        return ids;
    }

    /**
     * 删除向量库中的文档
     *
     * @param ids 要删除的文档ID列表
     */
    @Override
    public void deleteDocuments(List<String> ids) {
        vectorStore.delete(ids);
        log.info("文档已从向量库删除，数量: {}", ids.size());
    }

    /**
     * 相似度检索（带知识库隔离）
     * 将查询文本向量化，然后在向量库中搜索最相似的文档
     *
     * @param query 查询文本
     * @param topK  返回最相似的K个结果
     * @return 相似文档列表，按相似度降序排列
     */
    @Override
    public List<Document> similaritySearch(String query, int topK, Long knowledgeBaseId) {
        // 构建搜索请求，设置查询文本和返回数量，带知识库隔离过滤
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .filterExpression(b.eq("knowledgeBaseId", String.valueOf(knowledgeBaseId)).build())
                .build();
        
        List<Document> results = vectorStore.similaritySearch(request);
        log.info("相似度检索完成，查询: {}, 知识库ID: {}, 结果数: {}", query, knowledgeBaseId, results.size());
        return results;
    }

    /**
     * 构建RAG上下文提示（带知识库隔离）
     * 将检索到的相关文档拼接成提示词，供AI回答时参考
     *
     * @param query 用户查询问题
     * @param topK  检索的相关文档数量
     * @return 构建好的上下文提示字符串
     */
    @Override
    public String buildRagContext(String query, int topK, Long knowledgeBaseId) {
         // 1. 检索相关文档
        List<Document> relevantDocs = similaritySearch(query, topK, knowledgeBaseId);
        if (relevantDocs.isEmpty()) {
            return "";
        }

        // 2. 构建提示词上下文
        StringBuilder context = new StringBuilder();
        context.append("以下是知识库检索到的相关资料。如果资料中提及了的，你必须参考资料回答：\n\n");
        for (int i = 0; i < relevantDocs.size(); i++) {
            context.append("【参考内容").append(i + 1).append("】\n");
            context.append(relevantDocs.get(i).getText()).append("\n\n");
        }
        return context.toString();
    }
}
