package com.smarthome.tools.mqtt.entity;

import cn.hutool.json.JSONObject;
import cn.hutool.json.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.smarthome.tools.time.TimeUtils;
import lombok.Data;

@Data
public class MqttEntity {
    private String deviceId; //设备id
    private JSONObject payload;
    private Long timestamp; // 时间戳
    private String timestampString;

    public MqttEntity(String topic, String payload) {
        this.deviceId = topic;
        this.timestamp = System.currentTimeMillis();
        this.timestampString = TimeUtils.formatTimestamp(this.timestamp);
        this.payload = new JSONObject(payload);
    }
}