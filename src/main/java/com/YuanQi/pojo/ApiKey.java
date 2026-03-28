package com.YuanQi.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * API Key实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("api_key")
public class ApiKey extends BaseEntity {

    /**
     * 所属用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 所属用户名（展示用，非数据库字段）
     */
    @TableField(exist = false)
    private String username;

    /**
     * 绑定的配置ID
     */
    @NotNull(message = "配置ID不能为空")
    @TableField("config_id")
    private Long configId;

    /**
     * 配置名称（展示用，非数据库字段）
     */
    @TableField(exist = false)
    private String configName;

    /**
     * Key名称（备注）
     */
    @NotBlank(message = "Key名称不能为空")
    @TableField("key_name")
    private String keyName;

    /**
     * API Key（UUID）
     */
    @TableField("api_key")
    private String apiKey;

    /**
     * 过期时间（NULL永不过期）
     */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /**
     * 状态：0-禁用 1-启用
     */
    @TableField("status")
    private Integer status;
}
