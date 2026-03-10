package com.medlab.service;

import com.medlab.entity.SessionMessage;
import com.medlab.repository.SessionMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 会话对话历史服务
 * 
 * 职责：
 * 1. 保存每轮用户提问和AI回复
 * 2. 查询某用户的全部会话历史（按时间升序）
 * 3. 用户登出时清空该用户的全部会话记录
 */
@Slf4j
@Service
public class SessionChatService {

    @Autowired
    private SessionMessageRepository sessionMessageRepository;

    /**
     * 保存一条会话消息
     */
    public void saveMessage(UUID userId, String role, String content) {
        SessionMessage msg = new SessionMessage();
        msg.setUserId(userId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        sessionMessageRepository.save(msg);
    }

    /**
     * 获取某用户的全部会话历史（按时间升序）
     */
    public List<SessionMessage> getHistory(UUID userId) {
        return sessionMessageRepository.findByUserIdOrderByCreatedAtAsc(userId);
    }

    /**
     * 清空某用户的全部会话记录（登出时调用）
     */
    @Transactional
    public void clearHistory(UUID userId) {
        sessionMessageRepository.deleteByUserId(userId);
        log.info("已清空用户 {} 的会话历史", userId);
    }
}
