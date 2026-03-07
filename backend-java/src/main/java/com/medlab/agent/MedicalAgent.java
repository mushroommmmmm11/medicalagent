package com.medlab.agent;

/**
 * 医疗AI智能体接口
 * 使用LangChain4j框架定义AI服务
 */
public interface MedicalAgent {
    String analyzeReport(String reportContent);
    String chat(String userQuery);
    String getDiagnosisSuggestion(String symptoms);
}
