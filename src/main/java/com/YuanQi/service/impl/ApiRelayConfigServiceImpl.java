package com.YuanQi.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.YuanQi.mapper.ApiRelayConfigMapper;
import com.YuanQi.pojo.ApiRelayConfig;
import com.YuanQi.service.ApiRelayConfigService;
import com.YuanQi.utils.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * API中转配置服务实现
 */
@Service
@RequiredArgsConstructor
public class ApiRelayConfigServiceImpl extends ServiceImpl<ApiRelayConfigMapper, ApiRelayConfig> implements ApiRelayConfigService {

    /**
     * 创建配置
     */
    @Override
    public ApiRelayConfig create(ApiRelayConfig config) {
        config.setIsPublic(config.getIsPublic() == null ? 0 : config.getIsPublic());
        save(config);
        return config;
    }

    /**
     * 更新配置
     */
    @Override
    public ApiRelayConfig update(ApiRelayConfig config) {
        Long userId = StpUtil.getLoginIdAsLong();
        ApiRelayConfig existing = getById(config.getId());
        if (existing == null) {
            throw new BusinessException("配置不存在");
        }
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此配置");
        }
        updateById(config);
        return config;
    }

    /**
     * 分页查询配置
     */
    @Override
    public IPage<ApiRelayConfig> pageList(Integer page, Integer size, Long userId, Boolean onlyMine, Long id) {
        Page<ApiRelayConfig> pageParam = new Page<>(page, size);
        return baseMapper.selectPageWithUsername(pageParam, userId, onlyMine, id);
    }

    /**
     * 删除自己的配置
     */
    @Override
    public void deleteMyConfig(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        ApiRelayConfig config = getById(id);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }
        if (!config.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此配置");
        }
        removeById(id);
    }

    /**
     * 根据ID获取配置（校验权限）
     */
    @Override
    public ApiRelayConfig getConfigById(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        ApiRelayConfig config = getById(id);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }
        // 只能查看自己的或公开的
        if (!config.getUserId().equals(userId) && config.getIsPublic() != 1) {
            throw new BusinessException("无权查看此配置");
        }
        return config;
    }
}
