package com.YuanQi.service;

import com.YuanQi.pojo.ApiRelayLog;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * API中转服务接口
 */
public interface ApiRelayService {

    /**
     * 调用AI并返回结果
     * @param apiKey API Key字符串
     * @param message 输入消息
     * @param imageUrl 图片URL（可选）
     * @return AI回复内容
     */
    String call(String apiKey, String message, String imageUrl);

    /**
     * 分页查询调用记录
     */
    IPage<ApiRelayLog> pageList(Integer page, Integer size, Long userId);
}
