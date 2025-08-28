package com.smarthome.mqtt.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MqttMessageSender {

    @Autowired
    private MessageChannel mqttOutboundChannel;

    //
    public void sendMessage(String topic, String payload) {
        mqttOutboundChannel.send(MessageBuilder.withPayload(payload)
                .setHeader("mqtt_topic", topic)
                .build());
        log.info("以发送主题"+topic+":"+payload);
    }

}
