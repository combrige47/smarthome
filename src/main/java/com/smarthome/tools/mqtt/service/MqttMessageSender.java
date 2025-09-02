package com.smarthome.tools.mqtt.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class MqttMessageSender {

    private final MessageChannel mqttOutboundChannel;

    @Autowired
    public MqttMessageSender(MessageChannel mqttOutboundChannel) {
        this.mqttOutboundChannel = mqttOutboundChannel;
    }
    @Async
    public CompletableFuture<Boolean> sendMessageAsync(String topic, String payload) {
        try {
            boolean result = mqttOutboundChannel.send(MessageBuilder.withPayload(payload)
                    .setHeader("mqtt_topic", topic)
                    .build());
            log.info("已发送主题{}:{}", topic, payload);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("发送主题{}失败", topic, e);
            return CompletableFuture.completedFuture(false);
        }
    }

}
