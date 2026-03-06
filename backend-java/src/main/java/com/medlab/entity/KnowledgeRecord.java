package com.medlab.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 知识库记录实体类
 * 对应数据库的 knowledge_records 表
 * 
 * 字段说明：
 * - id: 记录的唯一标识符
 * - content: 医疗知识内容（如医学文献、诊断指南等）
 * - source: 知识来源（如医学论文、临床指南、报告等）
 * - category: 知识分类（如诊断、治疗、预防等）
 * - embedding: 向量嵌入值（用于向量数据库检索）
 * - createdAt: 记录创建时间
 */
@Entity
@Table(name = "knowledge_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(length = 255)
    private String source;
    
    @Column(length = 100)
    private String category;
    
    @Column(columnDefinition = "TEXT")
    private String embedding;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
