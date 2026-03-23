package com.YuanQi.pojo;

import com.YuanQi.configuration.CryptoTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 加密密码（自动加密存储，查询自动解密）
     */
    @TableField(value = "password", typeHandler = CryptoTypeHandler.class)
    private String password;

    /**
     * 头像URL
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 角色：admin-管理员 user-普通用户
     */
    @TableField("role")
    private String role;

    /**
     * 状态：0-禁用 1-正常
     */
    @TableField("status")
    private Integer status;

    /**
     * 智谱AI API Key（自动加密存储，查询自动解密）
     */
    @TableField(value = "api_key", typeHandler = CryptoTypeHandler.class)
    private String apiKey;

    /**
     * 文字聊天模型
     */
    @TableField("chat_model")
    private String chatModel;

    /**
     * 图文聊天模型
     */
    @TableField("chat_vision_model")
    private String chatVisionModel;

    /**
     * 生图模型
     */
    @TableField("image_model")
    private String imageModel;

    /**
     * 生视频模型
     */
    @TableField("video_model")
    private String videoModel;
}