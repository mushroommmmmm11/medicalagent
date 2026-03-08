package com.medlab.repository;

import com.medlab.entity.KnowledgeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识库记录的数据访问对象（DAO）
 * 继承JpaRepository，自动获得CRUD和分页排序等功能
 * 
 * 职责：
 * 1. 数据库的增删改查操作
 * 2. 自定义查询方法，如按分类查询、按来源查询等
 * 3. 向量相似度搜索（向量数据库集成）
 */
@Repository
public interface KnowledgeRecordRepository extends JpaRepository<KnowledgeRecord, Long> {
    
    /**
     * 按分类查询知识记录
     */
    List<KnowledgeRecord> findByCategory(String category);
    
    /**
     * 按来源查询知识记录
     */
    List<KnowledgeRecord> findBySource(String source);
    
    /**
     * 模糊搜索知识内容
     */
    List<KnowledgeRecord> findByContentContaining(String keyword);
}
