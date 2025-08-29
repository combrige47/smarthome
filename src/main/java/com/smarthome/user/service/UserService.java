package com.smarthome.user.service;

import com.smarthome.user.entity.UserEntity;
import com.smarthome.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired
    private UserRepository UserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public UserEntity register(String username, String password) {
        // 1. 校验用户名是否已存在（直接用 repository 查询）
        UserEntity existingUser = UserRepository.findByUsername(username);
        if (existingUser != null) {
            throw new RuntimeException("用户名已被注册");
        }

        // 2. 创建用户并加密密码
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // Security 要求密码加密存储

        // 3. 保存到数据库
        return UserRepository.save(user);
    }
}