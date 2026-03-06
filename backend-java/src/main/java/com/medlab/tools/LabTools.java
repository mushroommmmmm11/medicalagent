package com.medlab.tools;

import com.medlab.service.KnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AI智能体的工具箱
 * 包含被@Tool注解标记的Java方法
 * 在AI推理时，这些方法会被Agent自动调用以完成任务
 * 
 * 工具职责：
 * 1. 数据库查询 - 检索知识库中的医学信息
 * 2. 外部API调用 - 调用Python OCR服务进行文字识别
 * 3. 计算和处理 - 数据转换、统计等
 * 4. 验证和校验 - 输入数据的有效性检查
 */
@Component
public class LabTools {
    
    @Autowired
    private KnowledgeService knowledgeService;
    
    /**
     * 查询医学知识
     * 当AI需要获取医学背景信息时调用此方法
     * 
     * @param keyword 查询关键词
     * @return 匹配的知识内容
     */
    public String queryMedicalKnowledge(String keyword) {
        var records = knowledgeService.searchByKeyword(keyword);
        if (records.isEmpty()) {
            return "No knowledge found for keyword: " + keyword;
        }
        
        StringBuilder result = new StringBuilder();
        for (var record : records) {
            result.append("Source: ").append(record.getSource()).append("\n");
            result.append("Content: ").append(record.getContent()).append("\n\n");
        }
        return result.toString();
    }
    
    /**
     * 调用OCR服务进行文字识别
     * 将医疗报告图片转换为可处理的文本
     * 
     * @param imagePath 图片路径
     * @return 识别出的文本
     */
    public String performOCR(String imagePath) {
        // TODO: 通过RestTemplate调用Python OCR服务
        // POST请求到: http://ocr-service:8000/api/v1/ocr
        return "OCR result for: " + imagePath;
    }
    
    /**
     * 获取报告分类
     * 根据报告内容推断报告类型
     * 
     * @param reportContent 报告内容
     * @return 报告分类
     */
    public String classifyReport(String reportContent) {
        if (reportContent.contains("血液") || reportContent.contains("CBC")) {
            return "Blood Test Report";
        } else if (reportContent.contains("肝功") || reportContent.contains("Liver")) {
            return "Liver Function Test";
        } else if (reportContent.contains("肾功") || reportContent.contains("Kidney")) {
            return "Kidney Function Test";
        }
        return "General Medical Report";
    }
    
    /**
     * 验证数据完整性
     * 
     * @param data 待验证的数据
     * @return 验证结果
     */
    public boolean validateData(String data) {
        return data != null && !data.trim().isEmpty();
    }
}
