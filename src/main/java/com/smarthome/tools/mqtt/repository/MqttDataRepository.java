package com.smarthome.tools.mqtt.repository;

import com.smarthome.tools.mqtt.entity.MqttDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MqttDataRepository extends JpaRepository<MqttDataEntity, Long> {
    List<MqttDataEntity> findMqttDataEntitiesByTopic(String deviceId);

    List<MqttDataEntity> findMqttDataEntitiesByTopicAndTimestamp(String deviceId, Long timestamp);

    boolean existsByTopic(String deviceId);

    MqttDataEntity findTopByTopicOrderByTimestampDesc(String deviceId);
}
