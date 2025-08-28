package com.smarthome.mqtt.entity;

import lombok.Data;

@Data //  lombok 注解，自动生成 getter/setter
public class MqttEntity {
    private String test;    //测试传输
    private String deviceId; // 设备唯一标识
    private String type; // 设备类型（如 temperature、humidity、light）
    private Double value; // 数值（如温度值、湿度值）
    private Long timestamp; // 上报时间戳

    public MqttEntity(String topic, String payload) {
        this.test = topic;
        this.deviceId = topic;
        this.type = topic;
        this.value = Double.parseDouble(payload);
        this.timestamp = System.currentTimeMillis();
    }
}