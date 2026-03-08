package com.medlab.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户实体类
 * 对应数据库 users 表
 * 
 * 职责：
 * 1. 存储用户基本信息（用户名、邮箱、电话等）
 * 2. 存储加密后的密码哈希值
 * 3. 记录用户创建时间和最后登录时间
 * 4. 用户的身体信息（病历史、年龄、性别等）用于个性化的医疗建议
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;
    
    @Column(unique = true, nullable = false, length = 100)
    private String username;
    
    @Column(unique = true, nullable = false, length = 255)
    private String email;
    
    @Column(nullable = false, length = 255)
    private String passwordHash;
    
    @Column(unique = true, length = 20)
    private String phone;
    
    @Column(length = 100)
    private String realName;
    
    @Column(length = 10)
    private String gender;
    
    @Column
    private Integer age;
    
    @Column(unique = true, length = 50)
    private String idNumber;
    
    @Column(length = 50)
    private String createRole = "USER";
    
    @Column(length = 50)
    private String status = "ACTIVE";
    
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(columnDefinition = "TEXT")
    private String medicalHistory;
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 更新最后登录时间
     */
    public void updateLastLoginTime() {
        this.lastLoginTime = LocalDateTime.now();
    }
}
