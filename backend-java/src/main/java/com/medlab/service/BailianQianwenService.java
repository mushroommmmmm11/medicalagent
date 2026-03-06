package com.medlab.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 百炼千问 API 服务
 * 
 * 职责：
 * 1. 调用阿里云百炼千问 API
 * 2. 处理请求和响应的封装
 * 3. 错误处理和日志记录
 */
@Slf4j
@Service
public class BailianQianwenService {
    
    @Autowired//@Autowired：Spring 的依赖注入注解，表示由 Spring 容器自动注入 RestTemplate 实例
    private RestTemplate restTemplate;
    
    @Value("${ai.bailian.api-key}")//@Value("")：Spring 提供的注解，用于将外部配置的值注入到类的成员变量中
    private String apiKey;
    
    @Value("${ai.bailian.base-url}")
    private String baseUrl;
    
    @Value("${ai.bailian.model}")
    private String model;
    
    @Value("${ai.bailian.temperature:0.7}")//回答的创造性，范围0-1，默认0.7
    private float temperature;
    
    @Value("${ai.bailian.top-p:0.95}")//下一个词的采样范围，范围0-1，默认0.95
    private float topP;
    
    @Value("${ai.bailian.max-tokens:2000}")//生成文本的最大长度，默认2000
    private int maxTokens;
    
    private final ObjectMapper objectMapper = new ObjectMapper();//用于将Java对象转换为JSON字符串，或将JSON字符串解析为Java对象
    
    /**
     * 调用百炼千问 API 生成文本
     * 
     * @param prompt 用户提示词
     * @return AI 生成的文本
     * @throws Exception 调用失败时抛出异常
     */
    public String generateText(String prompt) throws Exception {
      try {
    // 1. 构建请求体（需自行实现buildRequestBody方法，拼接prompt参数为API要求的JSON格式）
    String requestBody = buildRequestBody(prompt);
    
    // 2. 构建请求头，设置API调用必要的HTTP头信息
    HttpHeaders headers = new HttpHeaders();
    // 设置请求体格式为JSON
    headers.setContentType(MediaType.APPLICATION_JSON);
    // 设置认证令牌（Bearer + API密钥）
    headers.set("Authorization", "Bearer " + apiKey);
    // 设置自定义用户代理，标识调用方
    headers.set("User-Agent", "MedLabAgent/1.0");
    // 声明接受JSON格式的响应
    headers.set("Accept", "application/json");
    
    // 3. 构造HTTP请求实体（包含请求体和请求头）
    HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
    
    // 4. 日志记录（INFO级别记录调用动作，DEBUG级别记录详细信息）
    log.info("Calling Bailian Qianwen API with model: {}", model);
    log.debug("Request URL: {}", baseUrl);
    log.debug("Request body: {}", requestBody);
    // 脱敏打印API密钥（只显示前5位）
    log.debug("API Key prefix: {}***", apiKey.substring(0, Math.min(5, apiKey.length())));
    
    // 5. 发送POST请求，接收字符串格式的响应
    String response = restTemplate.postForObject(baseUrl, entity, String.class);
    
    // 6. 记录响应日志，解析响应结果（需自行实现parseResponse方法）
    log.info("Successfully received response from Bailian API");
    log.debug("Bailian Qianwen API response: {}", response);
    return parseResponse(response);
} catch (Exception e) {
    // 7. 异常处理：记录错误日志，封装为运行时异常抛出（保留原始异常栈）
    log.error("Error calling Bailian Qianwen API: {}", e.getMessage(), e);
    throw new RuntimeException("调用百炼千问 API 失败: " + e.getMessage(), e);
}
    }
    
    /**
     * 构建请求体 JSON
     * 
     * @param prompt 用户提示词
     * @return JSON 格式的请求体
     */
    private String buildRequestBody(String prompt) {
        // 使用官方推荐的 JSON 格式，不包含 max_tokens（可能导致url error）
        StringBuilder json = new StringBuilder();
        json.append("{\"model\":\"").append(model).append("\",");
        json.append("\"input\":{\"messages\":[{\"role\":\"user\",\"content\":");
        json.append(escapeJson(prompt));
        json.append("}]},");
        json.append("\"parameters\":{");
        json.append("\"temperature\":").append(temperature).append(",");
        json.append("\"top_p\":").append(topP);
        json.append("}}");
        
        log.debug("构建的请求体: {}", json.toString());
        return json.toString();
    }
    
    /**
     * 解析 API 响应
     * 
     * @param response API 响应的 JSON 字符串
     * @return 提取的文本内容
     * @throws Exception 解析失败时抛出异常
     */
    private String parseResponse(String response) throws Exception {
        log.debug("Parsing response: {}", response);
        
        JsonNode root = objectMapper.readTree(response);
        
        // 检查是否有错误
        if (root.has("code") && !root.get("code").asText().equals("200")) {
            String errorMsg = root.has("message") ? root.get("message").asText() : "Unknown error";
            throw new RuntimeException("Bailian API error: " + errorMsg);
        }
        
        if (root.has("error")) {
            String errorMsg = root.get("error").asText();
            throw new RuntimeException("Bailian API error: " + errorMsg);
        }
        
        // 提取生成的文本 - 百炼的标准响应格式
        if (root.has("output")) {
            JsonNode output = root.get("output");
            if (output.has("text")) {
                String text = output.get("text").asText();
                log.info("Successfully got response from Bailian API");
                return text;
            } else if (output.has("choices")) {
                JsonNode choices = output.get("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode firstChoice = choices.get(0);
                    if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                        String content = firstChoice.get("message").get("content").asText();
                        log.info("Successfully got response from Bailian API");
                        return content;
                    }
                }
            }
        }
        
        log.warn("Could not parse expected response format from API");
        return "";
    }
    
    /**
     * 转义 JSON 字符串
     * 
     * @param text 原始文本
     * @return 转义后的 JSON 字符串
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
