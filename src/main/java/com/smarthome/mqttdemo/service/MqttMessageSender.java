package com.smarthome.mqttdemo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

@Service
public class MqttMessageSender {

    @Autowired
    private MessageChannel mqttOutboundChannel;

    //
    public void sendMessage(String topic, String payload) {
        mqttOutboundChannel.send(MessageBuilder.withPayload(payload)
                .setHeader("mqtt_topic", topic)
                .build());
    }

}
