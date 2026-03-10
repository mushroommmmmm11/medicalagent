package com.medlab.repository;

import com.medlab.entity.SessionMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 会话临时消息数据访问层
 */
@Repository
public interface SessionMessageRepository extends JpaRepository<SessionMessage, UUID> {

    /**
     * 按创建时间升序获取某用户的全部会话消息
     */
    List<SessionMessage> findByUserIdOrderByCreatedAtAsc(UUID userId);

    /**
     * 清空某用户的全部会话消息（登出时调用）
     */
    void deleteByUserId(UUID userId);
}
