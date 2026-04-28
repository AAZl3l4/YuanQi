package com.YuanQi.configuration;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * RAG配置类
 */
@Configuration
public class RagConfig {

    /**
     * 不配置本地Embedding模型 使用Spring AI默认的all-MiniLM-L6-v2模型
     * 首次启动自动下载(默认github 手动new也会github下载 不能自动读取 所以不写bean)
     * 使用Spring AI默认的all-MiniLM-L6-v2模型，首次启动自动下载
     * @return TransformersEmbeddingModel 本地嵌入模型实例
     */
//    @Bean
//    @Primary
//    public EmbeddingModel embeddingModel() {
//        // 不设置资源，使用默认模型 all-MiniLM-L6-v2
//        return new TransformersEmbeddingModel();
//    }

    /**
     * 配置向量存储（内存型向量库）
     *
     * @param embeddingModel 嵌入模型，用于将文本转换为向量
     * @return SimpleVectorStore 内存向量存储实例
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        // 使用EmbeddingModel构建SimpleVectorStore，建立向量存储与嵌入模型的关联
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
