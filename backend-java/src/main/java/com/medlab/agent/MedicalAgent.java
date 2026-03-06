package com.medlab.agent;

/**
 * 医疗AI智能体接口
 * 使用LangChain4j框架定义AI服务
 * 
 * 注意：实际实现需要使用LangChain4j的@AiService注解
 * 由于依赖配置问题，这里提供基本框架
 * 
 * 职责：
 * 1. 定义AI的核心行为和交互方式
 * 2. System Prompt的设定（指导AI的行为准则）
 * 3. 工具调用的管理（调用LabTools中的方法）
 * 4. 多轮对话的维护
 */
public interface MedicalAgent {
    
    /**
     * 分析医疗报告
     * 
     * @param reportContent 医疗报告内容
     * @return AI的分析结果
     */
    String analyzeReport(String reportContent);
    
    /**
     * 基于用户提问进行智能对话
     * 
     * @param userQuery 用户问题
     * @return AI的回答
     */
    String chat(String userQuery);
    
    /**
     * 获取诊断建议
     * 
     * @param symptoms 症状描述
     * @return 诊断建议
     */
    String getDiagnosisSuggestion(String symptoms);
}
