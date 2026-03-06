package com.medlab.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.medlab.service.BailianQianwenService;
import com.medlab.tools.LabTools;

/**
 * 百炼千问医疗AI智能体实现
 * 
 * 职责：
 * 1. 使用百炼千问 API 进行 AI 对话
 * 2. 整合医疗实验室工具
 * 3. 提供医疗分析和诊断建议
 */
@Slf4j
@Component
public class BailianMedicalAgent implements MedicalAgent {
    
    @Autowired
    private BailianQianwenService bailianService;
    
    @Autowired
    private LabTools labTools;
    
    private static final String SYSTEM_PROMPT = "你是一个专业的医学实验室AI智能体，具备以下特点：\n" +
            "1. 深厚的医学检验知识\n" +
            "2. 能够分析实验报告和诊断数据\n" +
            "3. 提供基于证据的医学建议\n" +
            "4. 注重患者安全和诊疗规范\n" +
            "\n" +
            "在提供任何医学建议时，请：\n" +
            "- 基于科学证据进行分析\n" +
            "- 建议患者咨询专业医生进行确诊\n" +
            "- 提供清晰的解释和可能的影响\n" +
            "- 避免给出确定性的医疗诊断";
    
    @Override
    public String analyzeReport(String reportContent) {
        try {
            String prompt = "请分析以下医疗报告，并提供专业的分析意见：\n\n" +
                    "报告内容：\n" + reportContent + "\n\n" +
                    "请从以下方面分析：\n" +
                    "1. 主要检查指标及其含义\n" +
                    "2. 异常结果及可能的原因\n" +
                    "3. 与该患者的医学意义\n" +
                    "4. 建议的后续检查或诊疗方向\n";
            
            String response = bailianService.generateText(prompt);
            log.info("Report analysis completed successfully");
            return response;
        } catch (Exception e) {
            log.error("Error analyzing report", e);
            return "分析报告时出错：" + e.getMessage();
        }
    }
    
    @Override
    public String chat(String userQuery) {
        try {
            String prompt = SYSTEM_PROMPT + "\n\n用户问题：" + userQuery + "\n\n请提供详细且专业的回答。\n";
            
            String response = bailianService.generateText(prompt);
            log.info("Chat response generated successfully");
            return response;
        } catch (Exception e) {
            log.error("Error in chat", e);
            return "处理问题时出错：" + e.getMessage();
        }
    }
    
    @Override
    public String getDiagnosisSuggestion(String symptoms) {
        try {
            String prompt = "根据以下患者症状，提供初步的诊断建议：\n\n" +
                    "症状描述：" + symptoms + "\n\n" +
                    "请提供：\n" +
                    "1. 可能相关的疾病或病症\n" +
                    "2. 建议的检查项目\n" +
                    "3. 可能的治疗方向（需强调咨询医生）\n" +
                    "4. 相关的医学知识补充\n\n" +
                    "重要提示：这只是初步建议，不能作为医疗诊断，患者必须咨询专业医生。\n";
            
            String response = bailianService.generateText(prompt);
            log.info("Diagnosis suggestion generated successfully");
            return response;
        } catch (Exception e) {
            log.error("Error generating diagnosis suggestion", e);
            return "生成诊断建议时出错：" + e.getMessage();
        }
    }
    
    /**
     * 获取实时医学知识
     * 
     * @param topic 医学主题
     * @return 相关的医学知识
     */
    public String getMedicalKnowledge(String topic) {
        try {
            String prompt = "请提供关于\"" + topic + "\"的详细医学知识，包括：\n" +
                    "1. 基本定义和分类\n" +
                    "2. 病因和发病机制\n" +
                    "3. 临床表现和诊断\n" +
                    "4. 治疗方法和预防\n" +
                    "5. 相关的检查指标\n";
            
            return bailianService.generateText(prompt);
        } catch (Exception e) {
            log.error("Error getting medical knowledge", e);
            return "获取医学知识时出错：" + e.getMessage();
        }
    }
    
    /**
     * 解释医学术语
     * 
     * @param term 医学术语
     * @return 术语解释
     */
    public String explainMedicalTerm(String term) {
        try {
            String prompt = "请用简单易懂的语言解释医学术语\"" + term + "\"，包括：\n" +
                    "1. 术语的含义\n" +
                    "2. 常见的使用场景\n" +
                    "3. 相关的检查或诊断\n" +
                    "4. 如果是异常值，其可能含义\n";
            
            return bailianService.generateText(prompt);
        } catch (Exception e) {
            log.error("Error explaining medical term", e);
            return "解释术语时出错：" + e.getMessage();
        }
    }
}
