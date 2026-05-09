package com.YuanQi.pojo.vo;

import com.YuanQi.pojo.Agent;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 智能体VO（继承Agent，增加统计字段）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentVO extends Agent {

    /**
     * 公开数量（统计）
     */
    private Long publicCount;

    /**
     * 私有数量（统计）
     */
    private Long privateCount;
}
