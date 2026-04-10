package com.YuanQi.service.impl;

import com.YuanQi.service.DocumentParseService;
import com.YuanQi.utils.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档解析服务实现
 * 使用Apache Tika解析各种格式的文档（PDF、Word、Excel、PPT、TXT等）
 */
@Slf4j
@Service
public class DocumentParseServiceImpl implements DocumentParseService {

    @Value("${yuanqi.rag.chunk-size:500}")
    private int chunkSize;

    @Value("${yuanqi.rag.overlap-size:50}")
    private int overlapSize;

    /**
     * 解析文档内容
     * 使用TikaDocumentReader自动识别文档类型并提取文本，并进行分块
     *
     * @param url 文档URL地址
     * @return 分块后的Document列表
     */
    @Override
    public List<Document> parseDocument(String url) {
        try {
            // 根据URL类型选择Resource实现
            Resource resource = getResource(url);
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<Document> documents = reader.get();

            // 对解析后的文档进行分块
            List<Document> chunks = splitDocuments(documents);
            log.info("文档解析成功，URL: {}, 分块数量: {}", url, chunks.size());
            return chunks;
        } catch (Exception e) {
            log.error("文档解析失败: {}", e.getMessage());
            throw new BusinessException("文档解析失败: " + e.getMessage());
        }
    }

    /**
     * 根据URL获取Resource
     * 如果是本平台上传的文件，转换为下载链接
     */
    private Resource getResource(String url) throws Exception {
        // 如果是本平台上传的文件URL（47.105.51.84），转换为下载链接
        if (url.contains("47.105.51.84") && url.contains("/api/file/") && !url.contains("/api/file/download/")) {
            String downloadUrl = url.replace("/api/file/", "/api/file/download/");
            log.info("转换为下载链接: {} -> {}", url, downloadUrl);
            return new UrlResource(downloadUrl);
        }
        // 其他URL直接使用
        return new UrlResource(url);
    }

    /**
     * 解析文档内容（返回纯文本）
     * 将Document列表合并为单个字符串
     *
     * @return 文档的纯文本内容
     */
    @Override
    public String extractText(List<Document> documents) {
        StringBuilder sb = new StringBuilder();
        for (Document doc : documents) {
            sb.append(doc.getText()).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * 分块文档
     * 使用TokenTextSplitter将长文档切分成小块，便于向量化存储和检索
     *
     * @param documents 原始文档列表
     * @return 分块后的文档列表
     */
    @Override
    public List<Document> splitDocuments(List<Document> documents) {
        // 创建分块器：chunkSize=500, minChunkSize=5, maxChunkSize=10000, keepSeparator=true
        TokenTextSplitter splitter = new TokenTextSplitter(
                chunkSize,               // 目标分块大小
                overlapSize,             // 分块重叠大小
                5,                       // 最小分块大小
                10000,                   // 最大分块大小
                true                     // 保留分隔符
        );

        List<Document> allChunks = new ArrayList<>();
        for (Document doc : documents) {
            // 对每个文档进行分块
            List<Document> chunks = splitter.split(doc);
            allChunks.addAll(chunks);
        }

        log.info("文档分块完成，原文档数: {}, 分块后数量: {}", documents.size(), allChunks.size());
        return allChunks;
    }
}
