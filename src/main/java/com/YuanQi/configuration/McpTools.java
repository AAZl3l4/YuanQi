package com.YuanQi.configuration;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
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
    @Tool(description = "查询指定城市的天气信息")
    public String getWeather(@ToolParam(description = "城市名称") String city) {
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
    @Tool(description = "获取随机一言或随机诗句")
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
}
