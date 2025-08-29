package com.smarthome.web.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthome.mqtt.service.DataCache; // 根据实际包路径调整
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class WebService {
    private static final Logger logger = LoggerFactory.getLogger(WebService.class);


    private final DataCache dataCache;
    private final ObjectMapper objectMapper;
    @Autowired
    public WebService(DataCache dataCache, ObjectMapper objectMapper) {
        this.dataCache = dataCache;
        this.objectMapper = objectMapper;
    }

    public String getAll() {
        try {
            return objectMapper.writeValueAsString(dataCache.getAlldata());
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            return "{}"; // 异常时返回空 JSON
        }
    }
    public String getById(String device) {
        try {
            return objectMapper.writeValueAsString(dataCache.getdata(device));
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            return "{}"; // 异常时返回空 JSON
        }
    }
}