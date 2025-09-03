package com.smarthome.user.controller;

import com.smarthome.tools.result.Result;
import com.smarthome.user.entity.UserEntity;
import com.smarthome.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


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
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false, defaultValue = "false") boolean rememberMe,
            HttpServletResponse response,
            HttpSession session) {

        try {
            // 调用服务层方法，传入必要参数，隐藏实现细节
            String message = userService.login(username, password, rememberMe, response, session);
            return ResponseEntity.ok(Result.success(String.valueOf(message)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.fail("用户名或密码错误"));
        }
    }

    @PostMapping("/register")
    @ResponseBody
    public ResponseEntity<String> register(
            @RequestParam String username,
            @RequestParam String password
    ) {
        try {
            userService.register(username, password);
            return ResponseEntity.ok("注册成功");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
