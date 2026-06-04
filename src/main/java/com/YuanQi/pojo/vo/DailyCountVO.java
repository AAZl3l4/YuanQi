package com.YuanQi.pojo.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 每日统计VO
 */
@Data
public class DailyCountVO {
    // 日期
    private LocalDate date;
    // 数量
    private Long count;
}
