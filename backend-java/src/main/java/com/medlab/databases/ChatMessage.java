package com.medlab.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户对话记录实体类
 * 对应数据库 chat_messages 表
 * 
 * 职责：
 * 1. 存储用户与 AI Agent 的对话历史
 * 2. 记录每次对话消耗的 tokens 数量（便于成本统计）
 * 3. 关联化验单和用户，用于上下文感知的回复
 */
@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;
    
    @Column(nullable = false, columnDefinition = "UUID")
    private UUID userId;
    
    @Column(columnDefinition = "UUID")
    private UUID reportId;
    
    @Column(length = 50)
    private String messageType;
    
    @Column(columnDefinition = "TEXT")
    private String userMessage;
    
    @Column(columnDefinition = "TEXT")
    private String agentResponse;
    
    @Column
    private Integer tokensUsed;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
