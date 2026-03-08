package com.medlab.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.medlab.service.BailianQianwenService;
import com.medlab.tools.LabTools;
import reactor.core.publisher.Flux;

/**
 * 百炼千问医疗AI智能体实现
 */
@Slf4j
@Component
public class AgentFuncRealize implements MedicalAgent {
    
    @Autowired
    private BailianQianwenService bailianService;
    
    @Autowired
    private LabTools labTools;
    
    // 提示词优化：减少冗余字符，为 AI 回复留出更多 Token 空间
    private static final String SYSTEM_PROMPT = "你是一个专业的医学实验室AI智能体，名为 MedLabAgent。 " +
            "你是由医学实验室系统驱动的专业助手。你具备深厚的医学检验知识，能分析报告指标并提供专业、严谨、亲切的建议。 " +
            "【注意】：任何建议后必须提示：'以上建议仅供参考，请以临床医生诊断为准'。";

    /**
     * 流式对话实现
     */
    public Flux<String> chatStream(String userQuery) {
        try {
            // 采用更简洁的拼接方式
            String prompt = String.format("%s\n\n用户问题：%s\n请以 MedLabAgent 身份详细回复：", SYSTEM_PROMPT, userQuery);
            log.info("Processing streaming chat...");
            return bailianService.generateTextStream(prompt);
        } catch (Exception e) {
            log.error("Error in streaming chat", e);
            return Flux.just("MedLabAgent 连接异常：" + e.getMessage());
        }
    }

    @Override
    public String chat(String userQuery) {
        try {
            String prompt = String.format("%s\n\n用户问题：%s\n请详细回答：", SYSTEM_PROMPT, userQuery);
            return bailianService.generateText(prompt);
        } catch (Exception e) {
            log.error("Error in chat", e);
            return "处理问题时出错：" + e.getMessage();
        }
    }

    @Override
    public String analyzeReport(String reportContent) {
        try {
            String prompt = String.format("%s\n\n分析报告内容：\n%s\n\n请从指标、原因、医学意义及建议四个维度详细分析。", SYSTEM_PROMPT, reportContent);
            return bailianService.generateText(prompt);
        } catch (Exception e) {
            log.error("Error analyzing report", e);
            return "分析报告时出错：" + e.getMessage();
        }
    }

    @Override
    public String getDiagnosisSuggestion(String symptoms) {
        try {
            String prompt = String.format("%s\n\n患者症状：%s\n\n请详细提供可能病症、建议检查及治疗方向。", SYSTEM_PROMPT, symptoms);
            return bailianService.generateText(prompt);
        } catch (Exception e) {
            log.error("Error generating diagnosis", e);
            return "生成诊断建议时出错：" + e.getMessage();
        }
    }

    @Override
    public String getMedicalKnowledge(String topic) {
        String prompt = String.format("%s\n\n请科普关于“%s”的医学知识，要求通俗易懂且科学严谨。", SYSTEM_PROMPT, topic);
        return bailianService.generateText(prompt);
    }

    @Override
    public String explainMedicalTerm(String term) {
        String prompt = String.format("%s\n\n请详细解释医学名词“%s”的含义及其在检验中的意义。", SYSTEM_PROMPT, term);
        return bailianService.generateText(prompt);
    }
}