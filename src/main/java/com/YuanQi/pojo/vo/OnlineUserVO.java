package com.YuanQi.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 在线用户VO
 */
@Data
public class OnlineUserVO implements Serializable {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 角色
     */
    private String role;

    /**
     * 终端索引
     */
    private Integer index;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * Token值
     */
    private String tokenValue;

    /**
     * 登录时间
     */
    private Date createTime;

    /**
     * 设备ID
     */
    private String deviceId;
}
