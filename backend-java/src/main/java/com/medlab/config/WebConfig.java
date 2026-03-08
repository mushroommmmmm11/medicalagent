package com.medlab.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 
 * 职责：
 * 1. 其他 Web 相关配置
 * 2. CORS 配置已移至 SecurityConfig 中统一管理
 * 
 * 说明：
 * SecurityConfig 中的 CorsConfigurationSource 已经全局配置了 CORS，
 * 所以这里不需要再通过 WebMvcConfigurer 重复配置，避免冲突。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // CORS 配置已统一到 SecurityConfig 中
}
