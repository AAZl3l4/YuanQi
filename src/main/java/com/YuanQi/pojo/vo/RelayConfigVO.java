package com.YuanQi.pojo.vo;

import com.YuanQi.pojo.ApiRelayConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 中转配置VO（继承ApiRelayConfig，增加统计字段）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RelayConfigVO extends ApiRelayConfig {

    /**
     * 公开数量（统计）
     */
    private Long publicCount;

    /**
     * 私有数量（统计）
     */
    private Long privateCount;
}
