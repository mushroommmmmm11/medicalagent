package com.medlab.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 工具类
 * 负责 Token 的生成和验证
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    
    @Value("${jwt.secret:your-secret-key-change-this-in-production}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;
    
    /**
     * 生成 JWT Token
     */
    public String generateToken(UUID userId, String idNumber) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("idNumber", idNumber);
        
        return createToken(claims, userId.toString());
    }
    
    /**
     * 创建 Token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        SecretKey key = getSigningKey();
        
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }
    
    /**
     * 从 Token 中获取用户 ID
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        String userId = (String) claims.get("userId");
        return UUID.fromString(userId);
    }
    
    /**
     * 从 Token 中获取身份证号
     */
    public String getIdNumberFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get("idNumber");
    }
    
    /**
     * 验证 Token 是否有效
     */
    public Boolean validateToken(String token) {
        try {
            SecretKey key = getSigningKey();
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取 Token 的所有声明
     */
    private Claims getAllClaimsFromToken(String token) {
        SecretKey key = getSigningKey();
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * 获取 Token 过期时间
     */
    public long getExpirationTime() {
        return jwtExpiration;
    }

    private SecretKey getSigningKey() {
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length >= 64) {
            return Keys.hmacShaKeyFor(secretBytes);
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] derivedKey = digest.digest(secretBytes);
            log.warn("JWT secret is shorter than 64 bytes; deriving a 512-bit key for HS512 compatibility.");
            return Keys.hmacShaKeyFor(derivedKey);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 algorithm is not available", e);
        }
    }
}
