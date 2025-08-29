package com.smarthome.mqtt.service;

import com.smarthome.mqtt.entity.MqttDataEntity;
import com.smarthome.mqtt.entity.MqttEntity;
import com.smarthome.mqtt.repository.MqttDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MqttDataService {
    @Autowired
    private MqttDataRepository mqttDataRepository;

    @Transactional
    public MqttDataEntity save(MqttEntity mqttEntity) {
        MqttDataEntity dataEntity = new MqttDataEntity(mqttEntity);
        return mqttDataRepository.save(dataEntity);
    }

    @Transactional(readOnly = true)
    public List<MqttDataEntity> findAll() {
        return mqttDataRepository.findAll();
    }
    @Transactional(readOnly = true)
    public List<MqttDataEntity> findByDeviceId(String deviceId) {
        return mqttDataRepository.findMqttDataEntitiesByDeviceId(deviceId);
    }

    @Transactional
    public List<MqttDataEntity> deleteByDeviceIdAndTimestamp(String deviceId,Long timestamp) {
        return mqttDataRepository.findMqttDataEntitiesByDeviceIdAndTimestamp(deviceId,timestamp);
    }

    @Transactional
    public void save(MqttDataEntity mqttDataEntity) {
        mqttDataRepository.save(mqttDataEntity);
    }
}