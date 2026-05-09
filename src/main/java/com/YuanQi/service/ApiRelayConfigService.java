package com.YuanQi.service;

import com.YuanQi.pojo.ApiRelayConfig;
import com.YuanQi.pojo.vo.RelayConfigVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * API中转配置服务接口
 */
public interface ApiRelayConfigService extends IService<ApiRelayConfig> {

    /**
     * 创建配置
     */
    ApiRelayConfig create(ApiRelayConfig config);

    /**
     * 更新配置
     */
    ApiRelayConfig update(ApiRelayConfig config);

    /**
     * 分页查询配置（带统计信息）
     * @param userId 指定用户ID
     * @param onlyMine 只看自己的
     * @param keyword 搜索关键词
     */
    IPage<RelayConfigVO> pageList(Integer page, Integer size, Long userId, Boolean onlyMine, Long id, String keyword);

    /**
     * 删除自己的配置
     */
    void deleteMyConfig(Long id);

    /**
     * 根据ID获取配置（校验权限）
     */
    ApiRelayConfig getConfigById(Long id);
}
