package com.smarthome.tools.mqtt.service;

import com.smarthome.tools.mqtt.entity.MqttEntity;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DataCache {
    // 存储设备最新数据：key=设备ID，payload=设备数据
    private static final Map<String, MqttEntity> MqttEntityMap = new ConcurrentHashMap<>();

    // 保存或更新设备数据
    public static void updatedata(MqttEntity data) {
        MqttEntityMap.put(data.getTopic(), data);
    }

    // 获取单个设备最新数据
    public MqttEntity getdata(String deviceId) {
        return MqttEntityMap.get(deviceId);
    }

    // 获取所有设备最新数据
    public Map<String, MqttEntity> getAlldata() {
        return MqttEntityMap;
    }
}