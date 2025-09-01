package com.smarthome.web.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthome.config.IatConfig;
import com.smarthome.tools.ai.XModelService;
import com.smarthome.tools.lat.IatClientApp;
import com.smarthome.tools.lat.XflatRecognizer;
import com.smarthome.tools.mqtt.service.DataCache; // 根据实际包路径调整
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.smarthome.tools.mqtt.service.MqttMessageSender;
import org.springframework.web.multipart.MultipartFile;
import com.smarthome.tools.ai.XModelService;


import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.IOException;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;
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

    public ResponseEntity<String> IatRecognizer(MultipartFile audioFile) {
        if (audioFile.isEmpty()) {
            return ResponseEntity.badRequest().body("请上传一个音频文件。");
        }
        String projectRoot = System.getProperty("user.dir");
        String targetDirPath = projectRoot + File.separator + "upload" + File.separator + "audio";
        File targetDir = new File(targetDirPath);
        if (!targetDir.exists()) {
            boolean isCreated = targetDir.mkdirs();
            if (!isCreated) {
                return ResponseEntity.status(500).body("无法创建音频存储文件夹：" + targetDirPath);
            }
            System.out.println("音频存储文件夹已创建：" + targetDirPath);
        }

        File tempFile = null;
        try {
            String fileName = "upload-" + UUID.randomUUID().toString() + ".pcm";
            tempFile = new File(targetDir, fileName);
            String tempFilePath = tempFile.getAbsolutePath();
            System.out.println("音频文件将保存到：" + tempFilePath);
            audioFile.transferTo(tempFile);
            System.out.println("音频文件保存成功：" + tempFilePath);

            XflatRecognizer recognizer = new XflatRecognizer();
            XflatRecognizer.file = tempFile.getAbsolutePath();
            String recognizeResult = recognizer.startRecognize();

            if (recognizeResult == null || recognizeResult.trim().isEmpty()) {
                return ResponseEntity.status(500).body("语音识别失败，未获取到有效结果。");
            }
            return ResponseEntity.ok("识别结果：" + recognizeResult);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("语音识别时发生错误: " + e.getMessage());
        } finally {
            if (tempFile != null) {
                try {
                    tempFile.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public String sendBedroomData(String topic, int value) {
        Map<String, Integer> payloadMap = new HashMap<>();
        payloadMap.put(topic, value);
        try {
            // 使用 ObjectMapper 将 Map 转换为 JSON 字符串
            String payloadJson = objectMapper.writeValueAsString(payloadMap);

            // 调用 sendMessage 方法发送 JSON 字符串
            mqttOutboundChannel.sendMessage("bedroom_cmd", payloadJson);

            return "数据发送成功：" + payloadJson;

        } catch (JsonProcessingException e) {
            // 处理 JSON 转换异常
            log.error("将卧室数据转换为 JSON 时出错", e);
            return "数据转换失败，请检查参数。";
        }
    }

    public String testAi(String voiceText) {
        String jsonData = xModelService.getMqttCommand(voiceText);
        JSONArray jsonArray = new JSONArray(jsonData);
        for (Object obj : jsonArray) {
            JSONObject jsonObject = (JSONObject) obj;
            String topic = jsonObject.getStr("topic");
            String payload = jsonObject.getStr("payload");
            mqttOutboundChannel.sendMessage(topic, payload);
        }
        return null;
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