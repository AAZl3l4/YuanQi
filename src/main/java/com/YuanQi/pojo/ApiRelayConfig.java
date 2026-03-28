package com.YuanQi.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API中转配置实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("api_relay_config")
public class ApiRelayConfig extends BaseEntity {

    /**
     * 创建者ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 创建者用户名（展示用，非数据库字段）
     */
    @TableField(exist = false)
    private String username;

    /**
     * 配置名称
     */
    @NotBlank(message = "配置名称不能为空")
    @TableField("name")
    private String name;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 人设/风格提示词
     */
    @NotBlank(message = "人设提示词不能为空")
    @TableField("persona_prompt")
    private String personaPrompt;

    /**
     * 是否公开：0-私有 1-公开
     */
    @TableField("is_public")
    private Integer isPublic;
}
