package com.medlab.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 化验单主表实体类
 * 对应数据库 lab_reports 表
 * 
 * 职责：
 * 1. 存储化验单的基本信息（名称、类型、上传路径等）
 * 2. 记录处理状态（PENDING、SUCCESS、FAILED）
 * 3. 存储 OCR 识别出的文本
 */
@Entity
@Table(name = "lab_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;
    
    @Column(nullable = false, columnDefinition = "UUID")
    private UUID userId;
    
    @Column(length = 255)
    private String reportName;
    
    @Column(length = 100)
    private String reportType;
    
    @Column(length = 500)
    private String uploadFilePath;
    
    @Column(length = 500)
    private String minioObjectName;
    
    @Column(length = 50)
    private String status = "PENDING";
    
    @Column(columnDefinition = "TEXT")
    private String ocrText;
    
    @Column
    private LocalDate reportDate;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
