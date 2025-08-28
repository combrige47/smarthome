package com.smarthome.mqtt.entity;

import com.smarthome.util.TimeUtils;
import lombok.Data;

@Data
public class MqttEntity {
    private String deviceId; //设备id
    private Double value;   //值
    private Long timestamp; // 时间戳
    private String timestampString;

    public MqttEntity(String topic, String payload) {
        this.deviceId = topic;
        this.value = Double.parseDouble(payload);
        this.timestamp = System.currentTimeMillis();
        this.timestampString = TimeUtils.formatTimestamp(this.timestamp);
    }
}