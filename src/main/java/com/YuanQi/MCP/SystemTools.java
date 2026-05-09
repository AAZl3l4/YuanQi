package com.YuanQi.MCP;

import com.YuanQi.mapper.*;
import com.YuanQi.pojo.*;
import com.YuanQi.pojo.vo.UsageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统管理工具集
 * 用于管理后台RAG问答，支持通过自然语言查询和操作系统数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemTools {

    private final UserMapper userMapper;
    private final UsageMapper usageMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final AgentMapper agentMapper;
    private final McpToolMapper mcpToolMapper;
    private final GeneratedContentMapper generatedContentMapper;
    private final ApiKeyMapper apiKeyMapper;
    private final ApiRelayConfigMapper apiRelayConfigMapper;
    private final ApiRelayLogMapper apiRelayLogMapper;

    /**
     * 查询用户统计
     */
    @Tool(description = "查询用户统计数据。当管理员询问用户数量、新用户、用户分布等问题时调用此工具。" +
            "支持查询：总用户数、今日新增、本周新增、本月新增、角色分布、状态分布。" +
            "如果不确定具体查询类型，直接调用此工具不传入参数，会返回全部统计数据。")
    public String queryUserStats(
            @ToolParam(description = "统计类型：total-总用户数 new-新增用户(今日/本周/本月) role-角色分布 status-状态分布 all-全部统计。如果不确定，不传此参数", required = false) String type) {
        log.debug("查询用户统计，类型: {}", type);
        try {
            Map<String, Object> result = new HashMap<>();

            // 当type为空或不明确时，返回全部统计数据
            boolean returnAll = type == null || type.isEmpty() || "all".equals(type);

            // 总用户数
            if (returnAll || "total".equals(type)) {
                Long total = userMapper.selectCount(
                        new LambdaQueryWrapper<User>()
                                .eq(User::getDeleted, 0));
                result.put("总用户数", total);
            }

            // 新增用户统计
            if (returnAll || "new".equals(type)) {
                Long todayCount = userMapper.selectCount(
                        new LambdaQueryWrapper<User>()
                                .eq(User::getDeleted, 0)
                                .ge(User::getCreateTime, LocalDate.now().atStartOfDay()));
                Long weekCount = userMapper.selectCount(
                        new LambdaQueryWrapper<User>()
                                .eq(User::getDeleted, 0)
                                .ge(User::getCreateTime, LocalDate.now().minusDays(7).atStartOfDay()));
                Long monthCount = userMapper.selectCount(
                        new LambdaQueryWrapper<User>()
                                .eq(User::getDeleted, 0)
                                .ge(User::getCreateTime, LocalDate.now().minusMonths(1).atStartOfDay()));
                result.put("今日新增", todayCount);
                result.put("本周新增", weekCount);
                result.put("本月新增", monthCount);
            }

            // 角色分布
            if (returnAll || "role".equals(type)) {
                Long adminCount = userMapper.selectCount(
                        new LambdaQueryWrapper<User>()
                                .eq(User::getDeleted, 0)
                                .eq(User::getRole, "admin"));
                Long userCount = userMapper.selectCount(
                        new LambdaQueryWrapper<User>()
                                .eq(User::getDeleted, 0)
                                .eq(User::getRole, "user"));
                result.put("管理员数量", adminCount);
                result.put("普通用户数量", userCount);
            }

            // 状态分布
            if (returnAll || "status".equals(type)) {
                Long activeCount = userMapper.selectCount(
                        new LambdaQueryWrapper<User>()
                                .eq(User::getDeleted, 0)
                                .eq(User::getStatus, 1));
                Long disabledCount = userMapper.selectCount(
                        new LambdaQueryWrapper<User>()
                                .eq(User::getDeleted, 0)
                                .eq(User::getStatus, 0));
                result.put("正常用户", activeCount);
                result.put("禁用用户", disabledCount);
            }

            return formatResult("用户统计", result);
        } catch (Exception e) {
            log.error("查询用户统计失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 查询用量统计
     */
    @Tool(description = "查询系统用量统计数据。当管理员询问Token消耗、API调用次数、用量趋势等问题时调用此工具。" +
            "支持查询：今日用量、本周用量、本月用量、总用量、全部统计。" +
            "如果不确定具体查询类型，直接调用此工具不传入参数，会返回全部统计数据。")
    public String queryUsageStats(
            @ToolParam(description = "时间范围：today-今日 week-本周 month-本月 total-总计 all-全部统计。如果不确定，不传此参数", required = false) String period) {
        log.debug("查询用量统计，时间范围: {}", period);
        try {
            Map<String, Object> result = new HashMap<>();

            // 当period为空或不明确时，返回全部统计数据
            boolean returnAll = period == null || period.isEmpty() || "all".equals(period);

            if (returnAll) {
                // 全部统计
                result.putAll(getUsageByPeriod("今日", LocalDate.now(), LocalDate.now()));
                result.putAll(getUsageByPeriod("本周", LocalDate.now().minusDays(7), LocalDate.now()));
                result.putAll(getUsageByPeriod("本月", LocalDate.now().minusMonths(1), LocalDate.now()));
                result.putAll(getUsageByPeriod("总计", null, LocalDate.now()));
            } else {
                LocalDate startDate = null;
                String periodName = "总计";

                if ("today".equals(period)) {
                    startDate = LocalDate.now();
                    periodName = "今日";
                } else if ("week".equals(period)) {
                    startDate = LocalDate.now().minusDays(7);
                    periodName = "本周";
                } else if ("month".equals(period)) {
                    startDate = LocalDate.now().minusMonths(1);
                    periodName = "本月";
                }

                result.putAll(getUsageByPeriod(periodName, startDate, LocalDate.now()));
            }

            return formatResult("用量统计", result);
        } catch (Exception e) {
            log.error("查询用量统计失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    private Map<String, Object> getUsageByPeriod(String prefix, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();
        UsageVO usage = usageMapper.selectUsage(null, startDate, endDate);

        result.put(prefix + "聊天次数", usage.getChatCount());
        result.put(prefix + "输入Token", usage.getInputTokens());
        result.put(prefix + "输出Token", usage.getOutputTokens());
        result.put(prefix + "生图次数", usage.getImageCount());
        result.put(prefix + "生视频次数", usage.getVideoCount());
        result.put(prefix + "API中转次数", usage.getRelayCount());
        result.put(prefix + "总Token消耗", usage.getInputTokens() + usage.getOutputTokens() +
                usage.getRelayInputTokens() + usage.getRelayOutputTokens());

        return result;
    }

    /**
     * 查询知识库统计
     */
    @Tool(description = "查询知识库统计数据。当管理员询问知识库数量、状态、处理情况等问题时调用此工具。" +
            "支持查询：总数、状态分布、新增统计。" +
            "如果不确定具体查询类型，直接调用此工具不传入参数，会返回全部统计数据。")
    public String queryKnowledgeBaseStats(
            @ToolParam(description = "统计类型：total-总数 status-状态分布 new-新增统计 all-全部统计。如果不确定，不传此参数", required = false) String type) {
        log.debug("查询知识库统计，类型: {}", type);
        try {
            Map<String, Object> result = new HashMap<>();

            // 当type为空或不明确时，返回全部统计数据
            boolean returnAll = type == null || type.isEmpty() || "all".equals(type);

            // 总数
            if (returnAll || "total".equals(type)) {
                Long total = knowledgeBaseMapper.selectCount(
                        new LambdaQueryWrapper<KnowledgeBase>()
                                .eq(KnowledgeBase::getDeleted, 0));
                result.put("知识库总数", total);
            }

            // 状态分布
            if (returnAll || "status".equals(type)) {
                Long processing = knowledgeBaseMapper.selectCount(
                        new LambdaQueryWrapper<KnowledgeBase>()
                                .eq(KnowledgeBase::getDeleted, 0)
                                .eq(KnowledgeBase::getStatus, 0));
                Long available = knowledgeBaseMapper.selectCount(
                        new LambdaQueryWrapper<KnowledgeBase>()
                                .eq(KnowledgeBase::getDeleted, 0)
                                .eq(KnowledgeBase::getStatus, 1));
                Long failed = knowledgeBaseMapper.selectCount(
                        new LambdaQueryWrapper<KnowledgeBase>()
                                .eq(KnowledgeBase::getDeleted, 0)
                                .eq(KnowledgeBase::getStatus, 2));
                result.put("处理中", processing);
                result.put("可用", available);
                result.put("失败", failed);
            }

            // 新增统计
            if (returnAll || "new".equals(type)) {
                Long todayCount = knowledgeBaseMapper.selectCount(
                        new LambdaQueryWrapper<KnowledgeBase>()
                                .eq(KnowledgeBase::getDeleted, 0)
                                .ge(KnowledgeBase::getCreateTime, LocalDate.now().atStartOfDay()));
                Long weekCount = knowledgeBaseMapper.selectCount(
                        new LambdaQueryWrapper<KnowledgeBase>()
                                .eq(KnowledgeBase::getDeleted, 0)
                                .ge(KnowledgeBase::getCreateTime, LocalDate.now().minusDays(7).atStartOfDay()));
                Long monthCount = knowledgeBaseMapper.selectCount(
                        new LambdaQueryWrapper<KnowledgeBase>()
                                .eq(KnowledgeBase::getDeleted, 0)
                                .ge(KnowledgeBase::getCreateTime, LocalDate.now().minusMonths(1).atStartOfDay()));
                result.put("今日新增", todayCount);
                result.put("本周新增", weekCount);
                result.put("本月新增", monthCount);
            }

            return formatResult("知识库统计", result);
        } catch (Exception e) {
            log.error("查询知识库统计失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 查询智能体统计
     */
    @Tool(description = "查询智能体统计数据。当管理员询问智能体数量、公开/私有分布等问题时调用此工具。" +
            "支持查询：总数、公开/私有分布、新增统计。" +
            "如果不确定具体查询类型，直接调用此工具不传入参数，会返回全部统计数据。")
    public String queryAgentStats(
            @ToolParam(description = "统计类型：total-总数 public-公开/私有分布 new-新增统计 all-全部统计。如果不确定，不传此参数", required = false) String type) {
        log.debug("查询智能体统计，类型: {}", type);
        try {
            Map<String, Object> result = new HashMap<>();

            // 当type为空或不明确时，返回全部统计数据
            boolean returnAll = type == null || type.isEmpty() || "all".equals(type);

            // 总数
            if (returnAll || "total".equals(type)) {
                Long total = agentMapper.selectCount(
                        new LambdaQueryWrapper<Agent>()
                                .eq(Agent::getDeleted, 0));
                result.put("智能体总数", total);
            }

            // 公开/私有分布
            if (returnAll || "public".equals(type)) {
                Long publicCount = agentMapper.selectCount(
                        new LambdaQueryWrapper<Agent>()
                                .eq(Agent::getDeleted, 0)
                                .eq(Agent::getIsPublic, 1));
                Long privateCount = agentMapper.selectCount(
                        new LambdaQueryWrapper<Agent>()
                                .eq(Agent::getDeleted, 0)
                                .eq(Agent::getIsPublic, 0));
                result.put("公开智能体", publicCount);
                result.put("私有智能体", privateCount);
            }

            // 新增统计
            if (returnAll || "new".equals(type)) {
                Long todayCount = agentMapper.selectCount(
                        new LambdaQueryWrapper<Agent>()
                                .eq(Agent::getDeleted, 0)
                                .ge(Agent::getCreateTime, LocalDate.now().atStartOfDay()));
                Long weekCount = agentMapper.selectCount(
                        new LambdaQueryWrapper<Agent>()
                                .eq(Agent::getDeleted, 0)
                                .ge(Agent::getCreateTime, LocalDate.now().minusDays(7).atStartOfDay()));
                Long monthCount = agentMapper.selectCount(
                        new LambdaQueryWrapper<Agent>()
                                .eq(Agent::getDeleted, 0)
                                .ge(Agent::getCreateTime, LocalDate.now().minusMonths(1).atStartOfDay()));
                result.put("今日新增", todayCount);
                result.put("本周新增", weekCount);
                result.put("本月新增", monthCount);
            }

            return formatResult("智能体统计", result);
        } catch (Exception e) {
            log.error("查询智能体统计失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 查询API Key统计
     */
    @Tool(description = "查询API Key统计数据。当管理员询问API Key数量、状态分布等问题时调用此工具。" +
            "支持查询：总数、状态分布(启用/禁用)、今日/本周/本月新增。" +
            "如果不确定具体查询类型，直接调用此工具不传入参数，会返回全部统计数据。")
    public String queryApiKeyStats(
            @ToolParam(description = "统计类型：total-总数 status-状态分布 new-新增统计 all-全部统计。如果不确定，不传此参数", required = false) String type) {
        log.debug("查询API Key统计，类型: {}", type);
        try {
            Map<String, Object> result = new HashMap<>();

            // 当type为空或不明确时，返回全部统计数据
            boolean returnAll = type == null || type.isEmpty() || "all".equals(type);

            // 总数
            if (returnAll || "total".equals(type)) {
                Long total = apiKeyMapper.selectCount(
                        new LambdaQueryWrapper<ApiKey>()
                                .eq(ApiKey::getDeleted, 0));
                result.put("API Key总数", total);
            }

            // 状态分布
            if (returnAll || "status".equals(type)) {
                Long enabledCount = apiKeyMapper.selectCount(
                        new LambdaQueryWrapper<ApiKey>()
                                .eq(ApiKey::getDeleted, 0)
                                .eq(ApiKey::getStatus, 1));
                Long disabledCount = apiKeyMapper.selectCount(
                        new LambdaQueryWrapper<ApiKey>()
                                .eq(ApiKey::getDeleted, 0)
                                .eq(ApiKey::getStatus, 0));
                result.put("已启用", enabledCount);
                result.put("已禁用", disabledCount);
            }

            // 新增统计
            if (returnAll || "new".equals(type)) {
                Long todayCount = apiKeyMapper.selectCount(
                        new LambdaQueryWrapper<ApiKey>()
                                .eq(ApiKey::getDeleted, 0)
                                .ge(ApiKey::getCreateTime, LocalDate.now().atStartOfDay()));
                Long weekCount = apiKeyMapper.selectCount(
                        new LambdaQueryWrapper<ApiKey>()
                                .eq(ApiKey::getDeleted, 0)
                                .ge(ApiKey::getCreateTime, LocalDate.now().minusDays(7).atStartOfDay()));
                Long monthCount = apiKeyMapper.selectCount(
                        new LambdaQueryWrapper<ApiKey>()
                                .eq(ApiKey::getDeleted, 0)
                                .ge(ApiKey::getCreateTime, LocalDate.now().minusMonths(1).atStartOfDay()));
                result.put("今日新增", todayCount);
                result.put("本周新增", weekCount);
                result.put("本月新增", monthCount);
            }

            return formatResult("API Key统计", result);
        } catch (Exception e) {
            log.error("查询API Key统计失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 查询API中转配置统计
     */
    @Tool(description = "查询API中转配置统计数据。当管理员询问中转配置数量、公开/私有分布等问题时调用此工具。" +
            "支持查询：总数、公开/私有分布、今日/本周/本月新增。" +
            "如果不确定具体查询类型，直接调用此工具不传入参数，会返回全部统计数据。")
    public String queryRelayConfigStats(
            @ToolParam(description = "统计类型：total-总数 public-公开/私有分布 new-新增统计 all-全部统计。如果不确定，不传此参数", required = false) String type) {
        log.debug("查询API中转配置统计，类型: {}", type);
        try {
            Map<String, Object> result = new HashMap<>();

            // 当type为空或不明确时，返回全部统计数据
            boolean returnAll = type == null || type.isEmpty() || "all".equals(type);

            // 总数
            if (returnAll || "total".equals(type)) {
                Long total = apiRelayConfigMapper.selectCount(
                        new LambdaQueryWrapper<ApiRelayConfig>()
                                .eq(ApiRelayConfig::getDeleted, 0));
                result.put("中转配置总数", total);
            }

            // 公开/私有分布
            if (returnAll || "public".equals(type)) {
                Long publicCount = apiRelayConfigMapper.selectCount(
                        new LambdaQueryWrapper<ApiRelayConfig>()
                                .eq(ApiRelayConfig::getDeleted, 0)
                                .eq(ApiRelayConfig::getIsPublic, 1));
                Long privateCount = apiRelayConfigMapper.selectCount(
                        new LambdaQueryWrapper<ApiRelayConfig>()
                                .eq(ApiRelayConfig::getDeleted, 0)
                                .eq(ApiRelayConfig::getIsPublic, 0));
                result.put("公开配置", publicCount);
                result.put("私有配置", privateCount);
            }

            // 新增统计
            if (returnAll || "new".equals(type)) {
                Long todayCount = apiRelayConfigMapper.selectCount(
                        new LambdaQueryWrapper<ApiRelayConfig>()
                                .eq(ApiRelayConfig::getDeleted, 0)
                                .ge(ApiRelayConfig::getCreateTime, LocalDate.now().atStartOfDay()));
                Long weekCount = apiRelayConfigMapper.selectCount(
                        new LambdaQueryWrapper<ApiRelayConfig>()
                                .eq(ApiRelayConfig::getDeleted, 0)
                                .ge(ApiRelayConfig::getCreateTime, LocalDate.now().minusDays(7).atStartOfDay()));
                Long monthCount = apiRelayConfigMapper.selectCount(
                        new LambdaQueryWrapper<ApiRelayConfig>()
                                .eq(ApiRelayConfig::getDeleted, 0)
                                .ge(ApiRelayConfig::getCreateTime, LocalDate.now().minusMonths(1).atStartOfDay()));
                result.put("今日新增", todayCount);
                result.put("本周新增", weekCount);
                result.put("本月新增", monthCount);
            }

            return formatResult("API中转配置统计", result);
        } catch (Exception e) {
            log.error("查询API中转配置统计失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 查询API中转调用记录统计
     */
    @Tool(description = "查询API中转调用记录统计。当管理员询问API调用次数、Token消耗等问题时调用此工具。" +
            "支持查询：今日、本周、本月、总计、全部统计。" +
            "如果不确定具体查询类型，直接调用此工具不传入参数，会返回全部统计数据。")
    public String queryRelayLogStats(
            @ToolParam(description = "时间范围：today-今日 week-本周 month-本月 total-总计 all-全部统计。如果不确定，不传此参数", required = false) String period) {
        log.debug("查询API中转调用记录统计，时间范围: {}", period);
        try {
            Map<String, Object> result = new HashMap<>();

            // 当period为空或不明确时，返回全部统计数据
            boolean returnAll = period == null || period.isEmpty() || "all".equals(period);

            if (returnAll) {
                result.putAll(getRelayLogStatsByPeriod("今日", LocalDate.now()));
                result.putAll(getRelayLogStatsByPeriod("本周", LocalDate.now().minusDays(7)));
                result.putAll(getRelayLogStatsByPeriod("本月", LocalDate.now().minusMonths(1)));
                result.putAll(getRelayLogStatsByPeriod("总计", null));
            } else {
                LocalDate startDate = null;
                String periodName = "总计";

                if ("today".equals(period)) {
                    startDate = LocalDate.now();
                    periodName = "今日";
                } else if ("week".equals(period)) {
                    startDate = LocalDate.now().minusDays(7);
                    periodName = "本周";
                } else if ("month".equals(period)) {
                    startDate = LocalDate.now().minusMonths(1);
                    periodName = "本月";
                }

                result.putAll(getRelayLogStatsByPeriod(periodName, startDate));
            }

            return formatResult("API中转调用统计", result);
        } catch (Exception e) {
            log.error("查询API中转调用记录统计失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    private Map<String, Object> getRelayLogStatsByPeriod(String prefix, LocalDate startDate) {
        Map<String, Object> result = new HashMap<>();

        // 调用次数
        Long callCount = apiRelayLogMapper.selectCount(
                new LambdaQueryWrapper<com.YuanQi.pojo.ApiRelayLog>()
                        .eq(com.YuanQi.pojo.ApiRelayLog::getDeleted, 0)
                        .ge(startDate != null, com.YuanQi.pojo.ApiRelayLog::getCreateTime,
                                startDate != null ? startDate.atStartOfDay() : null));

        // 输入Token
        List<com.YuanQi.pojo.ApiRelayLog> logs = apiRelayLogMapper.selectList(
                new LambdaQueryWrapper<com.YuanQi.pojo.ApiRelayLog>()
                        .eq(com.YuanQi.pojo.ApiRelayLog::getDeleted, 0)
                        .ge(startDate != null, com.YuanQi.pojo.ApiRelayLog::getCreateTime,
                                startDate != null ? startDate.atStartOfDay() : null)
                        .select(com.YuanQi.pojo.ApiRelayLog::getInputTokens, com.YuanQi.pojo.ApiRelayLog::getOutputTokens));

        int totalInputTokens = logs.stream().mapToInt(log -> log.getInputTokens() != null ? log.getInputTokens() : 0).sum();
        int totalOutputTokens = logs.stream().mapToInt(log -> log.getOutputTokens() != null ? log.getOutputTokens() : 0).sum();

        result.put(prefix + "调用次数", callCount);
        result.put(prefix + "输入Token", totalInputTokens);
        result.put(prefix + "输出Token", totalOutputTokens);
        result.put(prefix + "总Token消耗", totalInputTokens + totalOutputTokens);

        return result;
    }

    /**
     * 查询MCP工具状态
     */
    @Tool(description = "查询MCP工具状态。当管理员询问有哪些工具、工具是否启用等问题时调用此工具。")
    public String queryMcpToolStats() {
        log.debug("查询MCP工具状态");
        try {
            List<McpTool> tools = mcpToolMapper.selectList(
                    new LambdaQueryWrapper<McpTool>()
                            .eq(McpTool::getDeleted, 0)
                            .orderByAsc(McpTool::getSortOrder));

            StringBuilder sb = new StringBuilder("【MCP工具状态】\n\n");
            for (McpTool tool : tools) {
                sb.append("- ").append(tool.getName())
                        .append(": ").append(tool.getDescription())
                        .append(" (").append(tool.getEnabled() == 1 ? "已启用" : "已禁用").append(")\n");
            }

            Long enabledCount = tools.stream().filter(t -> t.getEnabled() == 1).count();
            Long disabledCount = tools.stream().filter(t -> t.getEnabled() == 0).count();
            sb.append("\n启用: ").append(enabledCount).append("个，禁用: ").append(disabledCount).append("个");

            return sb.toString();
        } catch (Exception e) {
            log.error("查询MCP工具状态失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 切换MCP工具状态
     */
    @Tool(description = "启用或禁用MCP工具。当管理员要求启用/禁用某个工具时调用此工具。" +
            "操作前请确认管理员的意图，工具名称必须精确匹配。")
    public String toggleMcpTool(
            @ToolParam(description = "工具名称，如：webSearch、getWeather、getRandomQuote、searchMusic") String toolName,
            @ToolParam(description = "目标状态：enable-启用 disable-禁用") String action) {
        log.debug("切换MCP工具状态，工具: {}, 操作: {}", toolName, action);
        try {
            McpTool tool = mcpToolMapper.selectOne(
                    new LambdaQueryWrapper<McpTool>()
                            .eq(McpTool::getName, toolName)
                            .eq(McpTool::getDeleted, 0));

            if (tool == null) {
                return "工具不存在: " + toolName + "。可用工具：webSearch、getWeather、getRandomQuote、searchMusic";
            }

            int newStatus = "enable".equals(action) ? 1 : 0;
            String actionText = newStatus == 1 ? "启用" : "禁用";

            if (tool.getEnabled() == newStatus) {
                return "工具 " + toolName + " 已经是" + actionText + "状态";
            }

            mcpToolMapper.update(null, new LambdaUpdateWrapper<McpTool>()
                    .eq(McpTool::getId, tool.getId())
                    .set(McpTool::getEnabled, newStatus));

            return "已成功" + actionText + "工具: " + toolName + " (" + tool.getDescription() + ")";
        } catch (Exception e) {
            log.error("切换MCP工具状态失败", e);
            return "操作失败: " + e.getMessage();
        }
    }

    /**
     * 查询生成内容统计
     */
    @Tool(description = "查询AI生成内容统计。当管理员询问生图、生视频数量等问题时调用此工具。" +
            "支持查询：今日、本周、本月、总计、全部统计。" +
            "如果不确定具体查询类型，直接调用此工具不传入参数，会返回全部统计数据。")
    public String queryGeneratedContentStats(
            @ToolParam(description = "时间范围：today-今日 week-本周 month-本月 total-总计 all-全部统计。如果不确定，不传此参数", required = false) String period) {
        log.debug("查询生成内容统计，时间范围: {}", period);
        try {
            Map<String, Object> result = new HashMap<>();

            // 当period为空或不明确时，返回全部统计数据
            boolean returnAll = period == null || period.isEmpty() || "all".equals(period);

            if (returnAll) {
                result.putAll(getContentStatsByPeriod("今日", LocalDate.now()));
                result.putAll(getContentStatsByPeriod("本周", LocalDate.now().minusDays(7)));
                result.putAll(getContentStatsByPeriod("本月", LocalDate.now().minusMonths(1)));
                result.putAll(getContentStatsByPeriod("总计", null));
            } else {
                LocalDate startDate = null;
                String periodName = "总计";

                if ("today".equals(period)) {
                    startDate = LocalDate.now();
                    periodName = "今日";
                } else if ("week".equals(period)) {
                    startDate = LocalDate.now().minusDays(7);
                    periodName = "本周";
                } else if ("month".equals(period)) {
                    startDate = LocalDate.now().minusMonths(1);
                    periodName = "本月";
                }

                result.putAll(getContentStatsByPeriod(periodName, startDate));
            }

            return formatResult("生成内容统计", result);
        } catch (Exception e) {
            log.error("查询生成内容统计失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    private Map<String, Object> getContentStatsByPeriod(String prefix, LocalDate startDate) {
        Map<String, Object> result = new HashMap<>();

        Long imageCount = generatedContentMapper.selectCount(
                new LambdaQueryWrapper<com.YuanQi.pojo.GeneratedContent>()
                        .eq(com.YuanQi.pojo.GeneratedContent::getDeleted, 0)
                        .eq(com.YuanQi.pojo.GeneratedContent::getStatus, 1)
                        .eq(com.YuanQi.pojo.GeneratedContent::getType, "image")
                        .ge(startDate != null, com.YuanQi.pojo.GeneratedContent::getCreateTime,
                                startDate != null ? startDate.atStartOfDay() : null));

        Long videoCount = generatedContentMapper.selectCount(
                new LambdaQueryWrapper<com.YuanQi.pojo.GeneratedContent>()
                        .eq(com.YuanQi.pojo.GeneratedContent::getDeleted, 0)
                        .eq(com.YuanQi.pojo.GeneratedContent::getStatus, 1)
                        .eq(com.YuanQi.pojo.GeneratedContent::getType, "video")
                        .ge(startDate != null, com.YuanQi.pojo.GeneratedContent::getCreateTime,
                                startDate != null ? startDate.atStartOfDay() : null));

        result.put(prefix + "生成图片", imageCount);
        result.put(prefix + "生成视频", videoCount);
        result.put(prefix + "总生成数", imageCount + videoCount);

        return result;
    }

    /**
     * 格式化结果
     */
    private String formatResult(String title, Map<String, Object> data) {
        StringBuilder sb = new StringBuilder("【").append(title).append("】\n\n");
        data.forEach((key, value) -> sb.append("- ").append(key).append(": ").append(value).append("\n"));
        return sb.toString();
    }
}
