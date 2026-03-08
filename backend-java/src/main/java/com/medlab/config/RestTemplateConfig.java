package com.medlab.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

/**
 * RestTemplate配置
 * 
 * 职责：
 * 1. 创建RestTemplate Bean用于HTTP调用
 * 2. 配置连接池、超时等参数
 * 3. 便于在服务中调用外部API（如Python OCR服务）
 */
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(90))
                .build();
    }
}
