package com.smarthome.web.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthome.mqtt.service.DataCache; // 根据实际包路径调整
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service // 标记为 Spring 管理的 Bean
public class WebService {

    @Autowired // 自动注入 DataCache（由 Spring 管理）
    private DataCache dataCache;

    @Autowired // 自动注入 ObjectMapper（来自 JsonConfig 的 Bean）
    private ObjectMapper objectMapper;

    public String getAll() {
        try {
            return objectMapper.writeValueAsString(dataCache.getAlldata());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}"; // 异常时返回空 JSON
        }
    }
    public String getById(String device) {
        try {
            return objectMapper.writeValueAsString(dataCache.getdata(device));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}"; // 异常时返回空 JSON
        }
    }
}