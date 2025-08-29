package com.smarthome.mqtt.controller;

import com.smarthome.mqtt.service.MqttMessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
public class MqttController {
    private final MqttMessageSender mqttMessageSender;

    @Autowired
    public MqttController(MqttMessageSender mqttMessageSender) {
        this.mqttMessageSender = mqttMessageSender;
    }

    @GetMapping("/send")
    @ResponseBody
    public String send(@RequestParam String topic, @RequestParam String message) {
        mqttMessageSender.sendMessage(topic, message);
        return "Message sent to topic " + topic;
    }

}
