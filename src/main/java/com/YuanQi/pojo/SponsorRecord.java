package com.YuanQi.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 赞助记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sponsor_record")
public class SponsorRecord extends BaseEntity {

    /**
     * 赞助者名称
     */
    @TableField("name")
    private String name;

    /**
     * 广告位展示内容
     */
    @TableField("ad_content")
    private String adContent;

    /**
     * 赞助金额(元)
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
}
