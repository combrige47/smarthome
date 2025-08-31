package com.smarthome.tools.token;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 Token（登录成功后调用）
     * @param username 用户名（可替换为用户 ID 等唯一标识）
     * @return JWT 字符串
     */
    public String generateToken(String username) {
        // 1. 设置 Token 载荷（自定义信息）
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username); // 存储用户名
        // claims.put("role", "USER"); // 可添加角色信息（用于权限控制）

        // 2. 生成 Token
        return Jwts.builder()
                .setClaims(claims) // 注入载荷
                .setIssuedAt(new Date()) // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // 过期时间
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // 签名算法+密钥
                .compact(); // 压缩为字符串
    }

    /**
     * 从 Token 中解析用户名（验证 Token 时调用）
     * @param token JWT 字符串
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // 验证密钥
                .build()
                .parseClaimsJws(token) // 解析 Token
                .getBody(); // 获取载荷
        return claims.get("username", String.class); // 从载荷中获取用户名
    }

    /**
     * 验证 Token 合法性（核心：是否篡改、是否过期）
     * @param token JWT 字符串
     * @return true=合法，false=非法
     */
    public boolean validateToken(String token) {
        try {
            // 解析 Token（若解析失败，会抛出对应异常）
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            // Token 格式错误
            System.out.println("无效的 Token 格式");
        } catch (ExpiredJwtException e) {
            // Token 已过期
            System.out.println("Token 已过期");
        } catch (UnsupportedJwtException e) {
            // 不支持的 Token 类型
            System.out.println("不支持的 Token 类型");
        } catch (IllegalArgumentException e) {
            // Token 载荷为空
            System.out.println("Token 载荷为空");
        }
        return false;
    }
}