package com.YuanQi.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI生成内容记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_generated_content")
public class GeneratedContent extends BaseEntity {

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
     * 类型：image-图片 video-视频
     */
    @TableField("type")
    private String type;

    /**
     * 提示词
     */
    @TableField("prompt")
    private String prompt;

    /**
     * 生成结果URL（图片/视频）
     */
    @TableField("result_url")
    private String resultUrl;

    /**
     * 封面图URL（视频专用）
     */
    @TableField("cover_url")
    private String coverUrl;

    /**
     * 异步任务ID（视频专用）
     */
    @TableField("task_id")
    private String taskId;

    /**
     * 状态：0-处理中 1-成功 2-失败
     */
    @TableField("status")
    private Integer status;

    /**
     * 错误信息（失败时）
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 使用的模型
     */
    @TableField("model_used")
    private String modelUsed;
}
