package com.smarthome.tools.mqtt.service;

import com.smarthome.tools.mqtt.entity.MqttDataEntity;
import com.smarthome.tools.mqtt.entity.MqttEntity;
import com.smarthome.tools.mqtt.repository.MqttDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class MqttDataService {
    private static final long SAVE_INTERVAL = 1000;
    private final MqttDataRepository mqttDataRepository;

    @Autowired
    public MqttDataService(MqttDataRepository mqttDataRepository) {
        this.mqttDataRepository = mqttDataRepository;
    }


    @Transactional
    public void save(MqttEntity mqttEntity) {
        MqttDataEntity dataEntity = new MqttDataEntity(mqttEntity);
        mqttDataRepository.save(dataEntity);
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
        Long lastSaveTime = 0L;
        if(mqttDataRepository.existsByDeviceId(deviceId)){
           MqttDataEntity lastEntity = mqttDataRepository.findTopByDeviceIdOrderByTimestampDesc(deviceId);
           if(lastEntity != null){
               lastSaveTime = lastEntity.getTimestamp();
           }
        }
        System.out.println(lastSaveTime);
        if (currentTime - lastSaveTime >= SAVE_INTERVAL) {
            mqttDataRepository.save(mqttDataEntity);
            log.info("设备[{}]数据存储成功，间隔：{}秒", deviceId, (currentTime - lastSaveTime)/1000);
        } else {
            log.info("设备[{}]数据间隔不足，跳过存储，当前间隔：{}秒",
                    deviceId, (currentTime - lastSaveTime)/1000);
        }
    }
}