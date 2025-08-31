package com.smarthome.tools.mqtt.entity;

import lombok.Data;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@Entity
public class MqttDataEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 主键，自增
    private String deviceId; //设备id
    private String value;   //值
    private Long timestamp;
    private String timestampStr;

    public MqttDataEntity() {
    }

    public MqttDataEntity(MqttEntity mqttEntity) {
        this.deviceId = mqttEntity.getDeviceId();
        this.value = mqttEntity.getValue();
        this.timestamp = mqttEntity.getTimestamp();
        this.timestampStr = mqttEntity.getTimestampString();
    }

}