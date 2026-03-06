package com.medlab.controller;

import com.medlab.agent.BailianMedicalAgent;
import com.medlab.service.KnowledgeService;
import com.medlab.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * AI智能体API控制器
 * 处理所有HTTP请求，是前端和后端的通信桥梁
 * 
 * 职责：
 * 1. 接收客户端HTTP请求
 * 2. 调用相应的服务进行业务处理
 * 3. 返回结果给客户端
 * 4. 错误处理和异常返回
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")  // 允许跨域请求
public class AgentController {
    
    @Autowired
    private BailianMedicalAgent bailianMedicalAgent;
    
    @Autowired
    private KnowledgeService knowledgeService;
    
    @Autowired
    private StorageService storageService;
    
    /**
     * 健康检查端点
     * 用于验证服务是否正常运行
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "MedLabAgent System is running");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 分析医疗报告
     * 使用百炼千问AI进行分析
     * 
     * @param reportContent 医疗报告内容
     * @return 分析结果
     */
    @PostMapping("/agent/analyze-report")
    public ResponseEntity<Map<String, String>> analyzeReport(
            @RequestParam String reportContent) {
        try {
            String analysis = bailianMedicalAgent.analyzeReport(reportContent);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("analysis", analysis);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * AI对话端点
     * 使用百炼千问AI进行医学咨询
     * 
     * @param userQuery 用户问题
     * @return 智能回答
     */
    @PostMapping("/agent/chat")
    public ResponseEntity<Map<String, String>> chat(
            @RequestParam String userQuery) {
        try {
            String aiResponse = bailianMedicalAgent.chat(userQuery);
            
            Map<String, String> result = new HashMap<>();
            result.put("status", "success");
            result.put("response", aiResponse);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 获取诊断建议
     * 
     * @param symptoms 症状描述
     * @return 诊断建议
     */
    @PostMapping("/agent/diagnosis")
    public ResponseEntity<Map<String, String>> getDiagnosisSuggestion(
            @RequestParam String symptoms) {
        try {
            String suggestion = bailianMedicalAgent.getDiagnosisSuggestion(symptoms);
            
            Map<String, String> result = new HashMap<>();
            result.put("status", "success");
            result.put("suggestion", suggestion);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 获取医学知识
     * 
     * @param topic 医学主题
     * @return 医学知识
     */
    @PostMapping("/agent/knowledge")
    public ResponseEntity<Map<String, String>> getMedicalKnowledge(
            @RequestParam String topic) {
        try {
            String knowledge = bailianMedicalAgent.getMedicalKnowledge(topic);
            
            Map<String, String> result = new HashMap<>();
            result.put("status", "success");
            result.put("knowledge", knowledge);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 解释医学术语
     * 
     * @param term 医学术语
     * @return 术语解释
     */
    @PostMapping("/agent/explain-term")
    public ResponseEntity<Map<String, String>> explainMedicalTerm(
            @RequestParam String term) {
        try {
            String explanation = bailianMedicalAgent.explainMedicalTerm(term);
            
            Map<String, String> result = new HashMap<>();
            result.put("status", "success");
            result.put("explanation", explanation);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 上传医疗报告文件
     * 
     * @param file 上传的文件
     * @return 文件保存路径
     */
    @PostMapping("/agent/upload-report")
    public ResponseEntity<Map<String, String>> uploadReport(
            @RequestParam("file") MultipartFile file) {
        try {
            String filePath = storageService.saveFile(file);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("filePath", filePath);
            response.put("fileName", file.getOriginalFilename());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 获取知识库中的信息
     */
    @GetMapping("/knowledge/search")
    public ResponseEntity<Map<String, Object>> searchKnowledge(
            @RequestParam String keyword) {
        try {
            var records = knowledgeService.searchByKeyword(keyword);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("results", records);
            response.put("count", records.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
