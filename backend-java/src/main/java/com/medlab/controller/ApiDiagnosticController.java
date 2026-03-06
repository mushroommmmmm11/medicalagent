package com.medlab.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * API 诊断控制器
 * 用于测试百炼千问 API 连接
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test")
@CrossOrigin(origins = "*")
public class ApiDiagnosticController {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${ai.bailian.api-key}")
    private String apiKey;
    
    @Value("${ai.bailian.base-url}")
    private String baseUrl;
    
    @Value("${ai.bailian.model}")
    private String model;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 测试百炼千问 API 连接
     */
    @GetMapping("/bailian")
    public ResponseEntity<Map<String, Object>> testBailianConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("开始测试百炼千问 API 连接");
            
            // 构建请求体
            String requestBody = "{\"model\":\"" + model + "\"," +
                    "\"input\":{\"messages\":[{\"role\":\"user\",\"content\":\"测试\"}]}," +
                    "\"parameters\":{\"temperature\":0.7,\"top_p\":0.95,\"max_tokens\":1000}}";
            
            log.info("请求 URL: {}", baseUrl);
            log.info("模型: {}", model);
            log.info("请求体: {}", requestBody);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("User-Agent", "MedLabAgent/1.0");
            headers.set("Accept", "application/json");
            
            log.info("请求头已设置");
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("发送请求到百炼千问 API...");
            long startTime = System.currentTimeMillis();
            
            String response = restTemplate.postForObject(baseUrl, entity, String.class);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("请求成功! 耗时: {}ms", duration);
            
            result.put("success", true);
            result.put("status", "连接成功");
            result.put("duration", duration + "ms");
            result.put("requestUrl", baseUrl);
            result.put("model", model);
            result.put("response", response);
            
            // 尝试解析响应
            try {
                Object jsonResponse = objectMapper.readValue(response, Object.class);
                result.put("parsedResponse", jsonResponse);
            } catch (Exception e) {
                result.put("parsedResponse", "无法解析 JSON: " + e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("测试失败: {}", e.getMessage(), e);
            
            result.put("success", false);
            result.put("status", "连接失败");
            result.put("errorType", e.getClass().getName());
            result.put("errorMessage", e.getMessage());
            result.put("requestUrl", baseUrl);
            result.put("model", model);
            
            // 获取详细错误信息
            if (e.getCause() != null) {
                result.put("causeType", e.getCause().getClass().getName());
                result.put("causeMessage", e.getCause().getMessage());
            }
            
            Throwable rootCause = getRootCause(e);
            if (rootCause != e) {
                result.put("rootCauseType", rootCause.getClass().getName());
                result.put("rootCauseMessage", rootCause.getMessage());
            }
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取根本异常
     */
    private Throwable getRootCause(Throwable t) {
        Throwable cause = t.getCause();
        if (cause == null) {
            return t;
        }
        return getRootCause(cause);
    }
    
    /**
     * 获取配置信息（不含敏感信息）
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("baseUrl", baseUrl);
        config.put("model", model);
        config.put("apiKeyPrefix", apiKey.substring(0, Math.min(10, apiKey.length())) + "***");
        config.put("status", "配置已读取");
        return ResponseEntity.ok(config);
    }
}
