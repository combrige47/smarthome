package com.smarthome.tools.mqtt.controller;

import com.smarthome.tools.mqtt.service.MqttMessageSender;
import com.smarthome.tools.result.Result;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


@Controller
public class MqttController {
    private final MqttMessageSender mqttMessageSender;

    @Autowired
    public MqttController(MqttMessageSender mqttMessageSender) {
        this.mqttMessageSender = mqttMessageSender;
    }

}
