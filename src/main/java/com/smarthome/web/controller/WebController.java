package com.smarthome.web.controller;

import com.smarthome.mqtt.service.DataCache;
import com.smarthome.mqtt.service.MqttMessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import com.smarthome.web.service.WebService;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class WebController {
    @Autowired
    private final WebService webService;

    public WebController(WebService webService) {
        this.webService = webService;
    }

    @GetMapping("/getdata")
    @ResponseBody
    public String get() {
        return webService.get();
    }
}
