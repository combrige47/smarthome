package com.smarthome.mqttdemo.controller;

import com.smarthome.mqttdemo.service.MqttMessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
public class TestController {

    @Autowired
    private MqttMessageSender mqttMessageSender;

    @GetMapping("/send")
    @ResponseBody
    public String send(@RequestParam String topic, @RequestParam String message) {
        mqttMessageSender.sendMessage(topic, message);
        return "Message sent to topic " + topic;
    }

}
