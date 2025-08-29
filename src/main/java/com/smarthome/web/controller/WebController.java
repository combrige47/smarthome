package com.smarthome.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import com.smarthome.web.service.WebService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class WebController {
    @Autowired
    private final WebService webService;

    public WebController(WebService webService) {
        this.webService = webService;
    }

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @GetMapping("/getdata")
    @ResponseBody
    public String getAll() {return webService.getAll();}

    @GetMapping("/getdata/{deciveid}")
    @ResponseBody
    public String getById(@PathVariable String deciveid) {return webService.getById(deciveid);}
}
