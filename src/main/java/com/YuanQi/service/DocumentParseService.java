package com.YuanQi.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 文档解析服务接口
 */
public interface DocumentParseService {

    /**
     * 解析文档内容 并进行分块
     */
    List<Document> parseDocument(String url);

    /**
     * 解析文档内容（返回纯文本）
     */
    String extractText(List<Document> documents);

    /**
     * 分块文档
     */
    List<Document> splitDocuments(List<Document> documents);
}
