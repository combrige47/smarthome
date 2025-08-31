package com.smarthome.tools.mqtt.config;

import com.smarthome.tools.mqtt.entity.MqttDataEntity;
import com.smarthome.tools.mqtt.entity.MqttEntity;
import com.smarthome.tools.mqtt.service.MqttDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import com.smarthome.tools.mqtt.service.DataCache;

@Slf4j
@Configuration
public class MqttPubSub {

    private final MqttPahoClientFactory mqttClientFactory;
    @Value("${mqtt.topic}")
    private String topic;
    @Autowired
    public MqttPubSub(MqttPahoClientFactory mqttClientFactory) {
        this.mqttClientFactory = mqttClientFactory;
    }


    //订阅板子数据的
    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter("mqtt-printer-" + System.currentTimeMillis()+"-in", mqttClientFactory, topic.split(","));
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        return adapter;
    }

    //获取topic的数据
    @Bean
    public IntegrationFlow mqttInFlow(MqttDataService mqttDataService) {
        return IntegrationFlows.from(mqttInbound())
                .handle(message -> {
                    String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
                    String payload = (String) message.getPayload();
                    // 打印原始消息
                    log.info("获取主题{}:{}", topic, payload);
                    MqttEntity mqttEntity = new MqttEntity(topic, payload);
                    DataCache.updatedata(mqttEntity);

                    MqttDataEntity mqttDataEntity = new MqttDataEntity(mqttEntity);
                    try{
                        mqttDataService.save(mqttDataEntity);
                        log.info("数据已保存到数据库");
                    }catch (Exception e){
                        log.error(e.getMessage());
                    }
                })
                .get();
    }



    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }


//    给板子发送数据
    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler =
                new MqttPahoMessageHandler("mqtt-printer-" + System.currentTimeMillis() + "-out", mqttClientFactory);
        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic(topic.split(",")[1]);
        return messageHandler;
    }

}