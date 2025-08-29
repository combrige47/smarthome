package com.smarthome.user.service;

import com.smarthome.user.entity.UserEntity;
import com.smarthome.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository UserRepository;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    public UserService(com.smarthome.user.repository.UserRepository userRepository, PasswordEncoder passwordEncoder) {
        UserRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}