package com.medlab.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 化验单详细指标实体类
 * 对应数据库 report_items 表
 * 
 * 职责：
 * 1. 存储化验单中的每一个检查指标（如：血红蛋白、白细胞等）
 * 2. 记录指标值、单位、参考范围
 * 3. 标记是否异常
 */
@Entity
@Table(name = "report_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;
    
    @Column(nullable = false, columnDefinition = "UUID")
    private UUID reportId;
    
    @Column(length = 255)
    private String itemName;
    
    @Column(length = 255)
    private String itemValue;
    
    @Column(length = 50)
    private String unit;
    
    @Column(length = 255)
    private String referenceRange;
    
    @Column
    private Boolean isAbnormal = false;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
