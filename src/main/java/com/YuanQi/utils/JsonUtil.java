package com.YuanQi.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// Json工具类
public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 禁用序列化时丢弃未知字段
    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // 将对象转换为 JSON 字符串
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 将 JSON 字符串转换为对象
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 获取 JSON 字符串中的某个节点
    public static JsonNode getNode(String json, String fieldName) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return node.get(fieldName);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}