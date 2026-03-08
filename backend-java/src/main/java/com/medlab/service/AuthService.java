package com.medlab.service;

import com.medlab.dto.request.LoginRequest;
import com.medlab.dto.request.RegisterRequest;
import com.medlab.dto.response.AuthResponse;
import com.medlab.dto.response.UserInfoResponse;
import com.medlab.entity.User;
import com.medlab.repository.UserRepository;
import com.medlab.util.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 认证服务
 * 
 * 职责：
 * 1. 处理用户注册（密码加密、数据验证）
 * 2. 处理用户登录（密码验证、Token 生成）
 * 3. 管理用户信息更新
 */
@Slf4j
@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 用户注册 - 简化版本（只需要真实姓名、身份证、密码）
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 验证密码一致性
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }
        
        // 检查身份证号是否已注册
        if (userRepository.existsByIdNumber(request.getIdNumber())) {
            throw new RuntimeException("该身份证号已被注册");
        }
        
        // 用身份证号后8位作为用户名（确保唯一性）
        String idNumberSuffix = request.getIdNumber().substring(request.getIdNumber().length() - 8);
        String username = "user_" + idNumberSuffix;
        
        // 创建新用户
        User user = new User();
        user.setUsername(username);  // 自动生成用户名
        user.setEmail(username + "@medlab.local");  // 自动生成邮箱
        user.setRealName(request.getRealName());
        user.setIdNumber(request.getIdNumber());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus("ACTIVE");
        
        User savedUser = userRepository.save(user);
        
        log.info("用户注册成功: {} ({})", savedUser.getRealName(), savedUser.getIdNumber());
        
        // 生成 Token 并返回
        return buildAuthResponse(savedUser);
    }
    
    /**
     * 用户登录
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByIdNumber(request.getIdNumber())
                .orElseThrow(() -> new RuntimeException("身份证号或密码错误"));
        
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("身份证号或密码错误");
        }
        
        // 检查账户状态
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("账户已被禁用");
        }
        
        // 更新最后登录时间
        user.updateLastLoginTime();
        userRepository.save(user);
        
        log.info("用户登录成功: {}", user.getUsername());
        
        return buildAuthResponse(user);
    }
    
    /**
     * 根据用户名获取用户信息
     */
    public UserInfoResponse getUserInfoByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        return convertToUserInfoResponse(user);
    }
    
    /**
     * 根据用户 ID 获取用户信息
     */
    public UserInfoResponse getUserInfoById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        return convertToUserInfoResponse(user);
    }
    
    /**
     * 修改用户信息
     */
    @Transactional
    public UserInfoResponse updateUserInfo(UUID userId, User updateData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 只允许修改某些字段
        if (updateData.getRealName() != null) {
            user.setRealName(updateData.getRealName());
        }
        if (updateData.getGender() != null) {
            user.setGender(updateData.getGender());
        }
        if (updateData.getAge() != null) {
            user.setAge(updateData.getAge());
        }
        if (updateData.getPhone() != null && !updateData.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(updateData.getPhone())) {
                throw new RuntimeException("手机号已被注册");
            }
            user.setPhone(updateData.getPhone());
        }
        
        User savedUser = userRepository.save(user);
        log.info("用户信息更新成功: {}", savedUser.getUsername());
        
        return convertToUserInfoResponse(savedUser);
    }
    
    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("旧密码错误");
        }
        
        // 设置新密码
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("用户密码修改成功: {}", user.getUsername());
    }
    
    /**
     * 构建认证响应
     */
    private AuthResponse buildAuthResponse(User user) {
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .user(convertToUserInfoResponse(user))
                .build();
    }
    
    /**
     * 转换为用户信息响应
     */
    private UserInfoResponse convertToUserInfoResponse(User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .realName(user.getRealName())
                .status(user.getStatus())
                .build();
    }
}
