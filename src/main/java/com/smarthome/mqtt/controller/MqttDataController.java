package com.smarthome.mqtt.controller;

import com.smarthome.mqtt.entity.MqttDataEntity;
import com.smarthome.mqtt.service.MqttDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/history")
public class MqttDataController {
    private final MqttDataService mqttDataService;

    @Autowired
    public MqttDataController(MqttDataService mqttDataService) {
        this.mqttDataService = mqttDataService;
    }

    @GetMapping("/")
    @ResponseBody
    public List<MqttDataEntity> findAll() {
        return mqttDataService.findAll();
    }

    @GetMapping("/{deciveId}")
    @ResponseBody
    public List<MqttDataEntity> FindHistoryById(
            @PathVariable String deciveId) {
        return mqttDataService.findByDeviceId(deciveId);
    }

    @GetMapping("/{deviceId}/{time}")
    @ResponseBody
    public List<MqttDataEntity> FindHistoryByDeviceIdAndTimestamp(
            @PathVariable String deviceId,
            @PathVariable("time") Long timestamp) {
        return mqttDataService.deleteByDeviceIdAndTimestamp(deviceId,timestamp);
    }



}
