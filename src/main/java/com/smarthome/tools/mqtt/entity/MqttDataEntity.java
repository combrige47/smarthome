package com.smarthome.tools.mqtt.entity;

import cn.hutool.json.JSONObject;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;

@Data
@Entity
@TypeDef(name = "json", typeClass = JsonType.class)
public class MqttDataEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 主键，自增
    private String topic;
    @Type(type = "json")
    @Column(columnDefinition = "json")
    private JSONObject payload;   //值
    private Long timestamp;
    private String timestampStr;

    public MqttDataEntity() {
    }

    public MqttDataEntity(MqttEntity mqttEntity) {
        this.topic = mqttEntity.getTopic();
        this.payload = mqttEntity.getPayload();
        this.timestamp = mqttEntity.getTimestamp();
        this.timestampStr = mqttEntity.getTimestampString();
    }

}