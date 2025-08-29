package com.smarthome.mqtt.service;

import com.smarthome.mqtt.entity.MqttDataEntity;
import com.smarthome.mqtt.entity.MqttEntity;
import com.smarthome.mqtt.repository.MqttDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class MqttDataService {
    @Autowired
    private MqttDataRepository mqttDataRepository;

    private static final long SAVE_INTERVAL = 1 * 1000 * 1000;

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
        String deviceId = mqttDataEntity.getDeviceId();
        Long currentTime = System.currentTimeMillis(); // 当前时间戳
        Long lastSaveTime =mqttDataRepository.findTopByDeviceId(deviceId).getTimestamp();
        System.out.println(lastSaveTime);
        if (currentTime - lastSaveTime >= SAVE_INTERVAL) {
            mqttDataRepository.save(mqttDataEntity);
            log.info("设备[{}]数据存储成功，间隔：{}秒", deviceId, (currentTime - lastSaveTime)/1000);
        } else {
            log.info("设备[{}]数据间隔不足1分钟，跳过存储，当前间隔：{}秒",
                    deviceId, (currentTime - lastSaveTime) / 1000);
        }
    }
}