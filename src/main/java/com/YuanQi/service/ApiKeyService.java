package com.YuanQi.service;

import com.YuanQi.pojo.ApiKey;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * API Key服务接口
 */
public interface ApiKeyService extends IService<ApiKey> {

    /**
     * 创建API Key
     */
    ApiKey create(ApiKey apiKey);

    /**
     * 分页查询API Key
     * @param userId 用户ID（null查全部）
     */
    IPage<ApiKey> pageList(Integer page, Integer size, Long userId);

    /**
     * 删除自己的API Key
     */
    void deleteMyKey(Long id, Long userId);

    /**
     * 根据API Key字符串验证并返回
     */
    ApiKey validateAndGet(String apiKey);
}
