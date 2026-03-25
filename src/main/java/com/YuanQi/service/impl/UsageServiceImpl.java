package com.YuanQi.service.impl;

import com.YuanQi.mapper.UsageMapper;
import com.YuanQi.pojo.vo.UsageVO;
import com.YuanQi.service.UsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 用量统计服务实现
 */
@Service
@RequiredArgsConstructor
public class UsageServiceImpl implements UsageService {

    private final UsageMapper usageMapper;

    /**
     * 查询用量统计
     */
    @Override
    public UsageVO getUsage(Long userId, LocalDate startDate, LocalDate endDate) {
        return usageMapper.selectUsage(userId, startDate, endDate);
    }
}
