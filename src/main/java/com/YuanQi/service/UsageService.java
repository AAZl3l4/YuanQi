package com.YuanQi.service;

import com.YuanQi.pojo.vo.UsageVO;

import java.time.LocalDate;

/**
 * 用量统计服务接口
 */
public interface UsageService {

    /**
     * 查询用量统计
     * @param userId 用户ID（null表示查全部用户）
     * @param startDate 开始日期
     * @param endDate 结束日期
     */
    UsageVO getUsage(Long userId, LocalDate startDate, LocalDate endDate);
}
