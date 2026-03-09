package com.medlab.controller;

import com.medlab.dto.request.LoginRequest;
import com.medlab.dto.request.RegisterRequest;
import com.medlab.dto.response.ApiResponse;
import com.medlab.dto.response.AuthResponse;
import com.medlab.service.AuthService;
import com.medlab.util.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
// 移除这里的 @CrossOrigin，交给 SecurityConfig 统一管理
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * 验证当前 token 是否有效，返回用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Object>> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(ApiResponse.error("401", "未提供有效的令牌"));
        }
        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).body(ApiResponse.error("401", "令牌无效或已过期"));
        }
        String username = jwtTokenProvider.getUsernameFromToken(token);
        return ResponseEntity.ok(ApiResponse.success(username, "令牌有效"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request, BindingResult bindingResult) {
        
        // 如果参数校验失败，直接打印具体原因并返回 400
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            log.error("注册参数校验失败: {}", errorMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("400", errorMsg));
        }

        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(ApiResponse.success(response, "注册成功"));
        } catch (Exception e) {
            log.error("注册逻辑失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("400", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request, BindingResult bindingResult) {
        
        // 如果参数校验失败，直接打印具体原因并返回 400
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            log.error("参数校验失败: {}", errorMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("400", errorMsg));
        }

        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success(response, "登录成功"));
        } catch (Exception e) {
            log.error("登录逻辑失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("401", e.getMessage()));
        }
    }
}