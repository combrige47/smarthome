package com.smarthome.user.controller;

import com.smarthome.tools.result.Result;
import com.smarthome.user.entity.UserEntity;
import com.smarthome.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@Controller
public class    UserController {

    private final UserService userService;
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<Result<String>> login(
            @RequestParam("username") String username,
            @RequestParam("password") String password) {
            return userService.login(username, password);
    }

    @PostMapping("/register")
    @ResponseBody
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
