package com.YuanQi.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_base")
public class KnowledgeBase extends BaseEntity {

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 用户名（展示用，非数据库字段）
     */
    @TableField(exist = false)
    private String username;

    /**
     * 知识库名称
     */
    @NotBlank(message = "知识库名称不能为空")
    @TableField("name")
    private String name;

    /**
     * 知识库描述
     */
    @TableField("description")
    private String description;

    /**
     * 文档URL地址
     */
    @NotBlank(message = "文档URL不能为空")
    @TableField("document_url")
    private String documentUrl;

    /**
     * 分块ID列表（逗号分隔）
     */
    @TableField("chunk_ids")
    private String chunkIds;

    /**
     * 分块数量
     */
    @TableField("chunk_count")
    private Integer chunkCount;

    /**
     * 状态：0-处理中 1-可用 2-失败
     */
    @TableField("status")
    private Integer status;
}
