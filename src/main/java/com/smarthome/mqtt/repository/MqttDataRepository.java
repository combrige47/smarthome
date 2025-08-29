package com.smarthome.mqtt.repository;

import com.smarthome.mqtt.entity.MqttDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MqttDataRepository extends JpaRepository<MqttDataEntity, Long> {
    List<MqttDataEntity> findMqttDataEntitiesByDeviceId(String deviceId);

    List<MqttDataEntity> findMqttDataEntitiesByDeviceIdAndTimestamp(String deviceId, Long timestamp);
}
