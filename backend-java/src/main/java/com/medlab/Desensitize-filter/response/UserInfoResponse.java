package com.medlab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 用户信息响应 DTO
 * 不包含敏感信息（如密码、身份证号）
 * 包含：
 * 1. 用户 ID（id） - 唯一标识用户的 UUID
 *  2. 真实姓名（realName） - 用户的真实姓名
 * 3. 账户状态（status） - 如 "ACTIVE", "INACTIVE", 
 * "LOCKED" 等，指示用户账户的当前状态
 * 设计原则：
 * 1. 提供必要的用户信息，支持前端显示用户相关数据
 *  2. 设计简洁，避免暴露敏感信息
 * 3. 包含账户状态，便于前端根据状态进行相应的处理（如显示不同的 UI 元素）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponse {

    private UUID id;

    private String realName;

    private String status;
}
