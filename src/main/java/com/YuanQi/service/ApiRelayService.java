package com.YuanQi.service;

import com.YuanQi.pojo.ApiRelayLog;
import com.YuanQi.pojo.dto.RelayChatDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * API中转服务接口
 */
public interface ApiRelayService {

    /**
     * 调用AI并返回结果（支持上下文）
     * @param apiKey API Key字符串
     * @param chatDTO 聊天请求DTO
     * @return AI回复内容
     */
    String call(String apiKey, RelayChatDTO chatDTO);

    /**
     * 分页查询调用记录
     * @param page 页码
     * @param size 每页大小
     * @param userId 用户ID（可选）
     * @param sender 发送者标识（可选）
     * @param configId 配置ID（可选）
     */
    IPage<ApiRelayLog> pageList(Integer page, Integer size, Long userId, String sender, Long configId);
}
