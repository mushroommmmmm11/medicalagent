package com.medlab.service;

import com.medlab.entity.User;
import com.medlab.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 用户医疗信息服务
 * 
 * 职责：
 * 1. 追加病历记录（对话确认后）
 * 2. 更新过敏药物（对话确认后）
 * 3. 查询用户病历历史（供模型参考）
 */
@Slf4j
@Service
public class UserMedicalService {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 追加病历记录（用户确认后调用）
     * 格式：日期,疾病,状态
     * 例如：2026-03-09,高血压,未康复
     *
     * @param userId  用户ID
     * @param disease 疾病名称
     * @param status  状态（未康复/已康复/待观察）
     */
    @Transactional
    public void appendMedicalHistory(UUID userId, String disease, String status) {
        String record = LocalDate.now() + "," + disease + "," + status;
        userRepository.appendMedicalHistory(userId, record);
        log.info("追加病历记录: userId={}, record={}", userId, record);
    }
    
    /**
     * 追加病历记录（自定义格式，用于文件上传自动保存）
     *
     * @param userId 用户ID
     * @param record 完整记录字符串
     */
    @Transactional
    public void appendMedicalHistoryRaw(UUID userId, String record) {
        userRepository.appendMedicalHistory(userId, record);
        log.info("追加病历记录(raw): userId={}, record={}", userId, record);
    }
    
    /**
     * 更新过敏药物信息
     *
     * @param userId      用户ID
     * @param drugAllergy 过敏药物（逗号分隔）
     */
    @Transactional
    public void updateDrugAllergy(UUID userId, String drugAllergy) {
        userRepository.updateDrugAllergy(userId, drugAllergy);
        log.info("更新过敏药物: userId={}, drugAllergy={}", userId, drugAllergy);
    }
    
    /**
     * 获取用户的病历历史（供模型参考）
     *
     * @param userId 用户ID
     * @return 病历历史字符串
     */
    public String getMedicalHistory(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return user.getLifetimeMedicalHistory();
    }
    
    /**
     * 获取用户的过敏药物信息
     *
     * @param userId 用户ID
     * @return 过敏药物字符串
     */
    public String getDrugAllergy(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return user.getDrugAllergy();
    }
    
    /**
     * 获取用户的完整医疗摘要（病历+过敏药物，用于给AI模型做上下文）
     *
     * @param userId 用户ID
     * @return 格式化的医疗摘要
     */
    public String getMedicalSummaryForAI(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        StringBuilder sb = new StringBuilder();
        
        if (user.getDrugAllergy() != null && !user.getDrugAllergy().isEmpty()) {
            sb.append("【过敏药物】").append(user.getDrugAllergy()).append("\n");
        }
        
        if (user.getLifetimeMedicalHistory() != null && !user.getLifetimeMedicalHistory().isEmpty()) {
            sb.append("【病历历史】").append(user.getLifetimeMedicalHistory());
        }
        
        return sb.toString();
    }
}
