package com.smarthome.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper(); // 可根据需要配置序列化规则（如日期格式）
    }
}