package com.smarthome.user.service;

import com.smarthome.user.entity.UserEntity;
import com.smarthome.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Service
@Slf4j
public class UserService {

    private final UserRepository UserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, HttpServletResponse response, HttpSession session) {
        UserRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public UserEntity register(String username, String password) {
        UserEntity existingUser = UserRepository.findByUsername(username);
        if (existingUser != null) {
            throw new RuntimeException("用户名已被注册");
        }
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));

        return UserRepository.save(user);
    }

    public String login(String username, String password, boolean rememberMe,
                                    HttpServletResponse response, HttpSession session) {
        // 1. 执行身份验证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 2. 根据"记住我"设置有效期（秒）
        int maxAge = rememberMe ? 604800 : 3600; // 7天/1小时

        // 3. 设置Session超时（与Cookie保持一致）
        session.setMaxInactiveInterval(maxAge);

        // 4. 创建并配置Cookie
        Cookie sessionCookie = createSessionCookie(session.getId(), maxAge);

        // 5. 写入Cookie到响应
        response.addCookie(sessionCookie);

        return "登录成功";
    }

    /**
     * 封装Cookie创建逻辑（单一职责，便于维护）
     * @param sessionId 会话ID
     * @param maxAge 有效期（秒）
     * @return 配置好的Cookie对象
     */
    private Cookie createSessionCookie(String sessionId, int maxAge) {
        Cookie cookie = new Cookie("JSESSIONID", sessionId);
        cookie.setPath("/"); // 全站生效
        cookie.setMaxAge(maxAge); // 有效期
        cookie.setSecure(true); // 仅HTTPS传输（与配置文件一致）
        cookie.setHttpOnly(false); // 允许前端访问（按配置文件设置）
        return cookie;
    }
}