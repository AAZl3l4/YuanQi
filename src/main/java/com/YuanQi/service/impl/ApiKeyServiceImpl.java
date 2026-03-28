package com.YuanQi.service.impl;

import cn.hutool.core.util.IdUtil;
import com.YuanQi.mapper.ApiKeyMapper;
import com.YuanQi.pojo.ApiKey;
import com.YuanQi.service.ApiKeyService;
import com.YuanQi.service.ApiRelayConfigService;
import com.YuanQi.utils.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * API Key服务实现
 */
@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl extends ServiceImpl<ApiKeyMapper, ApiKey> implements ApiKeyService {

    private final ApiRelayConfigService apiRelayConfigService;

    /**
     * 创建API Key
     */
    @Override
    public ApiKey create(ApiKey apiKey) {
        // 验证配置存在且用户有权限使用
        apiRelayConfigService.getConfigById(apiKey.getConfigId());
        // 生成UUID作为API Key
        apiKey.setApiKey(IdUtil.simpleUUID());
        apiKey.setStatus(1);
        save(apiKey);
        return apiKey;
    }

    /**
     * 分页查询API Key
     */
    @Override
    public IPage<ApiKey> pageList(Integer page, Integer size, Long userId) {
        Page<ApiKey> pageParam = new Page<>(page, size);
        return baseMapper.selectPageWithUsername(pageParam, userId);
    }

    /**
     * 删除自己的API Key
     */
    @Override
    public void deleteMyKey(Long id, Long userId) {
        ApiKey apiKey = getById(id);
        if (apiKey == null) {
            throw new BusinessException("API Key不存在");
        }
        if (!apiKey.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此API Key");
        }
        removeById(id);
    }

    /**
     * 根据API Key字符串验证并返回
     */
    @Override
    public ApiKey validateAndGet(String apiKey) {
        ApiKey key = getOne(new LambdaQueryWrapper<ApiKey>()
                .eq(ApiKey::getApiKey, apiKey)
                .eq(ApiKey::getDeleted, 0));

        if (key == null) {
            throw new BusinessException("无效的API Key");
        }
        if (key.getStatus() != 1) {
            throw new BusinessException("API Key已禁用");
        }
        if (key.getExpireTime() != null && key.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("API Key已过期");
        }
        return key;
    }
}
