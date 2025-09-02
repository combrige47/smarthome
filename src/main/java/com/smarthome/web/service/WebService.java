package com.smarthome.web.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthome.tools.ai.XModelService;
import com.smarthome.tools.lat.IatClientApp;
import com.smarthome.tools.mqtt.service.DataCache; // 根据实际包路径调整
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.smarthome.tools.mqtt.service.MqttMessageSender;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.IOException;
import java.util.UUID;


@Slf4j
@Service
public class WebService {
    private static final Logger logger = LoggerFactory.getLogger(WebService.class);
    private final MqttMessageSender mqttOutboundChannel;
    private final XModelService xModelService;
    private final DataCache dataCache;
    private final IatClientApp iatClientApp;
    private final ObjectMapper objectMapper;

    @Autowired
    public WebService(MqttMessageSender mqttOutboundChannel, XModelService xModelService, DataCache dataCache, IatClientApp iatClientApp, ObjectMapper objectMapper) {
        this.mqttOutboundChannel = mqttOutboundChannel;
        this.xModelService = xModelService;
        this.dataCache = dataCache;
        this.iatClientApp = iatClientApp;
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


    public String sendData(String topic, String value) {
        try {
            // 异步发送，不阻塞当前请求
            mqttOutboundChannel.sendMessageAsync(topic, value)
                    .whenComplete((success, ex) -> {
                        if (ex != null) {
                            log.error("发送失败", ex);
                        } else if (!success) {
                            log.warn("消息未被MQTT通道接收");
                        }
                    });
            return "消息已提交发送";
        } catch (Exception e) {
            throw new RuntimeException("提交发送任务失败", e);
        }
    }

    public String testAi(String voiceText) {
        String jsonData = xModelService.getMqttCommand(voiceText);
        System.out.println(jsonData);
        JSONArray jsonArray = new JSONArray(jsonData);
        for (Object obj : jsonArray) {
            JSONObject jsonObject = (JSONObject) obj;
            String topic = jsonObject.getStr("topic");
            String payload = jsonObject.getStr("payload");
            // 异步发送，不阻塞循环
            mqttOutboundChannel.sendMessageAsync(topic, payload);
        }
        return "指令已提交处理";
    }

    public String Iat(MultipartFile audioFile) throws IOException, InterruptedException {
        if (audioFile.isEmpty()) {
            return ("请上传一个音频文件。");
        }
        String projectRoot = System.getProperty("user.dir");
        String targetDirPath = projectRoot + File.separator + "upload" + File.separator + "audio";
        File targetDir = new File(targetDirPath);
        if (!targetDir.exists()) {
            boolean isCreated = targetDir.mkdirs();
            if (!isCreated) {
                return ("无法创建音频存储文件夹：" + targetDirPath);
            }
            System.out.println("音频存储文件夹已创建：" + targetDirPath);
        }

        File tempFile = null;
        String fileName = "upload-" + UUID.randomUUID().toString() + ".pcm";
        tempFile = new File(targetDir, fileName);
        String tempFilePath = tempFile.getAbsolutePath();
        System.out.println("音频文件将保存到：" + tempFilePath);
        audioFile.transferTo(tempFile);
        System.out.println("音频文件保存成功：" + tempFilePath);
        return iatClientApp.processAudioSync(tempFilePath);
    }

    public void IatToLLM(MultipartFile audioFile) throws IOException, InterruptedException {
        String voiceText = Iat(audioFile);
        testAi(voiceText);
    }
}