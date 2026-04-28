package com.YuanQi.configuration;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
        log.debug("调用天气查询工具，城市: {}", city);
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
        log.debug("调用随机一言工具");
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
    @Tool(description = "联网搜索实时信息。以下情况必须调用此工具：1.涉及'最新'、'今天'、'近期'、'最近'、'现在'等时间词 2.新闻、热点、时事 3.实时数据（股价、天气、汇率等）4.任何你不确定或不知道的信息 5.任何需要联网才能回答的问题。不要说'无法提供实时信息'，直接调用工具搜索。")
    public String webSearch(@ToolParam(description = "搜索关键词，提取用户问题中的核心关键词") String query) {
        log.debug("调用联网搜索工具，关键词: {}", query);
        
        // 先尝试主API
        String result = searchWithApi(query);

        // 如果主API失败或无结果，使用必应搜索兜底
        if (result == null || result.isEmpty()) {
            log.info("主API无结果，使用必应搜索兜底");
            result = searchWithBing(query);
        }
        
        return result != null ? result : "搜索失败，请稍后重试";
    }
    
    /**
     * 使用searchfree.site API搜索
     */
    private String searchWithApi(String query) {
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
                log.warn("searchfree.site API返回错误: {}", response.getStatus());
                return null;
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
            
            return result.length() > 0 ? result.toString() : null;
        } catch (Exception e) {
            log.warn("searchfree.site API搜索失败: {}", e.getMessage());
            return null;
        }
    }

    private String searchWithBing(String query) {
        try {
            // 1. 编码关键词
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

            // 2. 构建必应搜索URL（国内版）
            String url = String.format(
                    "https://cn.bing.com/search?q=%s&count=10&setmkt=zh-CN&setlang=zh",
                    encodedQuery
            );

            // 3. 发送请求（模拟真实浏览器）
            HttpResponse response = HttpRequest.get(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Referer", "https://cn.bing.com/")
                    .header("Connection", "keep-alive")
                    .timeout(10000)  // 10秒超时
                    .execute();

            // 4. 检查响应
            if (response.getStatus() != 200) {
                log.info("搜索失败，HTTP状态码: {}", response.getStatus());
                return String.format("搜索失败，HTTP状态码: %d", response.getStatus());
            }

            String html = response.body();

            // 5. 解析HTML（使用Jsoup）
            Document doc = Jsoup.parse(html);

            // 必应搜索结果主要在 .b_algo 中
            Elements results = doc.select("li.b_algo");

            if (results.isEmpty()) {
                // 备选选择器（必应偶尔改版）
                results = doc.select(".b_caption, .b_algo");
                if (results.isEmpty()) {
                    log.info("未找到搜索结果（可能被反爬或页面结构变更）");
                    return "未找到搜索结果";
                }
            }

            // 6. 提取结果
            List<JSONObject> resultList = new ArrayList<>();
            int count = 0;

            for (Element result : results) {
                if (count >= 5) break;  // 只取前5条

                // 标题
                Element titleElem = result.selectFirst("h2 a, .b_algoheader a");
                String title = titleElem != null ? titleElem.text() : "无标题";

                // 链接
                String link = titleElem != null ? titleElem.attr("href") : "";
                // 处理必应跳转链接
                if (link.startsWith("/")) {
                    link = "https://cn.bing.com" + link;
                }

                // 摘要
                Element snippetElem = result.selectFirst(".b_caption p, p, .b_algoSlug");
                String snippet = snippetElem != null ? snippetElem.text() : "";
                // 清理摘要中的广告标记
                snippet = snippet.replaceAll("广告", "").trim();

                // 跳过广告结果（通常有特定标记）
                if (result.select(".b_adSlug, .adSlug").isEmpty() && !title.isEmpty()) {
                    JSONObject item = new JSONObject();
                    item.set("title", title);
                    item.set("url", link);
                    item.set("snippet", snippet);
                    resultList.add(item);
                    count++;
                }
            }

            // 7. 格式化输出（给LLM用的上下文）
            if (resultList.isEmpty()) {
                log.info("未找到有效搜索结果");
                return "未找到有效搜索结果";
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("【必应搜索：%s】\n\n", query));

            for (int i = 0; i < resultList.size(); i++) {
                JSONObject item = resultList.get(i);
                sb.append(String.format("%d. %s\n", i + 1, item.getStr("title")));
                sb.append(String.format("   %s\n", item.getStr("snippet")));
                sb.append(String.format("   来源：%s\n\n", item.getStr("url")));
            }
            log.debug("成功,{}",sb.toString());
            return sb.toString();

        } catch (Exception e) {
            return String.format("搜索异常: %s", e.getMessage());
        }
    }

    /**
     * 点歌/音乐搜索工具
     */
    @Tool(description = "点歌或搜索音乐。当用户要求找歌、点歌、听歌、推荐音乐、指定歌名或歌手时调用此工具。支持通过歌名、歌手名、风格等关键词搜索。")
    public String searchMusic(@ToolParam(description = "搜索关键词，可以是歌名、歌手名或风格描述，如'周杰伦'、'稻香'、'古风歌曲'") String keyword) {
        log.info("调用音乐搜索工具，关键词: {}", keyword);
        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String searchUrl = "https://api.vkeys.cn/v2/music/tencent?word=" + encodedKeyword;

            HttpResponse response = HttpRequest.get(searchUrl)
                    .timeout(15000)
                    .execute();

            if (!response.isOk()) {
                return "音乐搜索失败，请稍后重试";
            }

            JSONObject json = JSONUtil.parseObj(response.body());
            if (json.getInt("code") != 200) {
                return "音乐搜索失败: " + json.getStr("message");
            }

            JSONArray data = json.getJSONArray("data");
            if (data == null || data.isEmpty()) {
                return "未找到相关歌曲，建议尝试其他关键词";
            }

            // 遍历所有结果，找到有播放链接的歌曲
            JSONObject foundSong = null;
            String playUrl = null;
            for (int i = 0; i < data.size(); i++) {
                JSONObject song = data.getJSONObject(i);
                Long songId = song.getLong("id");
                String url = getMusicPlayUrl(songId);
                if (url != null && !url.isEmpty()) {
                    foundSong = song;
                    playUrl = url;
                    break;
                }
            }

            // 如果都没找到播放链接，返回第一首的信息
            if (foundSong == null) {
                foundSong = data.getJSONObject(0);
            }

            String songName = foundSong.getStr("song");
            String singer = foundSong.getStr("singer");
            String album = foundSong.getStr("album");
            String cover = foundSong.getStr("cover");
            String subtitle = foundSong.getStr("subtitle");

            StringBuilder result = new StringBuilder();
            result.append("🎵 ").append(songName);
            if (subtitle != null && !subtitle.isEmpty()) {
                result.append(" (").append(subtitle).append(")");
            }
            result.append("\n");
            result.append("🎤 歌手: ").append(singer).append("\n");
            result.append("💿 专辑: ").append(album).append("\n");
            if (cover != null && !cover.isEmpty()) {
                result.append("🖼️ 封面: ").append(cover).append("\n");
            }
            if (playUrl != null && !playUrl.isEmpty()) {
                result.append("🔗 播放链接: ").append(playUrl).append("\n");
            }

            // 如果有更多结果，列出前3首
            if (data.size() > 1) {
                result.append("\n📋 更多相关歌曲:\n");
                int count = 0;
                for (int i = 0; i < data.size() && count < 3; i++) {
                    JSONObject song = data.getJSONObject(i);
                    // 跳过已找到的歌曲
                    if (song.getLong("id").equals(foundSong.getLong("id"))) {
                        continue;
                    }
                    result.append(count + 1).append(". ").append(song.getStr("song"));
                    result.append(" - ").append(song.getStr("singer"));
                    if (song.getStr("subtitle") != null && !song.getStr("subtitle").isEmpty()) {
                        result.append(" (").append(song.getStr("subtitle")).append(")");
                    }
                    result.append("\n");
                    count++;
                }
            }

            return result.toString();
        } catch (Exception e) {
            log.error("音乐搜索失败", e);
            return "音乐搜索失败: " + e.getMessage();
        }
    }

    /**
     * 获取音乐播放链接
     */
    private String getMusicPlayUrl(Long songId) {
        try {
            String url = "https://api.vkeys.cn/v2/music/tencent/geturl?id=" + songId;
            HttpResponse response = HttpRequest.get(url)
                    .timeout(15000)
                    .execute();

            if (!response.isOk()) {
                return null;
            }

            JSONObject json = JSONUtil.parseObj(response.body());
            if (json.getInt("code") != 200) {
                return null;
            }

            JSONObject data = json.getJSONObject("data");
            if (data != null) {
                return data.getStr("url");
            }
            return null;
        } catch (Exception e) {
            log.warn("获取音乐播放链接失败: {}", e.getMessage());
            return null;
        }
    }
}
