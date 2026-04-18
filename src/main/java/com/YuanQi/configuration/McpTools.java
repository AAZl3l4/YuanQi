package com.YuanQi.configuration;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * MCP工具集
 * 平台提供的工具，供AI模型调用
 */
@Slf4j
@Component
public class McpTools {

    private List<ToolCallback> allToolCallbacks;

    /**
     * 初始化所有工具回调 这里注册成bean也行
     */
    @PostConstruct
    public void init() {
        MethodToolCallbackProvider provider = MethodToolCallbackProvider.builder()
                .toolObjects(this)
                .build();
        this.allToolCallbacks = List.of(provider.getToolCallbacks());
        log.info("初始化MCP工具: {}", allToolCallbacks.stream()
                .map(c -> c.getToolDefinition().name()).toList());
    }

    /**
     * 获取所有工具回调
     */
    public List<ToolCallback> getAllToolCallbacks() {
        return allToolCallbacks;
    }

    /**
     * 天气查询工具
     */
    @Tool(description = "查询指定城市的实时天气信息。当用户询问天气、气温、下雨、穿什么衣服等与天气相关的问题时调用此工具。")
    public String getWeather(@ToolParam(description = "城市名称，如：北京、上海、广州") String city) {
        log.info("调用天气查询工具，城市: {}", city);
        try {
            String url = "https://uapis.cn/api/v1/misc/weather?city=" + 
                    URLEncoder.encode(city, StandardCharsets.UTF_8);
            
            HttpResponse response = HttpRequest.get(url)
                    .timeout(10000)
                    .execute();
            
            if (!response.isOk()) {
                return "天气查询失败，请稍后重试";
            }
            
            JSONObject json = JSONUtil.parseObj(response.body());
            if (json.isEmpty()) {
                return "未找到该城市的天气信息";
            }
            
            return String.format("%s %s 的天气情况：\n" +
                    "天气: %s\n" +
                    "温度: %s℃\n" +
                    "风向: %s\n" +
                    "风力: %s\n" +
                    "湿度: %s%%",
                    json.getStr("province"),
                    json.getStr("city"),
                    json.getStr("weather"),
                    json.getInt("temperature"),
                    json.getStr("wind_direction"),
                    json.getStr("wind_power"),
                    json.getInt("humidity"));
        } catch (Exception e) {
            log.error("天气查询失败", e);
            return "天气查询失败: " + e.getMessage();
        }
    }

    /**
     * 随机一言工具
     */
    @Tool(description = "获取随机一言或随机诗句。当用户想要一句励志的话、名言警句、诗词、或者想看点有意思的句子时调用此工具。")
    public String getRandomQuote() {
        log.info("调用随机一言工具");
        try {
            HttpResponse response = HttpRequest.get("https://v1.hitokoto.cn/?c=i")
                    .timeout(10000)
                    .execute();
            
            if (!response.isOk()) {
                return "获取一言失败，请稍后重试";
            }
            
            JSONObject json = JSONUtil.parseObj(response.body());
            String hitokoto = json.getStr("hitokoto");
            String from = json.getStr("from");
            String fromWho = json.getStr("from_who");
            
            return String.format("%s\n—— %s《%s》", hitokoto, fromWho, from);
        } catch (Exception e) {
            log.error("获取一言失败", e);
            return "获取一言失败: " + e.getMessage();
        }
    }

    /**
     * 联网搜索工具
     */
    @Tool(description = "联网搜索实时信息。当用户询问以下类型问题时必须调用此工具：1.最新新闻、热点事件 2.实时数据 3.近期发生的事情 4.你不确定或不知道的信息 5.需要联网才能回答的问题")
    public String webSearch(@ToolParam(description = "搜索关键词，提取用户问题中的核心关键词") String query) {
        log.info("调用联网搜索工具，关键词: {}", query);
        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.set("query", query);
            requestBody.set("auto_parameters", true);
            requestBody.set("max_results", 5);
            requestBody.set("include_answer", true);
            
            // 使用 searchfree.site API（免费，无需Key，国内可用）
            HttpResponse response = HttpRequest.post("https://searchfree.site/api/search")
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .timeout(30000)
                    .execute();
            
            if (!response.isOk()) {
                return "搜索失败，请稍后重试";
            }
            
            JSONObject json = JSONUtil.parseObj(response.body());
            StringBuilder result = new StringBuilder();
            
            // 获取AI生成的摘要答案
            String answer = json.getStr("answer");
            if (answer != null && !answer.isEmpty()) {
                result.append("【摘要】").append(answer).append("\n\n");
            }
            
            // 获取搜索结果
            JSONArray results = json.getJSONArray("results");
            if (results != null && !results.isEmpty()) {
                result.append("【相关结果】\n");
                for (int i = 0; i < results.size(); i++) {
                    JSONObject item = results.getJSONObject(i);
                    String title = item.getStr("title");
                    String url = item.getStr("url");
                    String content = item.getStr("content");
                    
                    result.append(i + 1).append(". ").append(title).append("\n");
                    if (content != null && !content.isEmpty()) {
                        result.append("   ").append(content.substring(0, Math.min(200, content.length())));
                        if (content.length() > 200) result.append("...");
                        result.append("\n");
                    }
                    result.append("   链接: ").append(url).append("\n\n");
                }
            }
            
            // 如果没有找到结果
            if (result.length() == 0) {
                return "未找到相关信息，建议尝试其他关键词";
            }
            return result.toString();
        } catch (Exception e) {
            log.error("联网搜索失败", e);
            return "搜索失败: " + e.getMessage();
        }
    }
}
