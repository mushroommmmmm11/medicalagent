package com.medlab.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 百炼千问 API 测试工具
 * 用于诊断 API 连接问题
 */
public class BailianApiTest {
    
    public static void main(String[] args) throws Exception {
        String apiKey = "sk-7abf8a7f73354b7583e99596c5170d83";
        String model = "qwen3.5-plus-2026-02-15";
        String baseUrl = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
        
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 构建请求体
        String requestBody = "{\"model\":\"" + model + "\"," +
                "\"input\":{\"messages\":[{\"role\":\"user\",\"content\":\"你好\"}]}," +
                "\"parameters\":{\"temperature\":0.7,\"top_p\":0.95,\"max_tokens\":1000}}";
        
        System.out.println("=== 百炼千问 API 测试 ===");
        System.out.println("URL: " + baseUrl);
        System.out.println("Model: " + model);
        System.out.println("API Key: " + apiKey.substring(0, 5) + "***");
        System.out.println("Request Body: " + requestBody);
        System.out.println();
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("User-Agent", "MedLabAgent/1.0");
            headers.set("Accept", "application/json");
            
            System.out.println("Headers:");
            headers.forEach((key, values) -> {
                if (!key.equals("Authorization")) {
                    System.out.println("  " + key + ": " + values);
                } else {
                    System.out.println("  " + key + ": Bearer ***");
                }
            });
            System.out.println();
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            System.out.println("发送请求...");
            long startTime = System.currentTimeMillis();
            String response = restTemplate.postForObject(baseUrl, entity, String.class);
            long endTime = System.currentTimeMillis();
            
            System.out.println("请求成功! 耗时: " + (endTime - startTime) + "ms");
            System.out.println("Response: " + response);
            
            // 解析响应
            Object jsonResponse = objectMapper.readValue(response, Object.class);
            System.out.println("Parsed Response: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResponse));
            
        } catch (Exception e) {
            System.out.println("请求失败!");
            System.out.println("Error Type: " + e.getClass().getName());
            System.out.println("Error Message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
