package com.smarthome.user.controller;

import com.beust.jcommander.Parameter;
import com.smarthome.user.entity.UserEntity;
import com.smarthome.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Controller
public class    UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @RequestParam String username,
            @RequestParam String password
    ) {
        try {
            UserEntity user = userService.register(username, password);
            return ResponseEntity.ok("注册成功，用户名：" + user.getUsername());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
