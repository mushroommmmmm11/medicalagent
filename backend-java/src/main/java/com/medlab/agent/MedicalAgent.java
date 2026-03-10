package com.medlab.agent;

import com.medlab.entity.SessionMessage;
import reactor.core.publisher.Flux;

import java.util.List;

public interface MedicalAgent {
    // 基础对话
    String chat(String userQuery);
    
    // 流式对话（带用户健康档案上下文和多轮历史）
    Flux<String> chatStream(String userQuery, String userContext, List<SessionMessage> history);
    
    // 报告分析
    String analyzeReport(String reportContent);
    
    // 诊断建议
    String getDiagnosisSuggestion(String symptoms);
    
    // --- 之前报错就是因为少了下面这两个定义 ---
    String getMedicalKnowledge(String topic);
    
    String explainMedicalTerm(String term);
}