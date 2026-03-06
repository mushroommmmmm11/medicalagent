package com.medlab.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 
 * 职责：
 * 1. 配置CORS（跨域资源共享）策略
 * 2. 配置拦截器
 * 3. 配置路径映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    /**
     * 配置CORS支持
     * 允许前端从不同域名跨域请求后端API
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
