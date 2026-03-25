package com.YuanQi.mapper;

import com.YuanQi.pojo.vo.UsageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 用量统计Mapper
 */
@Mapper
public interface UsageMapper {

    /**
     * 查询用量统计
     * @param userId 用户ID（null表示查全部用户）
     * @param startDate 开始日期
     * @param endDate 结束日期
     */
    UsageVO selectUsage(@Param("userId") Long userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);
}
