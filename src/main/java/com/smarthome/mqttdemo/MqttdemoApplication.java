package com.smarthome.mqttdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;

@SpringBootApplication
@EnableIntegration
@IntegrationComponentScan
public class MqttdemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MqttdemoApplication.class, args);
        System.out.println("MQTT数据打印服务已启动，等待接收数据...");
    }

}
