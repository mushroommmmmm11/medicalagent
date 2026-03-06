package com.medlab.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AgentController 单元测试
 * 
 * 职责：
 * 1. 测试API端点是否正常工作
 * 2. 验证请求/响应格式
 * 3. 检查错误处理
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AgentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    /**
     * 测试健康检查端点
     */
    @Test
    public void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").exists());
    }
}
