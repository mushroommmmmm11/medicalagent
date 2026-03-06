package com.medlab.service;

import com.medlab.entity.KnowledgeRecord;
import com.medlab.repository.KnowledgeRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 知识库业务服务
 * 
 * 职责：
 * 1. 知识记录的增删改查业务逻辑
 * 2. 文本向量化处理
 * 3. 向量相似度检索
 * 4. 知识库索引维护
 */
@Service
public class KnowledgeService {
    
    @Autowired
    private KnowledgeRecordRepository knowledgeRecordRepository;
    
    /**
     * 保存知识记录
     */
    public KnowledgeRecord saveKnowledge(KnowledgeRecord record) {
        // TODO: 调用向量化模型对content进行向量化
        // record.setEmbedding(vectorizeText(record.getContent()));
        return knowledgeRecordRepository.save(record);
    }
    
    /**
     * 按ID查询知识记录
     */
    public Optional<KnowledgeRecord> findById(Long id) {
        return knowledgeRecordRepository.findById(id);
    }
    
    /**
     * 按分类查询知识
     */
    public List<KnowledgeRecord> findByCategory(String category) {
        return knowledgeRecordRepository.findByCategory(category);
    }
    
    /**
     * 模糊搜索知识
     */
    public List<KnowledgeRecord> searchByKeyword(String keyword) {
        return knowledgeRecordRepository.findByContentContaining(keyword);
    }
    
    /**
     * 删除知识记录
     */
    public void deleteKnowledge(Long id) {
        knowledgeRecordRepository.deleteById(id);
    }
    
    /**
     * 获取所有知识记录
     */
    public List<KnowledgeRecord> getAllKnowledge() {
        return knowledgeRecordRepository.findAll();
    }
}
