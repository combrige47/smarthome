package com.smarthome.user.service;

import com.smarthome.tools.token.JwtTokenUtil;
import com.smarthome.tools.result.Result;
import com.smarthome.user.entity.UserEntity;
import com.smarthome.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class UserService {

    private final UserRepository UserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtTokenUtil jwtTokenUtil) {
        UserRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
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

    public ResponseEntity<Result<String>> login(String username, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            //String token = jwtTokenUtil.generateToken(username);
            return ResponseEntity.ok(Result.success("登录成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.fail("用户名或密码错误"));
        }
    }
}